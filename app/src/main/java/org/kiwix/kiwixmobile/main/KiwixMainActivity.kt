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
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.os.ConfigurationCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import eu.mhutti1.utils.storage.StorageDevice
import eu.mhutti1.utils.storage.StorageDeviceUtils
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.BuildConfig
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.drawable
import org.kiwix.kiwixmobile.core.R.mipmap
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DOWNLOAD_NOTIFICATION_TITLE
import org.kiwix.kiwixmobile.core.extensions.applyEdgeToEdgeInsets
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.ACTION_NEW_TAB
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.NEW_TAB_SHORTCUT_ID
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.databinding.ActivityKiwixMainBinding
import org.kiwix.kiwixmobile.kiwixActivityComponent
import org.kiwix.kiwixmobile.nav.destination.reader.KiwixReaderFragmentDirections
import javax.inject.Inject

const val NAVIGATE_TO_ZIM_HOST_FRAGMENT = "navigate_to_zim_host_fragment"
const val ACTION_GET_CONTENT = "GET_CONTENT"
const val OPENING_ZIM_FILE_DELAY = 300L
const val GET_CONTENT_SHORTCUT_ID = "get_content_shortcut"

class KiwixMainActivity : CoreMainActivity() {
  private var actionMode: ActionMode? = null
  override val cachedComponent by lazy { kiwixActivityComponent }
  override val searchFragmentResId: Int = R.id.searchFragment
  override val navController by lazy {
    (
      supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        as NavHostFragment
      ).navController
  }

  override val drawerContainerLayout: DrawerLayout by lazy {
    activityKiwixMainBinding.navigationContainer
  }

  override val drawerNavView: NavigationView by lazy {
    activityKiwixMainBinding.drawerNavView
  }

  override val readerTableOfContentsDrawer: NavigationView by lazy {
    activityKiwixMainBinding.readerDrawerNavView
  }

  override val navHostContainer by lazy {
    activityKiwixMainBinding.navHostFragment
  }

  @Inject lateinit var newBookDao: NewBookDao

  override val mainActivity: AppCompatActivity by lazy { this }
  override val appName: String by lazy { getString(R.string.app_name) }

  override val bookmarksFragmentResId: Int = R.id.bookmarksFragment
  override val settingsFragmentResId: Int = R.id.kiwixSettingsFragment
  override val historyFragmentResId: Int = R.id.historyFragment
  override val notesFragmentResId: Int = R.id.notesFragment
  override val readerFragmentResId: Int = R.id.readerFragment
  override val helpFragmentResId: Int = R.id.helpFragment
  override val zimHostFragmentResId: Int = R.id.zimHostFragment
  override val navGraphId: Int = R.navigation.kiwix_nav_graph
  override val topLevelDestinations =
    setOf(R.id.downloadsFragment, R.id.libraryFragment, R.id.readerFragment)

  private lateinit var activityKiwixMainBinding: ActivityKiwixMainBinding

  private var isIntroScreenVisible: Boolean = false

  private val finishActionModeOnDestinationChange =
    NavController.OnDestinationChangedListener { _, _, _ ->
      actionMode?.finish()
    }
  private val storageDeviceList = arrayListOf<StorageDevice>()

  override fun onCreate(savedInstanceState: Bundle?) {
    cachedComponent.inject(this)
    super.onCreate(savedInstanceState)
    activityKiwixMainBinding = ActivityKiwixMainBinding.inflate(layoutInflater)
    setContentView(activityKiwixMainBinding.root)

    navController.addOnDestinationChangedListener(finishActionModeOnDestinationChange)
    activityKiwixMainBinding.drawerNavView.apply {
      setupWithNavController(navController)
      setNavigationItemSelectedListener { item ->
        closeNavigationDrawer()
        onNavigationItemSelected(item)
      }
    }
    activityKiwixMainBinding.bottomNavView.setupWithNavController(navController)
    lifecycleScope.launch {
      migrateInternalToPublicAppDirectory()
    }
    handleZimFileIntent(intent)
    handleNotificationIntent(intent)
    handleGetContentIntent(intent)
    activityKiwixMainBinding.root.applyEdgeToEdgeInsets()
  }

