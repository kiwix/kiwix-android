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
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_custom_main.custom_drawer_container
import kotlinx.android.synthetic.main.activity_custom_main.drawer_nav_view
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.custom.R
import org.kiwix.kiwixmobile.custom.customActivityComponent

const val REQUEST_READ_FOR_OBB = 5002

class CustomMainActivity : CoreMainActivity() {

  override val navController: NavController by lazy {
    findNavController(R.id.custom_nav_controller)
  }
  override val drawerContainerLayout: DrawerLayout by lazy { custom_drawer_container }
  override val drawerNavView: NavigationView by lazy { drawer_nav_view }
  override val searchFragmentResId: Int = R.id.searchFragment
  override val bookmarksFragmentResId: Int = R.id.bookmarksFragment
  override val settingsFragmentResId: Int = R.id.customSettingsFragment
  override val readerFragmentResId: Int = R.id.customReaderFragment
  override val historyFragmentResId: Int = R.id.historyFragment
  override val helpFragmentResId: Int = R.id.helpFragment
  override val cachedComponent by lazy { customActivityComponent }
  override val topLevelDestinations =
    setOf(R.id.customReaderFragment)

  override fun injection(coreComponent: CoreComponent) {
    customActivityComponent.inject(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_custom_main)
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
    drawer_nav_view.setNavigationItemSelectedListener { item ->
      closeNavigationDrawer()
      onNavigationItemSelected(item)
    }
    drawer_nav_view.menu.findItem(R.id.menu_host_books)
      .isVisible = false
  }

  override fun getIconResId() = R.mipmap.ic_launcher
}
