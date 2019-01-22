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

package org.kiwix.kiwixmobile.intro;

import android.Manifest;

import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;
import com.schibsted.spain.barista.rule.BaristaRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.main.MainActivity;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.intent.Intents;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class IntroActivityTest {

  @Rule
  public BaristaRule<IntroActivity> activityTestRule = BaristaRule.create(IntroActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @Before
  public void setUp() {
    Intents.init();
    activityTestRule.launchActivity();
  }

  @Test
  public void testIntroActivity() {
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    ViewInteraction viewPager = onView(withId(R.id.view_pager));

    // Verify that the sliding view is working properly
    viewPager.perform(swipeLeft());
    onView(allOf(withId(R.id.heading), withText(R.string.save_books_offline), isDisplayed()))
        .check(matches(notNullValue()));
    onView(allOf(withId(R.id.subheading), withText(R.string.download_books_message), isDisplayed()))
        .check(matches(notNullValue()));

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    viewPager.perform(swipeRight());
    onView(allOf(withId(R.id.heading), withText(R.string.welcome_to_the_family), isDisplayed()))
        .check(matches(notNullValue()));
    onView(allOf(withId(R.id.subheading), withText(R.string.human_kind_knowledge), isDisplayed()))
        .check(matches(notNullValue()));

    // Verify that the button is there
    onView(withId(R.id.get_started)).check(matches(notNullValue()));
    onView(withId(R.id.get_started)).check(matches(withText(R.string.get_started)));
    onView(withId(R.id.get_started)).perform(click());

    // Test the intent generated for MainActivity
    intended(hasComponent(MainActivity.class.getName()));
  }

  @After
  public void endTest() {
    Intents.release();
  }
}
