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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.os.ConfigurationCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import org.kiwix.kiwixmobile.BuildConfig
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.databinding.ActivityKiwixMainBinding
import org.kiwix.kiwixmobile.kiwixActivityComponent
import org.kiwix.kiwixmobile.nav.destination.reader.KiwixReaderFragmentDirections

const val NAVIGATE_TO_ZIM_HOST_FRAGMENT = "navigate_to_zim_host_fragment"

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

  override val mainActivity: AppCompatActivity by lazy { this }

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
  override fun injection(coreComponent: CoreComponent) {
    cachedComponent.inject(this)
  }

  private val finishActionModeOnDestinationChange =
    NavController.OnDestinationChangedListener { _, _, _ ->
      actionMode?.finish()
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityKiwixMainBinding = ActivityKiwixMainBinding.inflate(layoutInflater)
    setContentView(activityKiwixMainBinding.root)
    if (intent.action == "GET_CONTENT") {
      navigate(R.id.downloadsFragment)
    }

    navController.addOnDestinationChangedListener(finishActionModeOnDestinationChange)
    activityKiwixMainBinding.drawerNavView.setupWithNavController(navController)
    activityKiwixMainBinding.drawerNavView.setNavigationItemSelectedListener { item ->
      closeNavigationDrawer()
      onNavigationItemSelected(item)
    }
    activityKiwixMainBinding.bottomNavView.setupWithNavController(navController)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    if (::activityKiwixMainBinding.isInitialized) {
      activityKiwixMainBinding.bottomNavView.menu.apply {
        findItem(R.id.readerFragment)?.title = resources.getString(R.string.reader)
        findItem(R.id.libraryFragment)?.title = resources.getString(R.string.library)
        findItem(R.id.downloadsFragment)?.title = resources.getString(R.string.download)
      }
      activityKiwixMainBinding.drawerNavView.menu.apply {
        findItem(R.id.menu_bookmarks_list)?.title = resources.getString(R.string.bookmarks)
        findItem(R.id.menu_history)?.title = resources.getString(R.string.history)
        findItem(R.id.menu_notes)?.title = resources.getString(R.string.pref_notes)
        findItem(R.id.menu_host_books)?.title = resources.getString(R.string.menu_wifi_hotspot)
        findItem(R.id.menu_settings)?.title = resources.getString(R.string.menu_settings)
        findItem(R.id.menu_help)?.title = resources.getString(R.string.menu_help)
        findItem(R.id.menu_support_kiwix)?.title = resources.getString(R.string.menu_support_kiwix)
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
    supportFragmentManager.fragments.filterIsInstance<FragmentActivityExtensions>().forEach {
      it.onNewIntent(intent, this)
    }
  }

  override fun getIconResId() = R.mipmap.ic_launcher
}
