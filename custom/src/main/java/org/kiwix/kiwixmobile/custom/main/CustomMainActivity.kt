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

package org.kiwix.kiwixmobile.custom.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.navigation.NavOptions
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R.drawable
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.setNavigationResultOnCurrent
import org.kiwix.kiwixmobile.core.extensions.browserIntent
import org.kiwix.kiwixmobile.core.main.ACTION_NEW_TAB
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.DrawerMenuItem
import org.kiwix.kiwixmobile.core.main.LEFT_DRAWER_ABOUT_APP_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.LEFT_DRAWER_HELP_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.LEFT_DRAWER_SUPPORT_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.NEW_TAB_SHORTCUT_ID
import org.kiwix.kiwixmobile.core.main.PAGE_URL_KEY
import org.kiwix.kiwixmobile.core.main.SHOULD_OPEN_IN_NEW_TAB
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.custom.BuildConfig
import org.kiwix.kiwixmobile.custom.CustomApp
import org.kiwix.kiwixmobile.custom.R
import org.kiwix.kiwixmobile.custom.customActivityComponent

class CustomMainActivity : CoreMainActivity() {
  override val mainActivity: AppCompatActivity by lazy { this }
  override val appName: String by lazy { getString(R.string.app_name) }

  override val searchFragmentRoute: String = CustomDestination.Search.route
  override val bookmarksFragmentRoute: String = CustomDestination.Bookmarks.route
  override val settingsFragmentRoute: String = CustomDestination.Settings.route
  override val readerFragmentRoute: String = CustomDestination.Reader.route
  override val historyFragmentRoute: String = CustomDestination.History.route
  override val notesFragmentRoute: String = CustomDestination.Notes.route
  override val helpFragmentRoute: String = CustomDestination.Help.route
  override val cachedComponent by lazy { customActivityComponent }
  override val topLevelDestinationsRoute = setOf(CustomDestination.Reader.route)

  @Suppress("InjectDispatcher")
  override fun onCreate(savedInstanceState: Bundle?) {
    customActivityComponent.inject(this)
    super.onCreate(savedInstanceState)
    setContent {
      navController = rememberNavController()
      leftDrawerState = rememberDrawerState(DrawerValue.Closed)
      uiCoroutineScope = rememberCoroutineScope()
      RestoreDrawerStateOnOrientationChange()
      PersistDrawerStateOnChange()
      CustomMainActivityScreen(
        navController = navController,
        leftDrawerContent = leftDrawerMenu,
        topLevelDestinationsRoute = topLevelDestinationsRoute,
        leftDrawerState = leftDrawerState,
        enableLeftDrawer = enableLeftDrawer.value,
        uiCoroutineScope = uiCoroutineScope,
        customBackHandler = customBackHandler
      )
      DialogHost(alertDialogShower)
      LaunchedEffect(Unit) {
        // Load the menu when UI is attached to screen.
        leftDrawerMenu.addAll(leftNavigationDrawerMenuItems)
      }
    }
    // run the migration on background thread to avoid any UI related issues.
    CoroutineScope(Dispatchers.IO).launch {
      if (!sharedPreferenceUtil.prefIsTest) {
        (applicationContext as CustomApp).customComponent
          .provideObjectBoxDataMigrationHandler()
          .migrate()
      }
    }
  }

  override fun getIconResId() = R.mipmap.ic_launcher

  /**
   * Hide the 'ZimHostFragment' option from the navigation menu
   * because we are now using fd (FileDescriptor)
   * to read the zim file from the asset folder. Currently,
   * 'KiwixServer' is unable to host zim files via fd.
   * This feature is temporarily removed for custom apps.
   * We will re-enable it for custom apps once the issue is resolved.
   * For more info see https://github.com/kiwix/kiwix-android/pull/3516,
   * https://github.com/kiwix/kiwix-android/issues/4026
   */
  override val zimHostDrawerMenuItem: DrawerMenuItem? = null

  /**
   * If custom app is configured to show the "Help menu" in navigation
   * then show it in navigation.
   */
  override val helpDrawerMenuItem: DrawerMenuItem? by lazy {
    if (BuildConfig.DISABLE_HELP_MENU) {
      null
    } else {
      DrawerMenuItem(
        title = getString(string.menu_help),
        iconRes = drawable.ic_help_24px,
        visible = true,
        onClick = { openHelpFragment() },
        testingTag = LEFT_DRAWER_HELP_ITEM_TESTING_TAG
      )
    }
  }

