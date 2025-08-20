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
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.data.remote.ObjectBoxToLibkiwixMigrator
import org.kiwix.kiwixmobile.core.data.remote.ObjectBoxToRoomMigrator
import org.kiwix.kiwixmobile.core.di.components.CoreActivityComponent
import org.kiwix.kiwixmobile.core.downloader.DownloadMonitor
import org.kiwix.kiwixmobile.core.downloader.downloadManager.APP_NAME_KEY
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadMonitorService
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadMonitorService.Companion.STOP_DOWNLOAD_SERVICE
import org.kiwix.kiwixmobile.core.error.ErrorActivity
import org.kiwix.kiwixmobile.core.extensions.browserIntent
import org.kiwix.kiwixmobile.core.extensions.isServiceRunning
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
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

// Fragments names for compose based navigation.
const val READER_FRAGMENT = "readerFragment"
const val LOCAL_LIBRARY_FRAGMENT = "localLibraryFragment"
const val DOWNLOAD_FRAGMENT = "downloadsFragment"
const val BOOKMARK_FRAGMENT = "bookmarkFragment"
const val NOTES_FRAGMENT = "notesFragment"
const val INTRO_FRAGMENT = "introFragment"
const val HISTORY_FRAGMENT = "historyFragment"
const val LANGUAGE_FRAGMENT = "languageFragment"
const val ZIM_HOST_FRAGMENT = "zimHostFragment"
const val HELP_FRAGMENT = "helpFragment"
const val SETTINGS_FRAGMENT = "settingsFragment"
const val SEARCH_FRAGMENT = "searchFragment"
const val LOCAL_FILE_TRANSFER_FRAGMENT = "localFileTransferFragment"

// Zim host deep link for opening the ZimHost fragment from notification.
const val ZIM_HOST_DEEP_LINK_SCHEME = "kiwix"
const val ZIM_HOST_NAV_DEEP_LINK = "$ZIM_HOST_DEEP_LINK_SCHEME://zimhost"

// Left drawer items testing tag.
const val LEFT_DRAWER_BOOKMARK_ITEM_TESTING_TAG = "leftDrawerBookmarkItemTestingTag"
const val LEFT_DRAWER_HISTORY_ITEM_TESTING_TAG = "leftDrawerHistoryItemTestingTag"
const val LEFT_DRAWER_NOTES_ITEM_TESTING_TAG = "leftDrawerNotesItemTestingTag"
const val LEFT_DRAWER_SETTINGS_ITEM_TESTING_TAG = "leftDrawerSettingsItemTestingTag"
const val LEFT_DRAWER_SUPPORT_ITEM_TESTING_TAG = "leftDrawerSupportItemTestingTag"
const val LEFT_DRAWER_HELP_ITEM_TESTING_TAG = "leftDrawerHelpItemTestingTag"
const val LEFT_DRAWER_ZIM_HOST_ITEM_TESTING_TAG = "leftDrawerZimHostItemTestingTag"
const val LEFT_DRAWER_ABOUT_APP_ITEM_TESTING_TAG = "leftDrawerAboutAppItemTestingTag"

abstract class CoreMainActivity : BaseActivity(), WebViewProvider {
  abstract val searchFragmentRoute: String

  @Inject lateinit var alertDialogShower: AlertDialogShower

  @Inject lateinit var externalLinkOpener: ExternalLinkOpener

  @Inject lateinit var rateDialogHandler: RateDialogHandler
  private var drawerToggle: ActionBarDrawerToggle? = null

  @Inject lateinit var zimReaderContainer: ZimReaderContainer

  /**
   * We have migrated the UI in compose, so providing the compose based navigation to activity
   * is responsibility of child activities such as KiwixMainActivity, and CustomMainActivity.
   */
  lateinit var navController: NavHostController
  val isNavControllerInitialized: Boolean
    get() = ::navController.isInitialized

  /**
   * For managing the leftDrawer.
   */
  lateinit var leftDrawerState: DrawerState

  /**
   * The compose coroutine scope for calling the compose based UI elements in coroutine scope.
   * Such as opening/closing leftDrawer.
   */
  lateinit var uiCoroutineScope: CoroutineScope

  /**
   * Managing the leftDrawerMenu in compose way so that when app's language changed
   * it will update the text in selected language.
   */
  protected val leftDrawerMenu = mutableStateListOf<DrawerMenuGroup>()

  /**
   * Manages the enabling/disabling the left drawer
   */
  val enableLeftDrawer = mutableStateOf(true)

  /**
   * For managing the back press of fragments.
   */
  val customBackHandler = mutableStateOf<(() -> FragmentActivityExtensions.Super)?>(null)

