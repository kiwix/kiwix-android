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
import android.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.BaseFragmentActivityExtensions
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.main.CoreNewNavigationActivity
import org.kiwix.kiwixmobile.kiwixActivityComponent

class KiwixNewNavigationActivity : CoreNewNavigationActivity() {
  private lateinit var navController: NavController
  private lateinit var appBarConfiguration: AppBarConfiguration
  override fun injection(coreComponent: CoreComponent) {
    kiwixActivityComponent.inject(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_new_navigation)
    val navView: BottomNavigationView = findViewById(R.id.nav_view)

    navController = findNavController(R.id.nav_host_fragment)
    // Passing each menu ID as a set of Ids because each
    // menu should be considered as top level destinations.
    appBarConfiguration = AppBarConfiguration(
      navController.graph
    )
    val appBarConfiguration = AppBarConfiguration(
      setOf(
        R.id.navigation_reader,
        R.id.navigation_library,
        R.id.navigation_downloads
      )
    )
    findViewById<Toolbar>(R.id.toolbar).setupWithNavController(navController, appBarConfiguration)
    navView.setupWithNavController(navController)
  }

  override fun onSupportNavigateUp(): Boolean {
    val navController = findNavController(R.id.nav_host_fragment)
    return navController.navigateUp() ||
      super.onSupportNavigateUp()
  }

  override fun onActionModeStarted(mode: ActionMode) {
    super.onActionModeStarted(mode)
    supportFragmentManager.fragments.filterIsInstance<BaseFragmentActivityExtensions>().forEach {
      it.onActionModeStarted(mode, this)
    }
  }

  override fun onActionModeFinished(mode: ActionMode) {
    super.onActionModeFinished(mode)
    supportFragmentManager.fragments.filterIsInstance<BaseFragmentActivityExtensions>().forEach {
      it.onActionModeFinished(mode, this)
    }
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
