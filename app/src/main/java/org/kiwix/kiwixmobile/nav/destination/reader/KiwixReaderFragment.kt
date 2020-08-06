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
import android.os.Bundle
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue.complexToDimensionPixelSize
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.drawerlayout.widget.DrawerLayout
import kotlinx.android.synthetic.main.activity_kiwix_main.bottom_nav_view
import org.json.JSONArray
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.anim
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions.Super
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions.Super.ShouldCall
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions.Super.ShouldNotCall
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.setupDrawerToggle
import org.kiwix.kiwixmobile.core.extensions.getAttribute
import org.kiwix.kiwixmobile.core.extensions.setImageDrawableCompat
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.CoreReaderFragment
import org.kiwix.kiwixmobile.core.main.CoreWebViewClient
import org.kiwix.kiwixmobile.core.main.ToolbarScrollingKiwixWebView
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TAG_CURRENT_ARTICLES
import org.kiwix.kiwixmobile.core.utils.TAG_CURRENT_FILE
import org.kiwix.kiwixmobile.core.utils.TAG_CURRENT_POSITIONS
import org.kiwix.kiwixmobile.core.utils.TAG_CURRENT_TAB
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.UpdateUtils
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import java.io.File

private const val HIDE_TAB_SWITCHER_DELAY: Long = 300

class KiwixReaderFragment : CoreReaderFragment() {

  override fun inject(baseActivity: BaseActivity) {
    (baseActivity as KiwixMainActivity).cachedComponent.inject(this)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val activity = activity as CoreMainActivity
    noOpenBookButton.setOnClickListener {
      activity.navigate(
        KiwixReaderFragmentDirections.actionNavigationReaderToNavigationLibrary()
      )
    }
    activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    activity.setupDrawerToggle(toolbar)
    setFragmentContainerBottomMarginToSizeOfNavBar()
    openPageInBookFromNavigationArguments()
  }

  private fun openPageInBookFromNavigationArguments() {
    val args = KiwixReaderFragmentArgs.fromBundle(requireArguments())
    if (args.pageUrl.isNotEmpty()) {
      if (args.zimFileUri.isNotEmpty()) {
        tryOpeningZimFile(args)
      }
      loadUrlWithCurrentWebview(args.pageUrl)
      requireArguments().clear()
    } else {
      manageExternalLaunchAndRestoringViewState(args.zimFileUri)
    }
  }

  private fun tryOpeningZimFile(args: KiwixReaderFragmentArgs) {
    val file = File(args.zimFileUri)
    if (!file.exists()) {
      activity.toast(R.string.error_file_not_found)
      return
    }
    openZimFile(file)
  }

  override fun loadDrawerViews() {
    drawerLayout = requireActivity().findViewById(R.id.navigation_container)
    tableDrawerRightContainer = requireActivity().findViewById(R.id.reader_drawer_nav_view)
  }

  private fun exitBook() {
    showNoBookOpenViews()
    bottomToolbar.visibility = GONE
    actionBar.title = getString(R.string.reader)
    contentFrame.visibility = GONE
    mainMenu?.hideBookSpecificMenuItems()
    closeZimBook()
  }

  private fun closeZimBook() {
    zimReaderContainer.setZimFile(null)
  }

  override fun openHomeScreen() {
    Handler().postDelayed({
      if (webViewList.size == 0) {
        hideTabSwitcher()
      }
    }, HIDE_TAB_SWITCHER_DELAY)
  }

  override fun hideTabSwitcher() {
    if (actionBar != null) {
      actionBar.setDisplayShowTitleEnabled(true)
      activity?.setupDrawerToggle(toolbar)

      setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

      closeAllTabsButton.setImageDrawableCompat(R.drawable.ic_close_black_24dp)
      if (tabSwitcherRoot.visibility == View.VISIBLE) {
        tabSwitcherRoot.visibility = GONE
        startAnimation(tabSwitcherRoot, anim.slide_up)
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0
        contentFrame.visibility = View.VISIBLE
      }
      if (mainMenu != null) {
        mainMenu.showWebViewOptions(true)
      }
      if (webViewList.isEmpty()) {
        exitBook()
      } else {
        selectTab(currentWebViewIndex)
      }
    }
  }

  private fun setFragmentContainerBottomMarginToSizeOfNavBar() {
    val actionBarHeight = context?.getAttribute(android.R.attr.actionBarSize)
    if (actionBarHeight != null) {
      setParentFragmentsBottomMarginTo(
        complexToDimensionPixelSize(
          actionBarHeight,
          resources.displayMetrics
        )
      )
    }
  }

