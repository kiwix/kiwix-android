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

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.navigation.NavigationView
import org.kiwix.kiwixmobile.core.extensions.browserIntent
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.custom.BuildConfig
import org.kiwix.kiwixmobile.custom.R
import org.kiwix.kiwixmobile.custom.customActivityComponent
import org.kiwix.kiwixmobile.custom.databinding.ActivityCustomMainBinding

class CustomMainActivity : CoreMainActivity() {

  override val navController: NavController by lazy {
    (
      supportFragmentManager.findFragmentById(
        R.id.custom_nav_controller
      ) as NavHostFragment
      )
      .navController
  }
  override val drawerContainerLayout: DrawerLayout by lazy {
    activityCustomMainBinding.customDrawerContainer
  }
  override val drawerNavView: NavigationView by lazy { activityCustomMainBinding.drawerNavView }
  override val readerTableOfContentsDrawer: NavigationView by lazy {
    activityCustomMainBinding.activityMainNavView
  }

  override val navHostContainer by lazy {
    activityCustomMainBinding.customNavController
  }

  override val mainActivity: AppCompatActivity by lazy { this }

  override val searchFragmentResId: Int = R.id.searchFragment
  override val bookmarksFragmentResId: Int = R.id.bookmarksFragment
  override val settingsFragmentResId: Int = R.id.customSettingsFragment
  override val readerFragmentResId: Int = R.id.customReaderFragment
  override val historyFragmentResId: Int = R.id.historyFragment
  override val notesFragmentResId: Int = R.id.notesFragment
  override val helpFragmentResId: Int = R.id.helpFragment
  override val zimHostFragmentResId: Int = R.id.zimHostFragment
  override val navGraphId: Int = R.navigation.custom_nav_graph
  override val cachedComponent by lazy { customActivityComponent }
  override val topLevelDestinations =
    setOf(R.id.customReaderFragment)

  private lateinit var activityCustomMainBinding: ActivityCustomMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    customActivityComponent.inject(this)
    super.onCreate(savedInstanceState)
    activityCustomMainBinding = ActivityCustomMainBinding.inflate(layoutInflater)
    setContentView(activityCustomMainBinding.root)
    if (savedInstanceState != null) {
      return
    }
  }

  override fun onStart() {
    super.onStart()
    navController.addOnDestinationChangedListener { _, destination, _ ->
      if (destination.id !in topLevelDestinations) {
        handleDrawerOnNavigation()
      }
    }
  }

  override fun setupDrawerToggle(toolbar: Toolbar) {
    super.setupDrawerToggle(toolbar)
    activityCustomMainBinding.drawerNavView.apply {
      /**
       * Hide the 'ZimHostFragment' option from the navigation menu
       * because we are now using fd (FileDescriptor)
       * to read the zim file from the asset folder. Currently,
       * 'KiwixServer' is unable to host zim files via fd.
       * This feature is temporarily hidden for custom apps.
       * We will re-enable it for custom apps once the issue is resolved.
       * For more info see https://github.com/kiwix/kiwix-android/pull/3516
       */
      menu.findItem(R.id.menu_host_books)?.isVisible = false
      /**
       * Hide the `HelpFragment` from custom apps.
       * We have not removed the relevant code for `HelpFragment` from custom apps.
       * If, in the future, we need to display this for all/some custom apps,
       * we can either remove the line below or configure it according to the requirements.
       * For more information, see https://github.com/kiwix/kiwix-android/issues/3584
       */
      menu.findItem(R.id.menu_help)?.isVisible = false

      /**
       * If custom app is configured to show the "About app_name app" in navigation
       * then show it navigation. "app_name" will be replaced with custom app name.
       */
      if (BuildConfig.ABOUT_APP_URL.isNotEmpty()) {
        menu.findItem(R.id.menu_about_app)?.apply {
          title = getString(R.string.menu_about_app, getString(R.string.app_name))
          isVisible = true
        }
      }

      /**
       * If custom app is configured to show the "Support app_name" in navigation
       * then show it navigation. "app_name" will be replaced with custom app name.
       */
      if (BuildConfig.SUPPORT_URL.isNotEmpty()) {
        menu.findItem(R.id.menu_support_kiwix)?.apply {
          title =
            getString(R.string.menu_support_kiwix_for_custom_apps, getString(R.string.app_name))
          isVisible = true
        }
      } else {
        /**
         * If custom app is not configured to show the "Support app_name" in navigation
         * then hide it from navigation.
         */
        menu.findItem(R.id.menu_support_kiwix)?.isVisible = false
      }
      setNavigationItemSelectedListener { item ->
        closeNavigationDrawer()
        onNavigationItemSelected(item)
      }
    }
  }

  /**
   * Overrides the method to configure the click behavior of the "About the app"
   * and "Support URL" features. When the "About the app" and "Support URL"
   * are enabled in a custom app, this function handles those clicks.
   */
  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menu_about_app -> {
        openExternalUrl(BuildConfig.ABOUT_APP_URL)
        true
      }

      R.id.menu_support_kiwix -> {
        openExternalUrl(BuildConfig.SUPPORT_URL)
        true
      }

      else -> super.onNavigationItemSelected(item)
    }
  }

  private fun openExternalUrl(url: String) {
    // check if the provided url is not empty.
    if (url.isNotEmpty()) {
      externalLinkOpener.openExternalUrl(url.toUri().browserIntent())
    }
  }

  override fun getIconResId() = R.mipmap.ic_launcher
}
