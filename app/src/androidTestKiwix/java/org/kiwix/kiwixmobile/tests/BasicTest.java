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
package org.kiwix.kiwixmobile.tests;

import android.Manifest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.Gravity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.main.SplashActivity;

import static org.kiwix.kiwixmobile.utils.StandardActions.enterHelp;
import static com.schibsted.spain.barista.interaction.BaristaDrawerInteractions.*;
import static com.schibsted.spain.barista.assertion.BaristaDrawerAssertions.*;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.*;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class BasicTest {

  @Rule
  public ActivityTestRule<SplashActivity> mActivityTestRule = new ActivityTestRule<>(
      SplashActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @Test
  public void basicTest() {
    enterHelp();

    openDrawer();

    assertDrawerIsOpen();

    assertDisplayed(R.id.titleText);
    assertDisplayed(R.string.menu_help);
    assertDisplayed(R.id.left_drawer_list);
    assertDisplayed(R.id.new_tab_button);

    closeDrawer();

    assertDrawerIsClosed();
  }

  @Test
  public void testRightDrawer() {
    enterHelp();

    openDrawerWithGravity(R.id.drawer_layout, Gravity.RIGHT);
    assertDrawerIsOpenWithGravity(R.id.drawer_layout, Gravity.RIGHT);

    assertDisplayed(R.string.no_section_info);

    closeDrawerWithGravity(R.id.drawer_layout, Gravity.RIGHT);
    assertDrawerIsClosedWithGravity(R.id.drawer_layout, Gravity.RIGHT);
  }
}