  private suspend fun migrateInternalToPublicAppDirectory() {
    if (!sharedPreferenceUtil.prefIsAppDirectoryMigrated) {
      val storagePath = getStorageDeviceList()
        .getOrNull(sharedPreferenceUtil.storagePosition)
        ?.name
      storagePath?.let {
        sharedPreferenceUtil.putPrefStorage(sharedPreferenceUtil.getPublicDirectoryPath(it))
        sharedPreferenceUtil.putPrefAppDirectoryMigrated(true)
      }
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

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    if (::activityKiwixMainBinding.isInitialized) {
      activityKiwixMainBinding.bottomNavView.menu.apply {
        findItem(R.id.readerFragment)?.title = resources.getString(string.reader)
        findItem(R.id.libraryFragment)?.title = resources.getString(string.library)
        findItem(R.id.downloadsFragment)?.title = resources.getString(string.download)
      }
      activityKiwixMainBinding.drawerNavView.menu.apply {
        findItem(org.kiwix.kiwixmobile.core.R.id.menu_bookmarks_list)?.title =
          resources.getString(string.bookmarks)
        findItem(org.kiwix.kiwixmobile.core.R.id.menu_history)?.title =
          resources.getString(string.history)
        findItem(org.kiwix.kiwixmobile.core.R.id.menu_notes)?.title =
          resources.getString(string.pref_notes)
        findItem(org.kiwix.kiwixmobile.core.R.id.menu_host_books)?.title =
          resources.getString(string.menu_wifi_hotspot)
        findItem(org.kiwix.kiwixmobile.core.R.id.menu_settings)?.title =
          resources.getString(string.menu_settings)
        findItem(org.kiwix.kiwixmobile.core.R.id.menu_help)?.title =
          resources.getString(string.menu_help)
        findItem(org.kiwix.kiwixmobile.core.R.id.menu_support_kiwix)?.title =
          resources.getString(string.menu_support_kiwix)
      }
    }
  }

  override fun configureActivityBasedOn(destination: NavDestination) {
    super.configureActivityBasedOn(destination)
    activityKiwixMainBinding.bottomNavView.isVisible = destination.id in topLevelDestinations
  }

  override fun onStart() {
    super.onStart()
    navController.addOnDestinationChangedListener { _, destination, _ ->
      activityKiwixMainBinding.bottomNavView.isVisible = destination.id in topLevelDestinations
      if (destination.id !in topLevelDestinations) {
        handleDrawerOnNavigation()
      }
    }
    if (sharedPreferenceUtil.showIntro() && !isIntroScreenNotVisible()) {
      navigate(KiwixReaderFragmentDirections.actionReaderFragmentToIntroFragment())
    }
    if (!sharedPreferenceUtil.prefIsTest) {
      sharedPreferenceUtil.setIsPlayStoreBuildType(BuildConfig.IS_PLAYSTORE)
    }
    setDefaultDeviceLanguage()
  }

  private fun setDefaultDeviceLanguage() {
    if (sharedPreferenceUtil.prefDeviceDefaultLanguage.isEmpty()) {
      ConfigurationCompat.getLocales(
        applicationContext.resources.configuration
      )[0]?.language?.let {
        sharedPreferenceUtil.putPrefDeviceDefaultLanguage(it)
        handleLocaleChange(
          this,
          sharedPreferenceUtil.prefLanguage,
          sharedPreferenceUtil
        )
      }
    }
  }

  private fun isIntroScreenNotVisible(): Boolean =
    isIntroScreenVisible.also { isIntroScreenVisible = true }

  override fun onSupportActionModeStarted(mode: ActionMode) {
    super.onSupportActionModeStarted(mode)
    actionMode = mode
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleNotificationIntent(intent)
    handleZimFileIntent(intent)
    handleGetContentIntent(intent)
    supportFragmentManager.fragments.filterIsInstance<FragmentActivityExtensions>().forEach {
      it.onNewIntent(intent, this)
    }
  }

  private fun handleGetContentIntent(intent: Intent?) {
    if (intent?.action == ACTION_GET_CONTENT) {
      activityKiwixMainBinding.bottomNavView.menu.findItem(R.id.downloadsFragment)?.let {
        NavigationUI.onNavDestinationSelected(it, navController)
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

        else -> toast(R.string.cannot_open_file)
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
    navigate(
      R.id.libraryFragment,
      bundleOf(
        ZIM_FILE_URI_KEY to path,
      )
    )
  }

  private fun handleNotificationIntent(intent: Intent) {
    if (intent.hasExtra(DOWNLOAD_NOTIFICATION_TITLE)) {
      Handler(Looper.getMainLooper()).postDelayed(
        {
          intent.getStringExtra(DOWNLOAD_NOTIFICATION_TITLE)?.let {
            newBookDao.bookMatching(it)?.let { bookOnDiskEntity ->
              openZimFromFilePath(bookOnDiskEntity.zimReaderSource.toDatabase())
            }
          }
        },
        OPENING_ZIM_FILE_DELAY
      )
    }
  }

  override fun getIconResId() = mipmap.ic_launcher

  override fun createApplicationShortcuts() {
    // Remove previously added dynamic shortcuts for old ids if any found.
    removeOutdatedIdShortcuts()
    ShortcutManagerCompat.addDynamicShortcuts(this, dynamicShortcutList())
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
    val newTabShortcut = ShortcutInfoCompat.Builder(this, NEW_TAB_SHORTCUT_ID)
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
    val getContentShortcut = ShortcutInfoCompat.Builder(this, GET_CONTENT_SHORTCUT_ID)
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
