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

package org.kiwix.kiwixmobile.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ActionMode
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.compose.rememberNavController
import eu.mhutti1.utils.storage.StorageDevice
import eu.mhutti1.utils.storage.StorageDeviceUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kiwix.kiwixmobile.BuildConfig
import org.kiwix.kiwixmobile.KiwixApp
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.drawable
import org.kiwix.kiwixmobile.core.R.mipmap
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DOWNLOAD_NOTIFICATION_TITLE
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DOWNLOAD_TIMEOUT_RESUME_INTENT
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.setNavigationResultOnCurrent
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.extensions.update
import org.kiwix.kiwixmobile.core.main.ACTION_NEW_TAB
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.DrawerMenuItem
import org.kiwix.kiwixmobile.core.main.LEFT_DRAWER_HELP_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.LEFT_DRAWER_SUPPORT_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.LEFT_DRAWER_ZIM_HOST_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.NEW_TAB_SHORTCUT_ID
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.main.ZIM_HOST_DEEP_LINK_SCHEME
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_PREFIX
import org.kiwix.kiwixmobile.core.utils.HUNDERED
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.kiwixActivityComponent
import org.kiwix.kiwixmobile.ui.KiwixDestination
import javax.inject.Inject

const val ACTION_GET_CONTENT = "GET_CONTENT"
const val OPENING_ZIM_FILE_DELAY = 300L
const val GET_CONTENT_SHORTCUT_ID = "get_content_shortcut"

class KiwixMainActivity : CoreMainActivity() {
  private var actionMode: ActionMode? = null
  override val cachedComponent by lazy { kiwixActivityComponent }
  override val searchFragmentRoute: String = KiwixDestination.Search.route

  @Inject lateinit var libkiwixBookOnDisk: LibkiwixBookOnDisk

  @Inject
  lateinit var kiwixDataStore: KiwixDataStore

  override val mainActivity: AppCompatActivity by lazy { this }
  override val appName: String by lazy { getString(R.string.app_name) }

  override val bookmarksFragmentRoute: String = KiwixDestination.Bookmarks.route
  override val settingsFragmentRoute: String = KiwixDestination.Settings.route
  override val historyFragmentRoute: String = KiwixDestination.History.route
  override val notesFragmentRoute: String = KiwixDestination.Notes.route
  override val readerFragmentRoute: String = KiwixDestination.Reader.route
  override val helpFragmentRoute: String = KiwixDestination.Help.route
  override val topLevelDestinationsRoute =
    setOf(
      KiwixDestination.Downloads.route,
      KiwixDestination.Library.route,
      KiwixDestination.Reader.route
    )

  private val shouldShowBottomAppBar = mutableStateOf(true)

  private var isIntroScreenVisible: Boolean = false

