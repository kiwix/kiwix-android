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

package org.kiwix.kiwixmobile.help;

import android.Manifest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.main.KiwixMainActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class HelpFragmentTest {

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
    openDrawer();
    clickOn(R.string.menu_help);
  }

  @Test
  public void verifyHelpActivity() {
    HelpRobot helpRobot = new HelpRobot();
    helpRobot.clickOnWhatDoesKiwixDo();
    helpRobot.assertWhatDoesKiwixDoIsExpanded();
    helpRobot.clickOnWhatDoesKiwixDo();
    helpRobot.clickOnWhereIsContent();
    helpRobot.assertWhereIsContentIsExpanded();
    helpRobot.clickOnWhereIsContent();
    helpRobot.clickOnSendFeedback();
  }

  private void openDrawer() {
    onView(withContentDescription(R.string.open_drawer)).perform(click());
  }
}
