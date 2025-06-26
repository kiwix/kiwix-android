/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.core.main

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.os.Process
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED
import androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions.Super.ShouldCall
import org.kiwix.kiwixmobile.core.data.remote.ObjectBoxToLibkiwixMigrator
import org.kiwix.kiwixmobile.core.data.remote.ObjectBoxToRoomMigrator
import org.kiwix.kiwixmobile.core.di.components.CoreActivityComponent
import org.kiwix.kiwixmobile.core.downloader.DownloadMonitor
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadMonitorService
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadMonitorService.Companion.STOP_DOWNLOAD_SERVICE
import org.kiwix.kiwixmobile.core.error.ErrorActivity
import org.kiwix.kiwixmobile.core.extensions.browserIntent
import org.kiwix.kiwixmobile.core.extensions.isServiceRunning
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.search.NAV_ARG_SEARCH_STRING
import org.kiwix.kiwixmobile.core.utils.EXTRA_IS_WIDGET_VOICE
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.TAG_FROM_TAB_SWITCHER
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.RateDialogHandler
import javax.inject.Inject
import kotlin.system.exitProcess

const val KIWIX_SUPPORT_URL = "https://www.kiwix.org/support"
const val PAGE_URL_KEY = "pageUrl"
const val SHOULD_OPEN_IN_NEW_TAB = "shouldOpenInNewTab"
const val FIND_IN_PAGE_SEARCH_STRING = "findInPageSearchString"
const val ZIM_FILE_URI_KEY = "zimFileUri"
const val KIWIX_INTERNAL_ERROR = 10
const val ACTION_NEW_TAB = "NEW_TAB"
const val NEW_TAB_SHORTCUT_ID = "new_tab_shortcut"

abstract class CoreMainActivity : BaseActivity(), WebViewProvider {
  abstract val searchFragmentResId: Int

  @Inject lateinit var alertDialogShower: AlertDialogShower

  @Inject lateinit var externalLinkOpener: ExternalLinkOpener

  @Inject lateinit var rateDialogHandler: RateDialogHandler
  private var drawerToggle: ActionBarDrawerToggle? = null

  @Inject lateinit var zimReaderContainer: ZimReaderContainer
  abstract val navController: NavController
  abstract val drawerContainerLayout: DrawerLayout
  abstract val drawerNavView: NavigationView
  abstract val readerTableOfContentsDrawer: NavigationView
  abstract val bookmarksFragmentResId: Int
  abstract val settingsFragmentResId: Int
  abstract val historyFragmentResId: Int
  abstract val notesFragmentResId: Int
  abstract val helpFragmentResId: Int
  abstract val cachedComponent: CoreActivityComponent
  abstract val topLevelDestinations: Set<Int>
  abstract val navHostContainer: FragmentContainerView
  abstract val mainActivity: AppCompatActivity
  abstract val appName: String

  @Inject lateinit var objectBoxToLibkiwixMigrator: ObjectBoxToLibkiwixMigrator

  @Inject lateinit var objectBoxToRoomMigrator: ObjectBoxToRoomMigrator

  @Inject
  lateinit var downloadMonitor: DownloadMonitor