  private val finishActionModeOnDestinationChange =
    NavController.OnDestinationChangedListener { _, _, _ ->
      actionMode?.finish()
    }
  private val storageDeviceList = arrayListOf<StorageDevice>()
  private val pendingIntentFlow = MutableStateFlow<Intent?>(null)

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    cachedComponent.inject(this)
    super.onCreate(savedInstanceState)
    setContent {
      val pendingIntent by pendingIntentFlow.collectAsState()
      navController = rememberNavController()
      leftDrawerState = rememberDrawerState(DrawerValue.Closed)
      uiCoroutineScope = rememberCoroutineScope()
      bottomAppBarScrollBehaviour = BottomAppBarDefaults.exitAlwaysScrollBehavior()
      val startDestination = remember {
        if (runBlocking { kiwixDataStore.showIntro.first() } && !isIntroScreenNotVisible()) {
          KiwixDestination.Intro.route
        } else {
          KiwixDestination.Reader.route
        }
      }
      RestoreDrawerStateOnOrientationChange()
      PersistDrawerStateOnChange()
      KiwixMainActivityScreen(
        navController = navController,
        leftDrawerContent = leftDrawerMenu,
        startDestination = startDestination,
        topLevelDestinationsRoute = topLevelDestinationsRoute,
        leftDrawerState = leftDrawerState,
        uiCoroutineScope = uiCoroutineScope,
        enableLeftDrawer = enableLeftDrawer.value,
        shouldShowBottomAppBar = shouldShowBottomAppBar.value,
        bottomAppBarScrollBehaviour = bottomAppBarScrollBehaviour,
        viewModelFactory = viewModelFactory,
        alertDialogShower = alertDialogShower
      )
      LaunchedEffect(Unit) {
        // Load the menu when UI is attached to screen.
        leftDrawerMenu.addAll(leftNavigationDrawerMenuItems)
      }
      LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener(finishActionModeOnDestinationChange)
      }
      val lifecycle = LocalLifecycleOwner.current.lifecycle
      LaunchedEffect(navController, pendingIntent) {
        snapshotFlow { pendingIntent }
          .filterNotNull()
          .collectLatest { intent ->
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
              // Wait until fragment manager is fully initialized and view hierarchy is ready
              delay(HUNDERED.toLong())
              handleAllIntents(intent)
              pendingIntentFlow.value = null
            }
          }
      }
      DialogHost(alertDialogShower)
    }
    runMigrations()
    intent?.let {
      pendingIntentFlow.value = it
    }
  }

  @Suppress("InjectDispatcher")
  private fun runMigrations() {
    lifecycleScope.launch {
      migrateInternalToPublicAppDirectory()
      migratedToPerAppLanguage()
    }
    // run the migration on background thread to avoid any UI related issues.
    CoroutineScope(Dispatchers.IO).launch {
      (applicationContext as KiwixApp).kiwixComponent
        .provideObjectBoxDataMigrationHandler()
        .migrate()
    }
  }

  private fun handleAllIntents(newIntent: Intent?) {
    newIntent?.let { intent ->
      handleZimFileIntent(intent)
      handleNotificationIntent(intent)
      handleGetContentIntent(intent)
      safelyHandleDeepLink(intent)
      handleBackgroundTimeoutLimitIntent(intent)
    }
  }

  private fun handleBackgroundTimeoutLimitIntent(intent: Intent?) {
    if (intent?.hasExtra(DOWNLOAD_TIMEOUT_RESUME_INTENT) == true) {
      val currentId = navController.currentDestination?.id
      val targetId = navController.graph.findNode(KiwixDestination.Downloads.route)?.id

      if (currentId != targetId) {
        navigate(KiwixDestination.Downloads.route) {
          launchSingleTop = true
          popUpTo(navController.graph.findStartDestination().id)
        }
      }
    }
  }

  private fun safelyHandleDeepLink(intent: Intent) {
    if (intent.data != null && intent.extras != null) {
      navController.handleDeepLink(intent)
    }
  }

  private suspend fun migrateInternalToPublicAppDirectory() {
    if (!kiwixDataStore.isAppDirectoryMigrated.first()) {
      val storagePath =
        getStorageDeviceList()
          .getOrNull(kiwixDataStore.selectedStoragePosition.first())
          ?.name
      storagePath?.let {
        kiwixDataStore.setSelectedStorage(kiwixDataStore.getPublicDirectoryPath(it))
        kiwixDataStore.setAppDirectoryMigrated(true)
      }
    }
  }

  private suspend fun migratedToPerAppLanguage() {
    if (!kiwixDataStore.perAppLanguageMigrated.first()) {
      AppCompatDelegate.setApplicationLocales(
        LocaleListCompat.forLanguageTags(kiwixDataStore.prefLanguage.first())
      )
      kiwixDataStore.putPerAppLanguageMigration(true)
    }
  }

  /**
   * Fetches the storage device list once in the main activity and reuses it across all fragments.
   * This is necessary because retrieving the storage device list, especially on devices with large SD cards,
   * is a resource-intensive operation. Performing this operation repeatedly in fragments can negatively
   * affect the user experience, as it takes time and can block the UI.
   *
   * If a fragment is destroyed and we need to retrieve the device list again, performing the operation
   * repeatedly leads to inefficiency. To optimize this, we fetch the storage device list once and reuse
   * it in all fragments, thereby reducing redundant processing and improving performance.
   */
  suspend fun getStorageDeviceList(): List<StorageDevice> {
    if (storageDeviceList.isEmpty()) {
      storageDeviceList.addAll(StorageDeviceUtils.getWritableStorage(this))
    }
    return storageDeviceList
  }

  override fun onStart() {
    super.onStart()
    lifecycleScope.launch {
      if (!kiwixDataStore.prefIsTest.first()) {
        kiwixDataStore.setIsPlayStoreBuild(BuildConfig.IS_PLAYSTORE)
      }
    }
  }

  private fun isIntroScreenNotVisible(): Boolean =
    isIntroScreenVisible.also {
      isIntroScreenVisible = true
    }

  override fun onSupportActionModeStarted(mode: ActionMode) {
    super.onSupportActionModeStarted(mode)
    actionMode = mode
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    pendingIntentFlow.value = intent
    supportFragmentManager.fragments.filterIsInstance<FragmentActivityExtensions>().forEach {
      it.onNewIntent(intent, this)
    }
  }

  private fun handleGetContentIntent(intent: Intent?) {
    if (intent?.action == ACTION_GET_CONTENT) {
      navigate(KiwixDestination.Downloads.route) {
        launchSingleTop = true
        popUpTo(navController.graph.findStartDestination().id)
      }
    }
  }

  private fun handleZimFileIntent(intent: Intent?) {
    intent?.data?.let {
      when (it.scheme) {
        "file",
        "content" -> {
          Handler(Looper.getMainLooper()).postDelayed({
            openLocalLibraryWithZimFilePath("$it")
            clearIntentDataAndAction()
          }, OPENING_ZIM_FILE_DELAY)
        }

        "zim" -> {
          val zimId = it.host
          val page = it.encodedPath?.removePrefix("/")
          if (zimId.isNullOrEmpty() || page.isNullOrEmpty()) {
            return toast(R.string.cannot_open_file)
          }
          lifecycleScope.launch {
            delay(OPENING_ZIM_FILE_DELAY)
            val book = libkiwixBookOnDisk.bookById(zimId)
              ?: return@launch toast(R.string.cannot_open_file)
            openPage("$CONTENT_PREFIX$page", book.zimReaderSource)
            clearIntentDataAndAction()
          }
        }

        else -> {
          if (it.scheme != ZIM_HOST_DEEP_LINK_SCHEME) {
            toast(R.string.cannot_open_file)
          }
        }
      }
    }
  }

  private fun clearIntentDataAndAction() {
    // if used once then clear it to avoid affecting any other functionality
    // of the application.
    intent.action = null
    intent.data = null
  }

  private fun openLocalLibraryWithZimFilePath(path: String) {
    navigate(KiwixDestination.Library.createRoute(zimFileUri = path))
  }

  private fun handleNotificationIntent(intent: Intent?) {
    if (intent?.hasExtra(DOWNLOAD_NOTIFICATION_TITLE) == true) {
      lifecycleScope.launch {
        delay(OPENING_ZIM_FILE_DELAY)
        intent.getStringExtra(DOWNLOAD_NOTIFICATION_TITLE)?.let {
          libkiwixBookOnDisk.bookMatching(it)?.let { bookOnDiskEntity ->
            openZimFromFilePath(bookOnDiskEntity.zimReaderSource.toDatabase())
          }
        }
      }
    }
  }

  private fun openZimFromFilePath(path: String) {
    navigate(KiwixDestination.Reader.route)
    setNavigationResultOnCurrent(path, ZIM_FILE_URI_KEY)
  }

  override val zimHostDrawerMenuItem: DrawerMenuItem? by lazy {
    DrawerMenuItem(
      title = getString(string.menu_wifi_hotspot),
      iconRes = drawable.ic_mobile_screen_share_24px,
      visible = true,
      onClick = { openZimHostFragment() },
      testingTag = LEFT_DRAWER_ZIM_HOST_ITEM_TESTING_TAG
    )
  }

  override val helpDrawerMenuItem: DrawerMenuItem? by lazy {
    DrawerMenuItem(
      title = getString(string.menu_help),
      iconRes = drawable.ic_help_24px,
      visible = true,
      onClick = { openHelpFragment() },
      testingTag = LEFT_DRAWER_HELP_ITEM_TESTING_TAG
    )
  }

  override val supportDrawerMenuItem: DrawerMenuItem? by lazy {
    DrawerMenuItem(
      title = getString(string.menu_support_kiwix),
      iconRes = drawable.ic_support_24px,
      visible = true,
      onClick = { openSupportKiwixExternalLink() },
      testingTag = LEFT_DRAWER_SUPPORT_ITEM_TESTING_TAG
    )
  }

  /**
   * In kiwix app we are not showing the "About app" item so returning null.
   */
  override val aboutAppDrawerMenuItem: DrawerMenuItem? = null

  private fun openZimHostFragment() {
    disableLeftDrawer()
    handleDrawerOnNavigation()
    navigate(KiwixDestination.ZimHost.route)
  }

  override fun getIconResId() = mipmap.ic_launcher

  override fun createApplicationShortcuts() {
    // Remove previously added dynamic shortcuts for old ids if any found.
    removeOutdatedIdShortcuts()
    ShortcutManagerCompat.addDynamicShortcuts(this, dynamicShortcutList())
  }

  override fun openSearch(searchString: String, isOpenedFromTabView: Boolean, isVoice: Boolean) {
    // Freshly open the search fragment.
    navigate(
      KiwixDestination.Search.createRoute(
        searchString = searchString,
        isOpenedFromTabView = isOpenedFromTabView,
        isVoice = isVoice
      ),
      NavOptions.Builder().setPopUpTo(searchFragmentRoute, inclusive = true).build()
    )
  }

  override fun hideBottomAppBar() {
    shouldShowBottomAppBar.update { false }
  }

  override fun showBottomAppBar() {
    shouldShowBottomAppBar.update { true }
  }

  // Outdated shortcut ids(new_tab, get_content)
