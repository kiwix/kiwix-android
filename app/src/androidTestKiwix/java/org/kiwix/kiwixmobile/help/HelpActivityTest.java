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

package org.kiwix.kiwixmobile.help;

import android.Manifest;
import android.content.Context;
import android.support.test.espresso.intent.Intents;
import android.support.test.filters.LargeTest;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;

import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;
import com.schibsted.spain.barista.rule.BaristaRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class HelpActivityTest {

  @Rule
  public BaristaRule<HelpActivity> activityTestRule = BaristaRule.create(HelpActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);
  private Context context;

  @Before
  public void setUp() {
    Intents.init();
    context = getInstrumentation().getTargetContext();
    activityTestRule.launchActivity();
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
  }

  @Test
  public void testHelpActivity() {
    String test; // To store the temporary value of some strings while processing

    // Test the layout of the screen
    onView(withText(context.getString(R.string.help_5))).check(matches(notNullValue()));
    onView(withText(context.getString(R.string.help_5))).perform(click());
    test = context.getString(R.string.help_6) + "\n" + context.getString(R.string.help_7) + "\n" + context.getString(R.string.help_8) + "\n" + context.getString(R.string.help_9) + "\n" + context.getString(R.string.help_10) + "\n" + context.getString(R.string.help_11) + "\n";
    onView(withText(test)).check(matches(notNullValue()));

    onView(withText(context.getString(R.string.help_12))).check(matches(notNullValue()));
    onView(withText(context.getString(R.string.help_12))).perform(click());
    test = context.getString(R.string.help_13) + "\n" + context.getString(R.string.help_14) + "\n" + context.getString(R.string.help_15) + "\n" + context.getString(R.string.help_16) + "\n" + context.getString(R.string.help_17) + "\n" + context.getString(R.string.help_18) + "\n" + context.getString(R.string.help_19) + "\n";
    onView(withText(test)).check(matches(notNullValue()));

    onView(withText(context.getString(R.string.help_2))).check(matches(notNullValue()));
    onView(withText(context.getString(R.string.help_2))).perform(click());
    test = context.getString(R.string.help_3) + "\n" + context.getString(R.string.help_4) + "\n";
    onView(withText(test)).check(matches(notNullValue()));

    // Test the feedback intent
    onView(withText(context.getString(R.string.send_feedback))).check(matches(notNullValue()));
    onView(withText(context.getString(R.string.send_feedback))).perform(click());
    // TODO: Find a way to test the ACTION_SENDTO intent
  }

  @After
  public void endTest() {
    Intents.release();
  }
}