  @Suppress("InjectDispatcher")
  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.KiwixTheme)
    super.onCreate(savedInstanceState)
    if (!BuildConfig.DEBUG) {
      val appContext = applicationContext
      Thread.setDefaultUncaughtExceptionHandler { paramThread: Thread?, paramThrowable: Throwable? ->
        val intent = Intent(appContext, ErrorActivity::class.java)
        val extras = Bundle()
        extras.putSerializable(ErrorActivity.EXCEPTION_KEY, paramThrowable)
        intent.putExtras(extras)
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
        finish()
        Process.killProcess(Process.myPid())
        exitProcess(KIWIX_INTERNAL_ERROR)
      }
    }

    setMainActivityToCoreApp()
    // run the migration on background thread to avoid any UI related issues.
    CoroutineScope(Dispatchers.IO).launch {
      objectBoxToLibkiwixMigrator.migrateObjectBoxDataToLibkiwix()
    }
    // run the migration on background thread to avoid any UI related issues.
    CoroutineScope(Dispatchers.IO).launch {
      objectBoxToRoomMigrator.migrateObjectBoxDataToRoom()
    }
    lifecycleScope.launch(Dispatchers.IO) {
      createApplicationShortcuts()
    }
    handleBackPressed()
  }

  @Suppress("DEPRECATION")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    activeFragments().iterator().forEach { it.onActivityResult(requestCode, resultCode, data) }
  }

  override fun onStart() {
    super.onStart()
    setDialogHostToActivity(alertDialogShower)
    externalLinkOpener.setAlertDialogShower(alertDialogShower)
    rateDialogHandler.setAlertDialogShower(alertDialogShower)
    downloadMonitor.startMonitoringDownload()
    stopDownloadServiceIfRunning()
    rateDialogHandler.checkForRateDialog(getIconResId())
    navController.addOnDestinationChangedListener { _, destination, _ ->
      configureActivityBasedOn(destination)
    }
  }

  /**
   * Stops the DownloadService if it is currently running,
   * as the application is now in the foreground and can handle downloads directly.
   */
  private fun stopDownloadServiceIfRunning() {
    if (isServiceRunning(DownloadMonitorService::class.java)) {
      startService(
        Intent(
          this,
          DownloadMonitorService::class.java
        ).setAction(STOP_DOWNLOAD_SERVICE)
      )
    }
  }

  override fun onDestroy() {
    onBackPressedCallBack.remove()
    super.onDestroy()
  }

  override fun onStop() {
    startMonitoringDownloads()
    downloadMonitor.stopListeningDownloads()
    super.onStop()
  }

  /**
   * Starts monitoring the downloads by ensuring that the `DownloadMonitorService` is running.
   * This service keeps the Fetch instance alive when the application is in the background
   *  or has been killed by the user or system, allowing downloads to continue in the background.
   */
  private fun startMonitoringDownloads() {
    if (!isServiceRunning(DownloadMonitorService::class.java)) {
      startService(Intent(this, DownloadMonitorService::class.java))
    }
  }

  open fun configureActivityBasedOn(destination: NavDestination) {
    if (destination.id !in topLevelDestinations) {
      handleDrawerOnNavigation()
    }
    readerTableOfContentsDrawer.setLockMode(
      if (destination.id == readerFragmentResId) {
        LOCK_MODE_UNLOCKED
      } else {
        LOCK_MODE_LOCKED_CLOSED
      }
    )
  }

  private fun NavigationView.setLockMode(lockMode: Int) {
    drawerContainerLayout.setDrawerLockMode(lockMode, this)
  }

  @Suppress("DEPRECATION")
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    activeFragments().iterator().forEach {
      it.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (drawerToggle?.isDrawerIndicatorEnabled == true) {
      return drawerToggle?.onOptionsItemSelected(item) == true
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onActionModeStarted(mode: ActionMode) {
    super.onActionModeStarted(mode)
    activeFragments().filterIsInstance<FragmentActivityExtensions>().forEach {
      it.onActionModeStarted(mode, this)
    }
  }

  override fun onActionModeFinished(mode: ActionMode) {
    super.onActionModeFinished(mode)
    activeFragments().filterIsInstance<FragmentActivityExtensions>().forEach {
      it.onActionModeFinished(mode, this)
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    this.intent.action = intent.action
    activeFragments().filterIsInstance<FragmentActivityExtensions>().forEach {
      it.onNewIntent(intent, this)
    }
  }

  override fun getCurrentWebView(): KiwixWebView? {
    return activeFragments().filterIsInstance<WebViewProvider>().firstOrNull()
      ?.getCurrentWebView()
  }

  override fun onSupportNavigateUp(): Boolean =
    navController.navigateUp() || super.onSupportNavigateUp()

  open fun setupDrawerToggle(shouldEnableRightDrawer: Boolean = false) {
    // Set the initial contentDescription to the hamburger icon.
    // This method is called from various locations after modifying the navigationIcon.
    // For example, we previously changed this icon/contentDescription to the "+" button
    // when opening the tabSwitcher. After closing the tabSwitcher, we reset the
    // contentDescription to the default hamburger icon.
    // Todo we will refactore this when migrating the CoreMainActivity.
    // toolbar.getToolbarNavigationIcon()?.setToolTipWithContentDescription(
    //   getString(R.string.open_drawer)
    // )
    drawerToggle =
      ActionBarDrawerToggle(
        this,
        drawerContainerLayout,
        R.string.open_drawer,
        R.string.close_drawer
      )
    drawerToggle?.let {
      drawerContainerLayout.addDrawerListener(it)
      it.syncState()
    }
    drawerContainerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    if (shouldEnableRightDrawer) {
      // Enable the right drawer
      drawerContainerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
    }
  }

  open fun disableDrawer(disableRightDrawer: Boolean = true) {
    drawerToggle?.isDrawerIndicatorEnabled = false
    drawerContainerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    if (disableRightDrawer) {
      // Disable the right drawer
      drawerContainerLayout.setDrawerLockMode(
        DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
        GravityCompat.END
      )
    }
  }

  open fun onNavigationItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_support_kiwix -> openSupportKiwixExternalLink()
      R.id.menu_settings -> openSettings()
      R.id.menu_help -> openHelpFragment()
      R.id.menu_notes -> openNotes()
      R.id.menu_history -> openHistory()
      R.id.menu_bookmarks_list -> openBookmarks()
      else -> return false
    }
    return true
  }

  private fun openHelpFragment() {
    navigate(helpFragmentResId)
    handleDrawerOnNavigation()
  }

  fun navigationDrawerIsOpen(): Boolean =
    drawerContainerLayout.isDrawerOpen(drawerNavView)

  fun closeNavigationDrawer() {
    drawerContainerLayout.closeDrawer(drawerNavView)
  }

  fun openNavigationDrawer() {
    drawerContainerLayout.openDrawer(drawerNavView)
  }

  fun openSupportKiwixExternalLink() {
    externalLinkOpener.openExternalUrl(KIWIX_SUPPORT_URL.toUri().browserIntent(), false)
  }

  private fun handleBackPressed() {
    onBackPressedDispatcher.addCallback(this, onBackPressedCallBack)
  }

  private val onBackPressedCallBack =
    object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if (navigationDrawerIsOpen()) {
          closeNavigationDrawer()
          return
        }
        if (activeFragments().filterIsInstance<FragmentActivityExtensions>().isEmpty()) {
          isEnabled = false
          return onBackPressedDispatcher.onBackPressed().also {
            isEnabled = true
          }
        }
        activeFragments().filterIsInstance<FragmentActivityExtensions>().forEach {
          if (it.onBackPressed(this@CoreMainActivity) == ShouldCall) {
            if (navController.currentDestination?.id?.equals(readerFragmentResId) == true &&
              navController.previousBackStackEntry?.destination
                ?.id?.equals(searchFragmentResId) == false
            ) {
              drawerToggle = null
              finish()
            } else {
              isEnabled = false
              onBackPressedDispatcher.onBackPressed()
              isEnabled = true
            }
          }
        }
      }
    }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    if (activeFragments().filterIsInstance<FragmentActivityExtensions>().isEmpty()) {
      return super.onCreateOptionsMenu(menu)
    }
    var returnValue = true
    activeFragments().filterIsInstance<FragmentActivityExtensions>().forEach {
      if (it.onCreateOptionsMenu(menu, this) == FragmentActivityExtensions.Super.ShouldCall) {
        returnValue = super.onCreateOptionsMenu(menu)
      }
    }
    return returnValue
  }

  private fun activeFragments(): MutableList<Fragment> =
    supportFragmentManager.fragments

  fun navigate(action: NavDirections) {
    navController.currentDestination?.getAction(action.actionId)?.run {
      navController.navigate(action)
    }
  }

  fun navigate(fragmentId: Int) {
    navController.navigate(fragmentId)
  }

  fun navigate(fragmentId: Int, bundle: Bundle) {
    navController.navigate(fragmentId, bundle)
  }

  fun navigate(fragmentId: Int, bundle: Bundle, navOptions: NavOptions) {
    navController.navigate(fragmentId, bundle, navOptions)
  }

  private fun openSettings() {
    handleDrawerOnNavigation()
    navigate(settingsFragmentResId)
  }

  private fun openHistory() {
    navigate(historyFragmentResId)
  }

  fun openSearch(
    searchString: String = "",
    isOpenedFromTabView: Boolean = false,
    isVoice: Boolean = false
  ) {
    navigate(
      searchFragmentResId,
      bundleOf(
        NAV_ARG_SEARCH_STRING to searchString,
        TAG_FROM_TAB_SWITCHER to isOpenedFromTabView,
        EXTRA_IS_WIDGET_VOICE to isVoice
      )
    )
  }

  fun openZimFromFilePath(path: String) {
    navigate(
      readerFragmentResId,
      bundleOf(
        ZIM_FILE_URI_KEY to path,
      )
    )
  }

  fun openPage(
    pageUrl: String,
    zimReaderSource: ZimReaderSource? = null,
    shouldOpenInNewTab: Boolean = false
  ) {
    var zimFileUri = ""
    if (zimReaderSource != null) {
      zimFileUri = zimReaderSource.toDatabase()
    }
    val navOptions = NavOptions.Builder()
      .setLaunchSingleTop(true)
      .setPopUpTo(readerFragmentResId, inclusive = true)
      .build()
    navigate(
      readerFragmentResId,
      bundleOf(
        PAGE_URL_KEY to pageUrl,
        ZIM_FILE_URI_KEY to zimFileUri,
        SHOULD_OPEN_IN_NEW_TAB to shouldOpenInNewTab
      ),
      navOptions
    )
  }

  private fun openBookmarks() {
    navigate(bookmarksFragmentResId)
    handleDrawerOnNavigation()
  }

  private fun openNotes() {
    navigate(notesFragmentResId)
  }

  protected fun handleDrawerOnNavigation() {
    closeNavigationDrawer()
    disableDrawer()
  }

  private fun setMainActivityToCoreApp() {
    (applicationContext as CoreApp).setMainActivity(this)
  }

  fun findInPage(searchString: String) {
    navigate(readerFragmentResId, bundleOf(FIND_IN_PAGE_SEARCH_STRING to searchString))
  }

  protected abstract fun getIconResId(): Int
  abstract val readerFragmentResId: Int
  abstract fun createApplicationShortcuts()
  abstract fun setDialogHostToActivity(alertDialogShower: AlertDialogShower)

  /**
   * This is for showing and hiding the bottomNavigationView when user scroll the screen.
   * We are making this abstract so that it can be easily used from the reader screen.
   * Since we do not have the bottomNavigationView in custom apps. So doing this way both apps will
   * provide there own implementation.
   *
   * TODO we will remove this once we will migrate mainActivity to the compose.
   */
  abstract fun toggleBottomNavigation(isVisible: Boolean)
}
