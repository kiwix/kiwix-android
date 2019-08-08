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

package org.kiwix.kiwixmobile.language;

import android.Manifest;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;
import com.schibsted.spain.barista.rule.BaristaRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.kiwix.kiwixmobile.testutils.Matcher.childAtPosition;
import static org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS;

@RunWith(AndroidJUnit4.class)
public class LanguageActivityTest {

  @Rule
  public BaristaRule<ZimManageActivity> activityTestRule = BaristaRule.create(ZimManageActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule =
      GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule =
      GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @Before
  public void setUp() {
    Intents.init();
    activityTestRule.launchActivity();
  }

  @Test
  @Ignore("Covert to kotlin/robot")
  public void testLanguageActivity() {
    onView(withText("Online")).perform(click());
    // Open the Language Activity
    onView(withContentDescription("Choose a language")).perform(click());

    // verify that the back, search and save buttons exist
    onView(withContentDescription("Navigate up")).check(matches(notNullValue()));
    onView(withContentDescription("Save languages")).check(matches(notNullValue()));
    onView(withContentDescription("Search")).check(matches(notNullValue()));

    // languages used for testing
    String language1 = "kongo";
    String language2 = "german";

    // References for the checkboxes for the corresponding languages
    ViewInteraction checkBox1, checkBox2;

    // Initialise Test test languages
    // Search for a particular language
    onView(withContentDescription("Search")).perform(click());
    onView(withId(R.id.search_src_text)).perform(replaceText(language1), closeSoftKeyboard());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    // Get a reference to the checkbox associated with the top selected language
    checkBox1 = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.recycler_view),
                    1),
                0),
            isDisplayed()));



    onView(withContentDescription("Save languages")).perform(click());

    // Now repeat the same process for another language
    onView(withContentDescription("Choose a language")).perform(click());
    onView(withContentDescription("Search")).perform(click());
    onView(withId(R.id.search_src_text)).perform(replaceText(language2), closeSoftKeyboard());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    onView(withContentDescription("Clear query")).perform(click());
    // Collapse the search view to go to the full list of languages
    onView(withContentDescription("Collapse")).perform(click());
    onView(withContentDescription("Save languages")).perform(click());

    // Start the Test
    // Verify that the languages are still selected (or unselected), even after collapsing the search list
    onView(withContentDescription("Choose a language")).perform(click());
    onView(withContentDescription("Search")).perform(click());
    onView(withId(R.id.search_src_text)).perform(replaceText(language1), closeSoftKeyboard());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    checkBox1.perform(click());
    onView(withContentDescription("Clear query")).perform(click());
    // Collapse the search view to go to the full list of languages
    onView(withContentDescription("Collapse")).perform(click());

    onView(withContentDescription("Search")).perform(click());
    onView(withId(R.id.search_src_text)).perform(replaceText(language2), closeSoftKeyboard());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    checkBox1.perform(click());
    onView(withContentDescription("Clear query")).perform(click());
    onView(withContentDescription("Collapse")).perform(click());

    onView(withContentDescription("Search")).perform(click());
    onView(withId(R.id.search_src_text)).perform(replaceText(language1), closeSoftKeyboard());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    checkBox1.check(matches(isChecked()));
    onView(withContentDescription("Clear query")).perform(click());
    onView(withContentDescription("Collapse")).perform(click());

    onView(withContentDescription("Search")).perform(click());
    onView(withId(R.id.search_src_text)).perform(replaceText(language2), closeSoftKeyboard());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    checkBox1.check(matches(isChecked()));
    onView(withContentDescription("Clear query")).perform(click());
    onView(withContentDescription("Collapse")).perform(click());

    // Verify that the new state of the languages is not saved in case the "X" button is pressed
    onView(withContentDescription("Navigate up")).perform(click());
    onView(withContentDescription("Choose a language")).perform(click());

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    onView(withContentDescription("Search")).perform(click());
    onView(withId(R.id.search_src_text)).perform(replaceText(language1), closeSoftKeyboard());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    checkBox1.check(matches(not(isChecked())));

    onView(withContentDescription("Clear query")).perform(click());
    onView(withContentDescription("Collapse")).perform(click());
    onView(withContentDescription("Search")).perform(click());
    onView(withId(R.id.search_src_text)).perform(replaceText(language2), closeSoftKeyboard());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    checkBox1.check(matches(not(isChecked())));
    onView(withContentDescription("Clear query")).perform(click());
    onView(withContentDescription("Collapse")).perform(click());
    onView(withContentDescription("Navigate up")).perform(click());

    // Verify that the new state of the languages saved in case the "save" button is pressed
    onView(withContentDescription("Choose a language")).perform(click());

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    onView(withContentDescription("Search")).perform(click());
    onView(withId(R.id.search_src_text)).perform(replaceText(language1), closeSoftKeyboard());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    checkBox1.perform(click());

    onView(withContentDescription("Clear query")).perform(click());
    onView(withContentDescription("Collapse")).perform(click());
    onView(withContentDescription("Search")).perform(click());
    onView(withId(R.id.search_src_text)).perform(replaceText(language2), closeSoftKeyboard());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    checkBox1.perform(click());
    onView(withContentDescription("Clear query")).perform(click());
    onView(withContentDescription("Collapse")).perform(click());
    onView(withContentDescription("Save languages")).perform(click());

    onView(withContentDescription("Choose a language")).perform(click());

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    onView(withContentDescription("Search")).perform(click());
    onView(withId(R.id.search_src_text)).perform(replaceText(language1), closeSoftKeyboard());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    checkBox1.check(matches(isChecked()));

    onView(withContentDescription("Clear query")).perform(click());
    onView(withContentDescription("Collapse")).perform(click());
    onView(withContentDescription("Search")).perform(click());
    onView(withId(R.id.search_src_text)).perform(replaceText(language2), closeSoftKeyboard());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    checkBox1.check(matches(isChecked()));
    onView(withContentDescription("Clear query")).perform(click());
    onView(withContentDescription("Collapse")).perform(click());
    onView(withContentDescription("Navigate up")).perform(click());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
  }

  @After
  public void endTest() {
    //IdlingRegistry.getInstance().unregister(LibraryFragment.IDLING_RESOURCE);
    Intents.release();
  }
}
