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

import com.schibsted.spain.barista.interaction.BaristaMenuClickInteractions;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.main.MainActivity;
import org.kiwix.kiwixmobile.testutils.TestUtils;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed;
import static com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn;
import static org.kiwix.kiwixmobile.testutils.TestUtils.getResourceString;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ContentTest {

  @Rule
  public ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class);

  @Test
  public void contentTest() {
    BaristaMenuClickInteractions.clickMenu(getResourceString(R.string.menu_zim_manager));

    TestUtils.allowPermissionsIfNeeded();

    assertDisplayed(R.id.action_search);

    clickOn(R.string.local_zims);

    assertDisplayed(R.string.zim_manager);
  }
}
