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

package org.kiwix.kiwixmobile.main;

import android.Manifest;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import com.schibsted.spain.barista.interaction.BaristaMenuClickInteractions;
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.core.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn;
import static org.hamcrest.CoreMatchers.allOf;
import static org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS;
import static org.kiwix.kiwixmobile.testutils.TestUtils.getResourceString;
import static org.kiwix.kiwixmobile.utils.StandardActions.enterSettings;
import static org.kiwix.kiwixmobile.utils.StandardActions.openDrawer;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {
  @Rule
  public ActivityTestRule<KiwixMainActivity> activityTestRule =
    new ActivityTestRule<>(KiwixMainActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule =
    GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @Before
  public void setup() {
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    onView(allOf(withText(R.string.reader),
      withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(click());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
  }

  @Test
  public void navigateHelp() {
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    openDrawer();
    clickOn(R.string.menu_help);
  }

  @Test
  public void navigateSettings() {
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    openDrawer();
    enterSettings();
  }

  @Test
  public void navigateBookmarks() {
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    openDrawer();
    BaristaMenuClickInteractions.clickMenu(getResourceString(R.string.bookmarks));
  }

  @Test
  public void navigateDeviceContent() {
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    clickOn(R.string.library);
  }

  @Test
  public void navigateOnlineContent() {
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    clickOn(R.string.download);
  }
}