  override val supportDrawerMenuItem: DrawerMenuItem? by lazy {
    /**
     * If custom app is configured to show the "Support app_name" in navigation
     * then show it in navigation. "app_name" will be replaced with custom app name.
     */
    if (BuildConfig.SUPPORT_URL.isNotEmpty()) {
      DrawerMenuItem(
        title = getString(
          string.menu_support_kiwix_for_custom_apps,
          getString(R.string.app_name)
        ),
        iconRes = drawable.ic_support_24px,
        true,
        onClick = {
          closeNavigationDrawer()
          externalLinkOpener.openExternalUrl(BuildConfig.SUPPORT_URL.toUri().browserIntent(), false)
        },
        testingTag = LEFT_DRAWER_SUPPORT_ITEM_TESTING_TAG
      )
    } else {
      /**
       * If custom app is not configured to show the "Support app_name" in navigation
       * then remove it from navigation.
       */
      null
    }
  }

  /**
   * If custom app is configured to show the "About app_name app" in navigation
   * then show it in navigation. "app_name" will be replaced with custom app name.
   */
  override val aboutAppDrawerMenuItem: DrawerMenuItem? by lazy {
    if (BuildConfig.ABOUT_APP_URL.isNotEmpty()) {
      DrawerMenuItem(
        title = getString(
          string.menu_about_app,
          getString(R.string.app_name)
        ),
        iconRes = drawable.ic_baseline_info,
        true,
        onClick = {
          closeNavigationDrawer()
          externalLinkOpener.openExternalUrl(
            BuildConfig.ABOUT_APP_URL.toUri().browserIntent(),
            false
          )
        },
        testingTag = LEFT_DRAWER_ABOUT_APP_ITEM_TESTING_TAG
      )
    } else {
      null
    }
  }

  override fun createApplicationShortcuts() {
    // Remove previously added dynamic shortcuts for old ids if any found.
    removeOutdatedIdShortcuts()
    // Create a shortcut for opening the "New tab"
    val newTabShortcut = ShortcutInfoCompat.Builder(this, NEW_TAB_SHORTCUT_ID)
      .setShortLabel(getString(string.new_tab_shortcut_label))
      .setLongLabel(getString(string.new_tab_shortcut_label))
      .setIcon(IconCompat.createWithResource(this, drawable.ic_shortcut_new_tab))
      .setDisabledMessage(getString(string.shortcut_disabled_message))
      .setIntent(
        Intent(this, CustomMainActivity::class.java).apply {
          action = ACTION_NEW_TAB
        }
      )
      .build()
    ShortcutManagerCompat.addDynamicShortcuts(this, listOf(newTabShortcut))
  }

  override fun openSearch(searchString: String, isOpenedFromTabView: Boolean, isVoice: Boolean) {
    navigate(
      CustomDestination.Search.createRoute(
        searchString = searchString,
        isOpenedFromTabView = isOpenedFromTabView,
        isVoice = isVoice
      ),
      NavOptions.Builder().setPopUpTo(searchFragmentRoute, inclusive = true).build()
    )
  }

  override fun openPage(
    pageUrl: String,
    zimReaderSource: ZimReaderSource?,
    shouldOpenInNewTab: Boolean
  ) {
    var zimFileUri = ""
    if (zimReaderSource != null) {
      zimFileUri = zimReaderSource.toDatabase()
    }
    val navOptions = NavOptions.Builder()
      .setLaunchSingleTop(true)
      .setPopUpTo(readerFragmentRoute, inclusive = true)
      .build()
    // Navigate to reader screen.
    navigate(CustomDestination.Reader.route, navOptions)
    // Set arguments on current destination(reader).
    setNavigationResultOnCurrent(zimFileUri, ZIM_FILE_URI_KEY)
    setNavigationResultOnCurrent(pageUrl, PAGE_URL_KEY)
    setNavigationResultOnCurrent(shouldOpenInNewTab, SHOULD_OPEN_IN_NEW_TAB)
  }

  override fun hideBottomAppBar() {
    // Do nothing since custom apps does not have the bottomAppBar.
  }

  override fun showBottomAppBar() {
    // Do nothing since custom apps does not have the bottomAppBar.
  }

  // Outdated shortcut id(new_tab)
  // Remove if the application has the outdated shortcut.
  private fun removeOutdatedIdShortcuts() {
    ShortcutManagerCompat.getDynamicShortcuts(this).forEach {
      if (it.id == "new_tab") {
        ShortcutManagerCompat.removeDynamicShortcuts(this, arrayListOf(it.id))
      }
    }
  }
}
