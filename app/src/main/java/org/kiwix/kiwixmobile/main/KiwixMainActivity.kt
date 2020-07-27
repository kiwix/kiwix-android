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
import kotlinx.android.synthetic.main.activity_new_navigation.bottom_nav_view
import kotlinx.android.synthetic.main.activity_new_navigation.drawer_nav_view
import kotlinx.android.synthetic.main.activity_new_navigation.new_navigation_container
import kotlinx.android.synthetic.main.activity_new_navigation.reader_drawer_nav_view
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.BaseFragmentActivityExtensions
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.intent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.start
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.REQUEST_PREFERENCES
import org.kiwix.kiwixmobile.kiwixActivityComponent
import org.kiwix.kiwixmobile.settings.KiwixSettingsActivity
import org.kiwix.kiwixmobile.webserver.ZimHostActivity

class KiwixMainActivity : CoreMainActivity() {
  private lateinit var navController: NavController
  private lateinit var appBarConfiguration: AppBarConfiguration

  private var actionMode: ActionMode? = null

  val cachedComponent by lazy { kiwixActivityComponent }

  override fun injection(coreComponent: CoreComponent) {
    cachedComponent.inject(this)
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
      ), new_navigation_container
    )
    drawer_nav_view.setupWithNavController(navController)
    drawer_nav_view.setNavigationItemSelectedListener(this)
    bottom_nav_view.setupWithNavController(navController)
    navigationContainer = new_navigation_container
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (drawerToggle.onOptionsItemSelected(item)) {
      return true
    }
    return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
  }

  override fun onSupportActionModeStarted(mode: ActionMode) {
    super.onSupportActionModeStarted(mode)
    actionMode = mode
  }

  override fun onSupportNavigateUp(): Boolean {
    val navController = findNavController(R.id.nav_host_fragment)
    return navController.navigateUp() ||
      super.onSupportNavigateUp()
  }

  override fun onBackPressed() {
    if (new_navigation_container.isDrawerOpen(drawer_nav_view)) {
      new_navigation_container.closeDrawer(drawer_nav_view)
      return
    } else if (new_navigation_container.isDrawerOpen(reader_drawer_nav_view)) {
      new_navigation_container.closeDrawer(reader_drawer_nav_view)
      return
    }
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

  override fun setupDrawerToggle(toolbar: Toolbar) {
    drawerToggle =
      ActionBarDrawerToggle(
        this, new_navigation_container, toolbar, R.string.open, R.string.close_all_tabs
      )
    new_navigation_container.addDrawerListener(drawerToggle)
    drawerToggle.syncState()
  }

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_host_books -> start<ZimHostActivity>()
      else -> return super.onNavigationItemSelected(item)
    }
    return true
  }

  override fun openSettingsActivity() {
    startActivityForResult(intent<KiwixSettingsActivity>(), REQUEST_PREFERENCES)
  }
}
