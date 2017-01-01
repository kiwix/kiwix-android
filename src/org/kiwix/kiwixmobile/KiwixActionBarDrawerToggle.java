package org.kiwix.kiwixmobile;

import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;

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