  private fun setParentFragmentsBottomMarginTo(margin: Int) {
    val params = parentFragment?.view?.layoutParams as ViewGroup.MarginLayoutParams?
    params?.bottomMargin = margin
    parentFragment?.view?.requestLayout()
  }

  override fun onPause() {
    super.onPause()
    // ScrollingViewWithBottomNavigationBehavior changes the margin to the size of the nav bar,
    // this resets the margin to zero, before fragment navigation.
    setParentFragmentsBottomMarginTo(0)
  }

  override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, menuInflater)
    if (zimReaderContainer.zimFileReader == null) {
      mainMenu?.hideBookSpecificMenuItems()
    }
  }

  override fun onCreateOptionsMenu(
    menu: Menu,
    activity: AppCompatActivity
  ): Super = ShouldCall

  override fun onResume() {
    super.onResume()
    if (zimReaderContainer.zimFile == null) {
      exitBook()
    }
  }

  override fun onBackPressed(activity: AppCompatActivity): Super {
    val callType = super.onBackPressed(activity)
    if (callType == ShouldCall && getCurrentWebView().canGoBack()) {
      getCurrentWebView().goBack()
    } else if (callType == ShouldCall) {
      getActivity()?.finish()
    }
    return ShouldNotCall
  }

  private fun manageExternalLaunchAndRestoringViewState(uri: String) {
    val settings = getSharedPrefSettings()
    val zimFile = settings?.getString(TAG_CURRENT_FILE, null)
    if (zimFile != null && File(zimFile).exists()) {
      Log.d(
        TAG_KIWIX,
        "Kiwix normal start, zimFile loaded last time -> Open last used zimFile $zimFile"
      )
      webViewList.clear()
      restoreTabStates()
    } else {
      Log.d(TAG_KIWIX, "Kiwix normal start, no zimFile loaded last time  -> display home page")
      exitBook()
    }
  }

  private fun restoreTabStates() {
    val settings = requireActivity().getSharedPreferences(SharedPreferenceUtil.PREF_KIWIX_MOBILE, 0)
    val zimFile = settings.getString(TAG_CURRENT_FILE, null)
    val zimArticles = settings.getString(TAG_CURRENT_ARTICLES, null)
    val zimPositions = settings.getString(TAG_CURRENT_POSITIONS, null)

    val currentTab = settings.getInt(TAG_CURRENT_TAB, 0)

    if (zimFile != null) {
      openZimFile(File(zimFile))
    } else {
      getCurrentWebView().snack(R.string.zim_not_opened)
    }
    try {
      val urls = JSONArray(zimArticles)
      val positions = JSONArray(zimPositions)
      var i = 0
      getCurrentWebView().loadUrl(UpdateUtils.reformatProviderUrl(urls.getString(i)))
      getCurrentWebView().scrollY = positions.getInt(i)
      i++
      while (i < urls.length()) {
        newTab(UpdateUtils.reformatProviderUrl(urls.getString(i)))
        safelyGetWebView(i).scrollY = positions.getInt(i)
        i++
      }
      selectTab(currentTab)
    } catch (e: Exception) {
      Log.w(TAG_KIWIX, "Kiwix shared preferences corrupted", e)
      // TODO: Show to user
    }
  }

  override fun createWebView(attrs: AttributeSet): ToolbarScrollingKiwixWebView {
    return ToolbarScrollingKiwixWebView(
      activity, this, attrs, activityMainRoot as ViewGroup, videoView,
      CoreWebViewClient(this, zimReaderContainer),
      toolbarContainer, bottomToolbar, requireActivity().bottom_nav_view,
      sharedPreferenceUtil
    )
  }

  override fun openFullScreen() {
    super.openFullScreen()
    requireActivity().bottom_nav_view.visibility = GONE
    setParentFragmentsBottomMarginTo(0)
    getCurrentWebView().translationY = 0f
  }

  override fun closeFullScreen() {
    super.closeFullScreen()
    requireActivity().bottom_nav_view.visibility = VISIBLE
    setFragmentContainerBottomMarginToSizeOfNavBar()
  }

  private fun getSharedPrefSettings() =
    activity?.getSharedPreferences(SharedPreferenceUtil.PREF_KIWIX_MOBILE, 0)

  override fun getIconResId() = R.mipmap.ic_launcher

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
}
