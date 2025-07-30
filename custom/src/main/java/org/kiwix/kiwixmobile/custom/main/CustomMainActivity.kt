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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.navigation.compose.rememberNavController
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R.drawable
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.extensions.browserIntent
import org.kiwix.kiwixmobile.core.main.ACTION_NEW_TAB
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.DrawerMenuItem
import org.kiwix.kiwixmobile.core.main.NEW_TAB_SHORTCUT_ID
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.custom.BuildConfig
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

  override fun onCreate(savedInstanceState: Bundle?) {
    customActivityComponent.inject(this)
    super.onCreate(savedInstanceState)
    setContent {
      navController = rememberNavController()
      leftDrawerState = rememberDrawerState(DrawerValue.Closed)
      uiCoroutineScope = rememberCoroutineScope()
      CustomMainActivityScreen(
        navController = navController,
        leftDrawerContent = leftDrawerMenu,
        topLevelDestinationsRoute = topLevelDestinationsRoute,
        leftDrawerState = leftDrawerState,
        enableLeftDrawer = enableLeftDrawer.value
      )
      DialogHost(alertDialogShower)
    }
    if (savedInstanceState != null) {
      return
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
   * Hide the `HelpFragment` from custom apps.
   * We have not removed the relevant code for `HelpFragment` from custom apps.
   * If, in the future, we need to display this for all/some custom apps,
   * we can either remove the line below or configure it according to the requirements.
   * For more information, see https://github.com/kiwix/kiwix-android/issues/3584
   */
  override val helpDrawerMenuItem: DrawerMenuItem? = null

  override val supportDrawerMenuItem: DrawerMenuItem? =
    /**
     * If custom app is configured to show the "Support app_name" in navigation
     * then show it navigation. "app_name" will be replaced with custom app name.
     */
    if (BuildConfig.SUPPORT_URL.isNotEmpty()) {
      DrawerMenuItem(
        title = CoreApp.instance.getString(
          string.menu_support_kiwix_for_custom_apps,
          CoreApp.instance.getString(R.string.app_name)
        ),
        iconRes = drawable.ic_support_24px,
        true,
        onClick = {
          closeNavigationDrawer()
          externalLinkOpener.openExternalUrl(BuildConfig.SUPPORT_URL.toUri().browserIntent(), false)
        }
      )
    } else {
      /**
       * If custom app is not configured to show the "Support app_name" in navigation
       * then remove it from navigation.
       */
      null
    }

  /**
   * If custom app is configured to show the "About app_name app" in navigation
   * then show it navigation. "app_name" will be replaced with custom app name.
   */
  override val aboutAppDrawerMenuItem: DrawerMenuItem? =
    if (BuildConfig.ABOUT_APP_URL.isNotEmpty()) {
      DrawerMenuItem(
        title = CoreApp.instance.getString(
          string.menu_about_app,
          CoreApp.instance.getString(R.string.app_name)
        ),
        iconRes = drawable.ic_baseline_info,
        true,
        onClick = {
          closeNavigationDrawer()
          externalLinkOpener.openExternalUrl(
            BuildConfig.ABOUT_APP_URL.toUri().browserIntent(),
            false
          )
        }
      )
    } else {
      null
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
      )
    )
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
