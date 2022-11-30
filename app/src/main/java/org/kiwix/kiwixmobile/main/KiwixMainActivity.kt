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
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_kiwix_main.bottom_nav_view
import kotlinx.android.synthetic.main.activity_kiwix_main.drawer_nav_view
import kotlinx.android.synthetic.main.activity_kiwix_main.navigation_container
import kotlinx.android.synthetic.main.activity_kiwix_main.reader_drawer_nav_view
import org.kiwix.kiwixmobile.BuildConfig
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.kiwixActivityComponent
import org.kiwix.kiwixmobile.nav.destination.reader.KiwixReaderFragmentDirections

const val NAVIGATE_TO_ZIM_HOST_FRAGMENT = "navigate_to_zim_host_fragment"

class KiwixMainActivity : CoreMainActivity() {
  private var actionMode: ActionMode? = null

  override val cachedComponent by lazy { kiwixActivityComponent }
  override val searchFragmentResId: Int = R.id.searchFragment
  override val navController by lazy { findNavController(R.id.nav_host_fragment) }
  override val drawerContainerLayout: DrawerLayout by lazy { navigation_container }
  override val drawerNavView: NavigationView by lazy { drawer_nav_view }
  override val readerTableOfContentsDrawer: NavigationView by lazy { reader_drawer_nav_view }
  override val bookmarksFragmentResId: Int = R.id.bookmarksFragment
  override val settingsFragmentResId: Int = R.id.kiwixSettingsFragment
  override val historyFragmentResId: Int = R.id.historyFragment
  override val notesFragmentResId: Int = R.id.notesFragment
  override val readerFragmentResId: Int = R.id.readerFragment
  override val helpFragmentResId: Int = R.id.helpFragment
  override val topLevelDestinations =
    setOf(R.id.downloadsFragment, R.id.libraryFragment, R.id.readerFragment)

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
    setContentView(R.layout.activity_kiwix_main)
    if (intent.action == "GET_CONTENT") {
      navigate(R.id.downloadsFragment)
    }

    navController.addOnDestinationChangedListener(finishActionModeOnDestinationChange)
    drawer_nav_view.setupWithNavController(navController)
    drawer_nav_view.setNavigationItemSelectedListener { item ->
      closeNavigationDrawer()
      onNavigationItemSelected(item)
    }
    bottom_nav_view.setupWithNavController(navController)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    bottom_nav_view?.menu?.apply {
      findItem(R.id.readerFragment)?.title = resources.getString(R.string.reader)
      findItem(R.id.libraryFragment)?.title = resources.getString(R.string.library)
      findItem(R.id.downloadsFragment)?.title = resources.getString(R.string.download)
    }
    drawer_nav_view?.menu?.apply {
      findItem(R.id.menu_bookmarks_list)?.title = resources.getString(R.string.bookmarks)
      findItem(R.id.menu_history)?.title = resources.getString(R.string.history)
      findItem(R.id.menu_notes)?.title = resources.getString(R.string.pref_notes)
      findItem(R.id.menu_host_books)?.title = resources.getString(R.string.menu_wifi_hotspot)
      findItem(R.id.menu_settings)?.title = resources.getString(R.string.menu_settings)
      findItem(R.id.menu_help)?.title = resources.getString(R.string.menu_help)
      findItem(R.id.menu_support_kiwix)?.title = resources.getString(R.string.menu_support_kiwix)
    }
  }

  override fun configureActivityBasedOn(destination: NavDestination) {
    super.configureActivityBasedOn(destination)
    bottom_nav_view.isVisible = destination.id in topLevelDestinations
  }

  override fun onStart() {
    super.onStart()
    navController.addOnDestinationChangedListener { _, destination, _ ->
      bottom_nav_view.isVisible = destination.id in topLevelDestinations
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

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_host_books -> openZimHostFragment()
      else -> return super.onNavigationItemSelected(item)
    }
    return true
  }

  private fun openZimHostFragment() {
    disableDrawer()
    navigate(R.id.zimHostFragment)
  }

  override fun getIconResId() = R.mipmap.ic_launcher
}
