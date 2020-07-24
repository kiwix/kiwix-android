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
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_new_navigation.bottom_nav_view
import kotlinx.android.synthetic.main.activity_new_navigation.container
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.BaseFragmentActivityExtensions
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.kiwixActivityComponent

class KiwixNewNavigationActivity : CoreMainActivity() {
  private lateinit var navController: NavController
  private lateinit var appBarConfiguration: AppBarConfiguration
  private lateinit var drawerToggle: ActionBarDrawerToggle
  private var actionMode: ActionMode? = null

  override fun injection(coreComponent: CoreComponent) {
    kiwixActivityComponent.inject(this)
  }

  private val finishActionModeOnDestinationChange =
    NavController.OnDestinationChangedListener { controller, destination, arguments ->
      actionMode?.finish()
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_new_navigation)

    navController = findNavController(R.id.nav_host_fragment)
    navController.addOnDestinationChangedListener(finishActionModeOnDestinationChange)
    appBarConfiguration = AppBarConfiguration(
      setOf(
        R.id.navigation_downloads,
        R.id.navigation_library,
        R.id.navigation_reader
      ), container
    )
    findViewById<NavigationView>(R.id.drawer_nav_view).setupWithNavController(navController)
    bottom_nav_view.setupWithNavController(navController)
  }

  override fun onSupportActionModeStarted(mode: ActionMode) {
    super.onSupportActionModeStarted(mode)
    actionMode = mode
  }

  fun setupDrawerToggle(toolbar: Toolbar) {
    drawerToggle =
      ActionBarDrawerToggle(
        this, container, toolbar, R.string.open, R.string.close_all_tabs
      )
    container.addDrawerListener(drawerToggle)
    drawerToggle.isDrawerIndicatorEnabled = true
    drawerToggle.syncState()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (drawerToggle.onOptionsItemSelected(item)) {
      return true
    }
    return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
  }

  override fun onSupportNavigateUp(): Boolean {
    val navController = findNavController(R.id.nav_host_fragment)
    return navController.navigateUp() ||
      super.onSupportNavigateUp()
  }

  override fun onBackPressed() {
    supportFragmentManager.fragments.filterIsInstance<BaseFragmentActivityExtensions>().forEach {
      if (it.onBackPressed(this) == BaseFragmentActivityExtensions.Super.ShouldCall) {
        super.onBackPressed()
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    supportFragmentManager.fragments.filterIsInstance<BaseFragmentActivityExtensions>().forEach {
      it.onNewIntent(intent, this)
    }
  }
}
