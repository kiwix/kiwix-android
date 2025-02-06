/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.nav.destination.reader

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.R.anim
import org.kiwix.kiwixmobile.core.R.drawable
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions.Super
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions.Super.ShouldCall
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.setupDrawerToggle
import org.kiwix.kiwixmobile.core.extensions.coreMainActivity
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.extensions.setBottomMarginToFragmentContainerView
import org.kiwix.kiwixmobile.core.extensions.setImageDrawableCompat
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.CoreReaderFragment
import org.kiwix.kiwixmobile.core.main.CoreWebViewClient
import org.kiwix.kiwixmobile.core.main.RestoreOrigin
import org.kiwix.kiwixmobile.core.main.RestoreOrigin.FromExternalLaunch
import org.kiwix.kiwixmobile.core.main.RestoreOrigin.FromSearchScreen
import org.kiwix.kiwixmobile.core.main.ToolbarScrollingKiwixWebView
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource.Companion.fromDatabaseValue
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TAG_CURRENT_FILE
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.io.File

private const val HIDE_TAB_SWITCHER_DELAY: Long = 300

class KiwixReaderFragment : CoreReaderFragment() {
  private var isFullScreenVideo: Boolean = false

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val activity = activity as CoreMainActivity
    noOpenBookButton?.setOnClickListener {
      activity.navigate(
        KiwixReaderFragmentDirections.actionNavigationReaderToNavigationLibrary()
      )
    }
    activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
    toolbar?.let { activity.setupDrawerToggle(it, true) }
    openPageInBookFromNavigationArguments()
  }

  @Suppress("MagicNumber")
  private fun openPageInBookFromNavigationArguments() {
    showProgressBarWithProgress(30)
    val args = KiwixReaderFragmentArgs.fromBundle(requireArguments())
    coreReaderLifeCycleScope?.launch {
      if (args.pageUrl.isNotEmpty()) {
        if (args.zimFileUri.isNotEmpty()) {
          tryOpeningZimFile(args.zimFileUri)
        } else {
          // Set up bookmarks for the current book when opening bookmarks from the Bookmark screen.
          // This is necessary because we are not opening the ZIM file again; the bookmark is
          // inside the currently opened book. Bookmarks are set up when opening the ZIM file.
          // See https://github.com/kiwix/kiwix-android/issues/3541
          zimReaderContainer?.zimFileReader?.let(::setUpBookmarks)
        }
        hideProgressBar()
        loadUrlWithCurrentWebview(args.pageUrl)
      } else {
        if (args.zimFileUri.isNotEmpty()) {
          tryOpeningZimFile(args.zimFileUri)
        } else {
          val restoreOrigin =
            if (args.searchItemTitle.isNotEmpty()) FromSearchScreen else FromExternalLaunch
          manageExternalLaunchAndRestoringViewState(restoreOrigin)
        }
      }
      requireArguments().clear()
    }
  }

  private suspend fun tryOpeningZimFile(zimFileUri: String) {
    // Stop any ongoing WebView loading and clear the WebView list
    // before setting a new ZIM file to the reader. This helps prevent native crashes.
    // The WebView's `shouldInterceptRequest` method continues to be invoked until the WebView is
    // fully destroyed, which can cause a native crash. This happens because a new ZIM file is set
    // in the reader while the WebView is still trying to access content from the old archive.
    stopOngoingLoadingAndClearWebViewList()
    // Close the previously opened book in the reader before opening a new ZIM file
    // to avoid native crashes due to "null pointer dereference." These crashes can occur
    // when setting a new ZIM file in the archive while the previous one is being disposed of.
    // Since the WebView may still asynchronously request data from the disposed archive,
    // we close the previous book before opening a new ZIM file in the archive.
    closeZimBook()
    // Update the reader screen title to prevent showing the previously set title
    // when creating the new archive object.
    updateTitle()
    val filePath = FileUtils.getLocalFilePathByUri(
      requireActivity().applicationContext, Uri.parse(zimFileUri)
    )
    if (filePath == null || !File(filePath).isFileExist()) {
      // Close the previously opened book in the reader. Since this file is not found,
      // it will not be set in the zimFileReader. The previously opened ZIM file
      // will be saved when we move between fragments. If we return to the reader again,
      // it will attempt to open the last opened ZIM file with the last loaded URL,
      // which is inside the non-existing ZIM file. This leads to unexpected behavior.
      exitBook()
      activity.toast(string.error_file_not_found)
      return
    }
    val zimReaderSource = ZimReaderSource(File(filePath))
    openZimFile(zimReaderSource)
  }

  override fun loadDrawerViews() {
    drawerLayout = requireActivity().findViewById(R.id.navigation_container)
    tableDrawerRightContainer = requireActivity().findViewById(R.id.reader_drawer_nav_view)
  }

  override fun openHomeScreen() {
    Handler(Looper.getMainLooper()).postDelayed({
      if (webViewList.size == 0) {
        hideTabSwitcher(false)
      }
    }, HIDE_TAB_SWITCHER_DELAY)
  }

  /**
   * Hides the tab switcher and optionally closes the ZIM book based on the `shouldCloseZimBook` parameter.
   *
   * @param shouldCloseZimBook If `true`, the ZIM book will be closed, and the `ZimFileReader` will be set to `null`.
   * If `false`, it skips setting the `ZimFileReader` to `null`. This is particularly useful when restoring tabs,
   * as setting the `ZimFileReader` to `null` would require re-creating it, which is a resource-intensive operation,
   * especially for large ZIM files.
   *
   * Refer to the following methods for more details:
   * @See exitBook
   * @see closeTab
   * @see closeAllTabs
   */
  override fun hideTabSwitcher(shouldCloseZimBook: Boolean) {
    actionBar?.let { actionBar ->
      actionBar.setDisplayShowTitleEnabled(true)
      toolbar?.let { activity?.setupDrawerToggle(it, true) }

      setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

      closeAllTabsButton?.setImageDrawableCompat(drawable.ic_close_black_24dp)
      if (tabSwitcherRoot?.visibility == View.VISIBLE) {
        tabSwitcherRoot?.visibility = GONE
        startAnimation(tabSwitcherRoot, anim.slide_up)
        progressBar?.visibility = View.GONE
        progressBar?.progress = 0
        contentFrame?.visibility = View.VISIBLE
      }
      mainMenu?.showWebViewOptions(true)
      if (webViewList.isEmpty()) {
        exitBook(shouldCloseZimBook)
      } else {
        // Reset the top margin of web views to 0 to remove any previously set margin
        // This ensures that the web views are displayed without any additional
        // top margin for kiwix main app.
        setTopMarginToWebViews(0)
        selectTab(currentWebViewIndex)
      }
    }
  }

  private fun setFragmentContainerBottomMarginToSizeOfNavBar() {
    val bottomNavigationView =
      requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_view)
    bottomNavigationView?.let {
      setBottomMarginToNavHostContainer(
        bottomNavigationView.measuredHeight
      )
    }
  }

  override fun onPause() {
    super.onPause()
    // ScrollingViewWithBottomNavigationBehavior changes the margin to the size of the nav bar,
    // this resets the margin to zero, before fragment navigation.
    setBottomMarginToNavHostContainer(0)
  }

  @Suppress("DEPRECATION")
  override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, menuInflater)
    if (zimReaderContainer?.zimFileReader == null) {
      mainMenu?.hideBookSpecificMenuItems()
    }
  }

  override fun onCreateOptionsMenu(
    menu: Menu,
    activity: AppCompatActivity
  ): Super = ShouldCall

  override fun onResume() {
    super.onResume()
    setFragmentContainerBottomMarginToSizeOfNavBar()
    if (isFullScreenVideo || isInFullScreenMode()) {
      hideNavBar()
    }
  }

  override fun restoreViewStateOnInvalidJSON() {
    Log.d(TAG_KIWIX, "Kiwix normal start, no zimFile loaded last time  -> display home page")
    exitBook()
  }

  /**
   * Restores the view state based on the provided JSON data and restore origin.
   *
   * Depending on the `restoreOrigin`, this method either restores the last opened ZIM file
   * (if the launch is external) or skips re-opening the ZIM file when coming from the search screen,
   * as the ZIM file is already set in the reader. The method handles setting up the ZIM file and bookmarks,
   * and restores the tabs and positions from the provided data.
   *
   * @param zimArticles   JSON string representing the list of articles to be restored.
   * @param zimPositions  JSON string representing the positions of the restored articles.
   * @param currentTab    Index of the tab to be restored as the currently active one.
   * @param restoreOrigin Indicates whether the restoration is triggered from an external launch or the search screen.
   */

  override fun restoreViewStateOnValidJSON(
    zimArticles: String?,
    zimPositions: String?,
    currentTab: Int,
    restoreOrigin: RestoreOrigin
  ) {
    when (restoreOrigin) {
      FromExternalLaunch -> {
        coreReaderLifeCycleScope?.launch {
          val settings =
            requireActivity().getSharedPreferences(SharedPreferenceUtil.PREF_KIWIX_MOBILE, 0)
          val zimReaderSource = fromDatabaseValue(settings.getString(TAG_CURRENT_FILE, null))
          if (zimReaderSource?.canOpenInLibkiwix() == true) {
            if (zimReaderContainer?.zimReaderSource == null) {
              openZimFile(zimReaderSource)
              Log.d(
                TAG_KIWIX,
                "Kiwix normal start, Opened last used zimFile: -> ${zimReaderSource.toDatabase()}"
              )
            } else {
              zimReaderContainer?.zimFileReader?.let(::setUpBookmarks)
            }
            restoreTabs(zimArticles, zimPositions, currentTab)
          } else {
            getCurrentWebView()?.snack(string.zim_not_opened)
            exitBook() // hide the options for zim file to avoid unexpected UI behavior
          }
        }
      }

      FromSearchScreen -> {
        restoreTabs(zimArticles, zimPositions, currentTab)
      }
    }
  }

  @Throws(IllegalArgumentException::class)
  override fun createWebView(attrs: AttributeSet?): ToolbarScrollingKiwixWebView? {
    requireNotNull(activityMainRoot)
    return ToolbarScrollingKiwixWebView(
      requireContext(),
      this,
      attrs ?: throw IllegalArgumentException("AttributeSet must not be null"),
      activityMainRoot as ViewGroup,
      requireNotNull(videoView),
      CoreWebViewClient(this, requireNotNull(zimReaderContainer)),
      requireNotNull(toolbarContainer),
      requireNotNull(bottomToolbar),
      sharedPreferenceUtil = requireNotNull(sharedPreferenceUtil),
      parentNavigationBar = requireActivity().findViewById(R.id.bottom_nav_view)
    )
  }

  override fun onFullscreenVideoToggled(isFullScreen: Boolean) {
    isFullScreenVideo = isFullScreen
    if (isFullScreenVideo) {
      hideNavBar()
    } else {
      showNavBar()
    }
    super.onFullscreenVideoToggled(isFullScreen)
  }

  override fun openFullScreen() {
    super.openFullScreen()
    hideNavBar()
  }

  override fun closeFullScreen() {
    super.closeFullScreen()
    showNavBar()
    setFragmentContainerBottomMarginToSizeOfNavBar()
  }

  private fun hideNavBar() {
    requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_view).visibility = GONE
    setBottomMarginToNavHostContainer(0)
  }

  private fun showNavBar() {
    // show the navBar if fullScreenMode is not active.
    if (!isInFullScreenMode()) {
      requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_view).visibility =
        VISIBLE
    }
  }

  override fun createNewTab() {
    newMainPageTab()
  }

  private fun setBottomMarginToNavHostContainer(margin: Int) {
    coreMainActivity.navHostContainer
      .setBottomMarginToFragmentContainerView(margin)
  }
}