// Remove if the application has the outdated shortcuts.
  private fun removeOutdatedIdShortcuts() {
    ShortcutManagerCompat.getDynamicShortcuts(this).forEach {
      if (it.id == "new_tab" || it.id == "get_content") {
        ShortcutManagerCompat.removeDynamicShortcuts(this, arrayListOf(it.id))
      }
    }
  }

  private fun dynamicShortcutList(): List<ShortcutInfoCompat> {
    // Create a shortcut for opening the "New tab"
    val newTabShortcut =
      ShortcutInfoCompat.Builder(this, NEW_TAB_SHORTCUT_ID)
        .setShortLabel(getString(string.new_tab_shortcut_label))
        .setLongLabel(getString(string.new_tab_shortcut_label))
        .setIcon(IconCompat.createWithResource(this, drawable.ic_shortcut_new_tab))
        .setDisabledMessage(getString(string.shortcut_disabled_message))
        .setIntent(
          Intent(this, KiwixMainActivity::class.java).apply {
            action = ACTION_NEW_TAB
          }
        )
        .build()

    // create a shortCut for opening the online fragment.
    val getContentShortcut =
      ShortcutInfoCompat.Builder(this, GET_CONTENT_SHORTCUT_ID)
        .setShortLabel(getString(string.get_content_shortcut_label))
        .setLongLabel(getString(string.get_content_shortcut_label))
        .setIcon(IconCompat.createWithResource(this, drawable.ic_shortcut_get_content))
        .setDisabledMessage(getString(string.shortcut_disabled_message))
        .setIntent(
          Intent(this, KiwixMainActivity::class.java).apply {
            action = ACTION_GET_CONTENT
          }
        )
        .build()

    return listOf(newTabShortcut, getContentShortcut)
  }
}
