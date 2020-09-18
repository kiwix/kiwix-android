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
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.core.os.bundleOf
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
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.kiwixActivityComponent
import org.kiwix.kiwixmobile.localFileTransfer.URIS_KEY

const val NAVIGATE_TO_ZIM_HOST_FRAGMENT = "navigate_to_zim_host_fragment"

class KiwixMainActivity : CoreMainActivity() {
  private var actionMode: ActionMode? = null

  override val cachedComponent by lazy { kiwixActivityComponent }
  override val searchFragmentResId: Int = R.id.searchFragment
  override val navController by lazy { findNavController(R.id.nav_host_fragment) }
  override val drawerContainerLayout: DrawerLayout by lazy { navigation_container }
  override val drawerNavView: NavigationView by lazy { drawer_nav_view }
  override val bookmarksFragmentResId: Int = R.id.bookmarksFragment
  override val settingsFragmentResId: Int = R.id.kiwixSettingsFragment
  override val historyFragmentResId: Int = R.id.historyFragment
  override val readerFragmentResId: Int = R.id.readerFragment
  override val helpFragmentResId: Int = R.id.helpFragment
  override val topLevelDestinations =
    setOf(R.id.downloadsFragment, R.id.libraryFragment, R.id.readerFragment)

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

    navController.addOnDestinationChangedListener(finishActionModeOnDestinationChange)
    drawer_nav_view.setupWithNavController(navController)
    drawer_nav_view.setNavigationItemSelectedListener { item ->
      closeNavigationDrawer()
      onNavigationItemSelected(item)
    }
    bottom_nav_view.setupWithNavController(navController)
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
    if (sharedPreferenceUtil.showIntro()) {
      navigate(R.id.introFragment)
    }
  }

  override fun onSupportActionModeStarted(mode: ActionMode) {
    super.onSupportActionModeStarted(mode)
    actionMode = mode
  }

  override fun onBackPressed() {
    if (readerDrawerIsOpen()) {
      closeReaderDrawer()
      return
    }
    super.onBackPressed()
  }

  private fun closeReaderDrawer() {
    drawerContainerLayout.closeDrawer(reader_drawer_nav_view)
  }

  private fun readerDrawerIsOpen() =
    drawerContainerLayout.isDrawerOpen(reader_drawer_nav_view)

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    when (intent.action) {
      Intent.ACTION_SEND_MULTIPLE -> {
        val uris: ArrayList<Uri>? = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        navigate(R.id.localFileTransferFragment, bundleOf(URIS_KEY to uris?.toTypedArray()))
      }
    }
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
