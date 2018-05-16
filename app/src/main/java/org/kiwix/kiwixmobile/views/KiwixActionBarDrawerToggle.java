/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.views;

import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import org.kiwix.kiwixmobile.main.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;

public class KiwixActionBarDrawerToggle extends ActionBarDrawerToggle {

  private final DrawerLayout drawerLayout;

  public KiwixActionBarDrawerToggle(KiwixMobileActivity activity, DrawerLayout drawerLayout, Toolbar toolbar) {
    super(activity, drawerLayout, toolbar, 0, 0);
    this.drawerLayout = drawerLayout;
  }

  @Override
  public void onDrawerSlide(View drawerView, float slideOffset) {
    // Make sure it was the navigation drawer
    if (drawerView.getId() == R.id.left_drawer) {
      super.onDrawerSlide(drawerView, slideOffset);
    }
  }

  @Override
  public void onDrawerOpened(View drawerView) {
    // Make sure it was the navigation drawer
    if (drawerView.getId() == R.id.left_drawer) {
      super.onDrawerOpened(drawerView);
      drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
    } else {
      drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START);
    }
  }

  @Override
  public void onDrawerClosed(View drawerView) {
    // Make sure it was the navigation drawer
    if (drawerView.getId() == R.id.left_drawer) {
      super.onDrawerClosed(drawerView);
      drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END);
    } else {
      drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START);
    }
  }
}
