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
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.intent
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.REQUEST_PREFERENCES
import org.kiwix.kiwixmobile.custom.R
import org.kiwix.kiwixmobile.custom.customActivityComponent
import org.kiwix.kiwixmobile.custom.settings.CustomSettingsActivity

const val REQUEST_READ_FOR_OBB = 5002

class CustomMainActivity : CoreMainActivity() {

  override fun injection(coreComponent: CoreComponent) {
    customActivityComponent.inject(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    if (savedInstanceState != null) {
      return
    }
    supportFragmentManager.beginTransaction()
      .add(R.id.fragment_containter, CustomReaderFragment()).commit()

    navigationContainer = findViewById(R.id.custom_drawer_container)
  }

  override fun setupDrawerToggle(toolbar: Toolbar) {
    drawerToggle =
      ActionBarDrawerToggle(
        this,
        findViewById(R.id.custom_drawer_container),
        toolbar,
        R.string.open,
        R.string.close_all_tabs
      )
    findViewById<DrawerLayout>(R.id.custom_drawer_container).addDrawerListener(drawerToggle)
    drawerToggle.syncState()
    findViewById<NavigationView>(R.id.drawer_nav_view).setNavigationItemSelectedListener(this)
    findViewById<NavigationView>(R.id.drawer_nav_view).menu.findItem(R.id.menu_host_books)
      .isVisible = false
  }

  override fun openSettingsActivity() {
    startActivityForResult(intent<CustomSettingsActivity>(), REQUEST_PREFERENCES)
  }
}