  /**
   * For managing the the showing/hiding the bottomAppBar when scrolling.
   */
  @OptIn(ExperimentalMaterial3Api::class)
  var bottomAppBarScrollBehaviour: BottomAppBarScrollBehavior? = null
  abstract val bookmarksFragmentRoute: String
  abstract val settingsFragmentRoute: String
  abstract val historyFragmentRoute: String
  abstract val notesFragmentRoute: String
  abstract val helpFragmentRoute: String
  abstract val cachedComponent: CoreActivityComponent
  abstract val topLevelDestinationsRoute: Set<String>
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
    leftDrawerMenu.addAll(leftNavigationDrawerMenuItems)
  }

  @Suppress("DEPRECATION")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    activeFragments().iterator().forEach { it.onActivityResult(requestCode, resultCode, data) }
  }

  override fun onStart() {
    super.onStart()
    externalLinkOpener.setAlertDialogShower(alertDialogShower)
    rateDialogHandler.setAlertDialogShower(alertDialogShower)
    downloadMonitor.startMonitoringDownload()
    stopDownloadServiceIfRunning()
    rateDialogHandler.checkForRateDialog(getIconResId())
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
      startService(
        Intent(this, DownloadMonitorService::class.java).apply {
          putExtra(APP_NAME_KEY, appName)
        }
      )
    }
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

  fun enableLeftDrawer() {
    enableLeftDrawer.value = true
  }

  open fun disableLeftDrawer() {
    enableLeftDrawer.value = false
  }

  protected fun openHelpFragment() {
    handleDrawerOnNavigation()
    navigate(helpFragmentRoute)
  }

  fun navigationDrawerIsOpen(): Boolean = leftDrawerState.isOpen

  fun closeNavigationDrawer() {
    uiCoroutineScope.launch {
      leftDrawerState.close()
    }
  }

  fun openNavigationDrawer() {
    uiCoroutineScope.launch {
      leftDrawerState.open()
    }
  }

  fun openSupportKiwixExternalLink() {
    closeNavigationDrawer()
    externalLinkOpener.openExternalUrl(KIWIX_SUPPORT_URL.toUri().browserIntent(), false)
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

  fun navigate(route: String, navOptions: NavOptions? = null) {
    navController.navigate(route, navOptions)
  }

  fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit) {
    navigate(route, navOptions(builder))
  }

  private fun openSettings() {
    handleDrawerOnNavigation()
    navigate(settingsFragmentRoute)
  }

  private fun openHistory() {
    handleDrawerOnNavigation()
    navigate(historyFragmentRoute)
  }

  abstract fun openSearch(
    searchString: String = "",
    isOpenedFromTabView: Boolean = false,
    isVoice: Boolean = false
  )

  abstract fun openPage(
    pageUrl: String,
    zimReaderSource: ZimReaderSource? = null,
    shouldOpenInNewTab: Boolean = false
  )

  private fun openBookmarks() {
    handleDrawerOnNavigation()
    navigate(bookmarksFragmentRoute)
  }

  private fun openNotes() {
    handleDrawerOnNavigation()
    navigate(notesFragmentRoute)
  }

  protected fun handleDrawerOnNavigation() {
    closeNavigationDrawer()
    disableLeftDrawer()
    removeArgumentsOfReaderScreen()
  }

  private fun setMainActivityToCoreApp() {
    (applicationContext as CoreApp).setMainActivity(this)
  }

  fun findInPage(searchString: String) {
    navigate("$readerFragmentRoute?$FIND_IN_PAGE_SEARCH_STRING=$searchString")
  }

  private val bookRelatedDrawerGroup by lazy {
    DrawerMenuGroup(
      listOfNotNull(
        DrawerMenuItem(
          title = CoreApp.instance.getString(R.string.bookmarks),
          iconRes = R.drawable.ic_bookmark_black_24dp,
          visible = true,
          onClick = { openBookmarks() },
          testingTag = LEFT_DRAWER_BOOKMARK_ITEM_TESTING_TAG
        ),
        DrawerMenuItem(
          title = CoreApp.instance.getString(R.string.history),
          iconRes = R.drawable.ic_history_24px,
          visible = true,
          onClick = { openHistory() },
          testingTag = LEFT_DRAWER_HISTORY_ITEM_TESTING_TAG
        ),
        DrawerMenuItem(
          title = CoreApp.instance.getString(R.string.pref_notes),
          iconRes = R.drawable.ic_add_note,
          visible = true,
          onClick = { openNotes() },
          testingTag = LEFT_DRAWER_NOTES_ITEM_TESTING_TAG
        ),
        zimHostDrawerMenuItem
      )
    )
  }

  private val settingDrawerGroup by lazy {
    DrawerMenuGroup(
      listOf(
        DrawerMenuItem(
          title = CoreApp.instance.getString(R.string.menu_settings),
          iconRes = R.drawable.ic_settings_24px,
          visible = true,
          onClick = { openSettings() },
          testingTag = LEFT_DRAWER_SETTINGS_ITEM_TESTING_TAG
        )
      )
    )
  }

  private val helpAndSupportDrawerGroup by lazy {
    DrawerMenuGroup(
      listOfNotNull(
        helpDrawerMenuItem,
        supportDrawerMenuItem,
        aboutAppDrawerMenuItem
      )
    )
  }

  /**
   * Returns the "Wi-Fi Hotspot" menu item in the left drawer.
   * Currently, this feature is only included in the main Kiwix app.
   * Custom apps do not include this item.
   */
  abstract val zimHostDrawerMenuItem: DrawerMenuItem?

  /**
   * Returns the "Help" menu item in the left drawer.
   * In custom apps, this item is hidden.
   * Each app (main Kiwix or custom) provides its own implementation.
   */
  abstract val helpDrawerMenuItem: DrawerMenuItem?

  /**
   * Returns the "Support" menu item in the left drawer.
   * In custom apps, this item displays the application name dynamically.
   * Child activities are responsible for defining this drawer item.
   */
  abstract val supportDrawerMenuItem: DrawerMenuItem?

  /**
   * Returns the "About App" menu item in the left drawer.
   * For custom apps, this item is shown if configured.
   * It is not included in the main Kiwix app.
   * Child activities are responsible for defining this drawer item.
   */
  abstract val aboutAppDrawerMenuItem: DrawerMenuItem?

  protected val leftNavigationDrawerMenuItems by lazy {
    listOf<DrawerMenuGroup>(
      bookRelatedDrawerGroup,
      settingDrawerGroup,
      helpAndSupportDrawerGroup
    )
  }

  protected abstract fun getIconResId(): Int
  abstract val readerFragmentRoute: String
  abstract fun createApplicationShortcuts()
  abstract fun hideBottomAppBar()
  abstract fun showBottomAppBar()
  abstract fun removeArgumentsOfReaderScreen()
}
