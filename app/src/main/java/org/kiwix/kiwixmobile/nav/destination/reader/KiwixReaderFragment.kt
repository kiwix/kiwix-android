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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.R.anim
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions.Super
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions.Super.ShouldCall
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.consumeObservable
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.observeNavigationResult
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
import org.kiwix.kiwixmobile.core.main.FIND_IN_PAGE_SEARCH_STRING
import org.kiwix.kiwixmobile.core.main.ToolbarScrollingKiwixWebView
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SearchItemToOpen
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TAG_CURRENT_FILE
import org.kiwix.kiwixmobile.core.utils.TAG_FILE_SEARCHED
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.utils.titleToUrl
import org.kiwix.kiwixmobile.core.utils.urlSuffixToParsableUrl
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

    requireActivity().observeNavigationResult<String>(
      FIND_IN_PAGE_SEARCH_STRING,
      viewLifecycleOwner,
      Observer(::findInPage)
    )
    requireActivity().observeNavigationResult<SearchItemToOpen>(
      TAG_FILE_SEARCHED,
      viewLifecycleOwner,
      Observer(::openSearchItem)
    )
  }

  private fun openSearchItem(item: SearchItemToOpen) {
    zimReaderContainer?.titleToUrl(item.pageTitle)?.let {
      if (item.shouldOpenInNewTab) {
        createNewTab()
      }
      loadUrlWithCurrentWebview(zimReaderContainer?.urlSuffixToParsableUrl(it))
    }
    requireActivity().consumeObservable<SearchItemToOpen>(TAG_FILE_SEARCHED)
  }

  private fun openPageInBookFromNavigationArguments() {
    val args = KiwixReaderFragmentArgs.fromBundle(requireArguments())

    if (args.pageUrl.isNotEmpty()) {
      if (args.zimFileUri.isNotEmpty()) {
        tryOpeningZimFile(args.zimFileUri)
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

    if (filePath == null || !File(filePath).exists()) {
      activity.toast(R.string.error_file_not_found)
      return
    }
    openZimFile(File(filePath))
  }

  override fun loadDrawerViews() {
    drawerLayout = requireActivity().findViewById(R.id.navigation_container)
    tableDrawerRightContainer = requireActivity().findViewById(R.id.reader_drawer_nav_view)
  }

  private fun exitBook() {
    showNoBookOpenViews()
    bottomToolbar?.visibility = GONE
    actionBar?.title = getString(R.string.reader)
    contentFrame?.visibility = GONE
    mainMenu?.hideBookSpecificMenuItems()
    closeZimBook()
  }

  private fun closeZimBook() {
    zimReaderContainer?.setZimFile(null)
  }

  override fun openHomeScreen() {
    Handler().postDelayed({
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

      closeAllTabsButton?.setImageDrawableCompat(R.drawable.ic_close_black_24dp)
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
    if (zimReaderContainer?.zimFile == null) {
      exitBook()
    }
    if (isFullScreenVideo) {
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
    val zimFile = settings.getString(TAG_CURRENT_FILE, null)

    if (zimFile != null && File(zimFile).isFileExist()) {
      if (zimReaderContainer?.zimFile == null) {
        openZimFile(File(zimFile))
        Log.d(
          TAG_KIWIX,
          "Kiwix normal start, Opened last used zimFile: -> $zimFile"
        )
      }
    } else {
      getCurrentWebView()?.snack(R.string.zim_not_opened)
    }
    restoreTabs(zimArticles, zimPositions, currentTab)
  }

  override fun createWebView(attrs: AttributeSet?): ToolbarScrollingKiwixWebView {
    return ToolbarScrollingKiwixWebView(
      requireContext(), this, attrs!!, activityMainRoot as ViewGroup, videoView!!,
      CoreWebViewClient(this, zimReaderContainer!!, sharedPreferenceUtil!!),
      toolbarContainer!!, bottomToolbar!!, sharedPreferenceUtil = sharedPreferenceUtil!!,
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
    getCurrentWebView()?.translationY = 0f
  }

  private fun showNavBar() {
    requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_view).visibility = VISIBLE
  }

  override fun createNewTab() {
    newMainPageTab()
  }

  override fun onNewIntent(
    intent: Intent,
    activity: AppCompatActivity
  ): Super {
    super.onNewIntent(activity.intent, activity)
    intent.data?.let {
      if ("file" == it.scheme) openZimFile(it.toFile())
      else activity.toast(R.string.cannot_open_file)
    }
    return ShouldCall
  }

  private fun setBottomMarginToNavHostContainer(margin: Int) {
    coreMainActivity.navHostContainer
      .setBottomMarginToFragmentContainerView(margin)
  }
}
