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
    toolbar?.let(activity::setupDrawerToggle)
    setFragmentContainerBottomMarginToSizeOfNavBar()
    openPageInBookFromNavigationArguments()
  }

  private fun openPageInBookFromNavigationArguments() {
    val args = KiwixReaderFragmentArgs.fromBundle(requireArguments())

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
      loadUrlWithCurrentWebview(args.pageUrl)
    } else {
      if (args.zimFileUri.isNotEmpty()) {
        tryOpeningZimFile(args.zimFileUri)
      } else {
        manageExternalLaunchAndRestoringViewState()
      }
    }
    requireArguments().clear()
  }

  private fun tryOpeningZimFile(zimFileUri: String) {
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
    openZimFile(ZimReaderSource(File(filePath)))
  }

  override fun loadDrawerViews() {
    drawerLayout = requireActivity().findViewById(R.id.navigation_container)
    tableDrawerRightContainer = requireActivity().findViewById(R.id.reader_drawer_nav_view)
  }

  override fun openHomeScreen() {
    Handler(Looper.getMainLooper()).postDelayed({
      if (webViewList.size == 0) {
        hideTabSwitcher()
      }
    }, HIDE_TAB_SWITCHER_DELAY)
  }

  override fun hideTabSwitcher() {
    actionBar?.let { actionBar ->
      actionBar.setDisplayShowTitleEnabled(true)
      toolbar?.let { activity?.setupDrawerToggle(it) }

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
        exitBook()
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
    if (zimReaderContainer?.zimReaderSource == null) {
      exitBook()
    }
    if (isFullScreenVideo || isInFullScreenMode()) {
      hideNavBar()
    }
  }

  override fun restoreViewStateOnInvalidJSON() {
    Log.d(TAG_KIWIX, "Kiwix normal start, no zimFile loaded last time  -> display home page")
    exitBook()
  }

  override fun restoreViewStateOnValidJSON(
    zimArticles: String?,
    zimPositions: String?,
    currentTab: Int
  ) {
    val settings = requireActivity().getSharedPreferences(SharedPreferenceUtil.PREF_KIWIX_MOBILE, 0)
    val zimReaderSource = fromDatabaseValue(settings.getString(TAG_CURRENT_FILE, null))
    if (zimReaderSource != null) {
      if (zimReaderContainer?.zimReaderSource == null) {
        openZimFile(zimReaderSource)
        Log.d(
          TAG_KIWIX,
          "Kiwix normal start, Opened last used zimFile: -> ${zimReaderSource.toDatabase()}"
        )
      } else {
        zimReaderContainer?.zimFileReader?.let(::setUpBookmarks)
      }
    } else {
      getCurrentWebView()?.snack(string.zim_not_opened)
      exitBook() // hide the options for zim file to avoid unexpected UI behavior
      return // book not found so don't need to restore the tabs for this file
    }
    restoreTabs(zimArticles, zimPositions, currentTab)
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
