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
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;
import com.schibsted.spain.barista.rule.BaristaRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.intro.IntroActivity;

import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static org.kiwix.kiwixmobile.testutils.Matcher.childAtPosition;
import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class HelpActivityTest {

  private Context context;

  @Rule
  public BaristaRule<IntroActivity> activityTestRule = BaristaRule.create(IntroActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @Before
  public void setUp() {
    Intents.init();
    context = getInstrumentation().getTargetContext();
    activityTestRule.launchActivity();
  }

  @Test
  public void testHelpActivity() {
    onView(withId(R.id.get_started)).check(matches(notNullValue()));
    onView(withId(R.id.get_started)).perform(click());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    // Verify that the home screen is open
    onView(childAtPosition(withId(R.id.toolbar), 0)).check(matches(notNullValue()));

    // Open the Help screen
    openActionBarOverflowOrOptionsMenu(context);
    onView(withText("Help")).perform(click());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    // Verify that the Help Activity is opened
    onView(withText("Help")).check(matches(notNullValue()));

    // Verify that the back button is displayed
    onView(withContentDescription("Navigate up")).check(matches(notNullValue()));
    // Verify that going back from the help screen we go to the previous screen
    onView(withContentDescription("Navigate up")).perform(click());

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    // Verify that the home activity is opened
    onView(childAtPosition(withId(R.id.toolbar), 0)).check(matches(notNullValue()));

    // Open a zim file
    onView(allOf(withId(R.id.recycler_view), childAtPosition(withId(R.id.get_content_card), 1))).perform(actionOnItemAtPosition(1, click()));
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    // Open the help activity again
    openActionBarOverflowOrOptionsMenu(context);
    onView(withText("Help")).perform(click());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    onView(withText("Help")).check(matches(notNullValue()));

    // Verify that going back from the help screen we go back to the Zim file
    onView(withContentDescription("Navigate up")).perform(click());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    onView(childAtPosition(withId(R.id.toolbar), 0)).check(matches(notNullValue()));

    // Open the Help screen again
    openActionBarOverflowOrOptionsMenu(context);
    onView(withText("Help")).perform(click());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

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
