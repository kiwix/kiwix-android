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
package org.kiwix.kiwixmobile.language

import android.Manifest
import org.kiwix.kiwixmobile.testutils.Matcher.Companion.childAtPosition
import androidx.test.rule.ActivityTestRule
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import androidx.test.rule.GrantPermissionRule
import org.kiwix.kiwixmobile.R
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.hamcrest.core.IsNull
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.testutils.TestUtils

@RunWith(AndroidJUnit4::class)
class LanguageFragmentTest {
  @Rule
  var activityTestRule = ActivityTestRule(
    KiwixMainActivity::class.java
  )

  @Rule
  var readPermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)

  @Rule
  var writePermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

  @Before fun setUp() {
    Intents.init()
  }

  @Test
  @Ignore("Covert to kotlin/robot")
  fun testLanguageFragment() {
    clickOn(R.string.download)
    // Open the Language Activity
    Espresso.onView(ViewMatchers.withContentDescription("Choose a language")).perform(
      ViewActions.click()
    )

    // verify that the back, search and save buttons exist
    Espresso.onView(ViewMatchers.withContentDescription("Navigate up")).check(
      ViewAssertions.matches(
        IsNull.notNullValue()
      )
    )
    Espresso.onView(ViewMatchers.withContentDescription("Save languages")).check(
      ViewAssertions.matches(
        IsNull.notNullValue()
      )
    )
    Espresso.onView(ViewMatchers.withContentDescription("Search")).check(
      ViewAssertions.matches(
        IsNull.notNullValue()
      )
    )

    // languages used for testing
    val language1 = "kongo"
    val language2 = "german"

    // Initialise Test test languages
    // Search for a particular language
    Espresso.onView(ViewMatchers.withContentDescription("Search")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withId(R.id.search_src_text))
      .perform(ViewActions.replaceText(language1), ViewActions.closeSoftKeyboard())
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())

    // References for the checkboxes for the corresponding languages
    // Get a reference to the checkbox associated with the top selected language
    val checkBox1: ViewInteraction = Espresso.onView(
      Matchers.allOf(
        ViewMatchers.withId(R.id.item_language_checkbox),
        childAtPosition(
          childAtPosition(
            ViewMatchers.withId(R.id.recycler_view),
            1
          ),
          0
        ),
        ViewMatchers.isDisplayed()
      )
    )
    Espresso.onView(ViewMatchers.withContentDescription("Save languages"))
      .perform(ViewActions.click())

    // Now repeat the same process for another language
    Espresso.onView(ViewMatchers.withContentDescription("Choose a language")).perform(
      ViewActions.click()
    )
    Espresso.onView(ViewMatchers.withContentDescription("Search")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withId(R.id.search_src_text))
      .perform(ViewActions.replaceText(language2), ViewActions.closeSoftKeyboard())
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    Espresso.onView(ViewMatchers.withContentDescription("Clear query")).perform(ViewActions.click())
    // Collapse the search view to go to the full list of languages
    Espresso.onView(ViewMatchers.withContentDescription("Collapse")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Save languages"))
      .perform(ViewActions.click())

    // Start the Test
    // Verify that the languages are still selected (or unselected), even after collapsing the search list
    Espresso.onView(ViewMatchers.withContentDescription("Choose a language")).perform(
      ViewActions.click()
    )
    Espresso.onView(ViewMatchers.withContentDescription("Search")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withId(R.id.search_src_text))
      .perform(ViewActions.replaceText(language1), ViewActions.closeSoftKeyboard())
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    checkBox1.perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Clear query")).perform(ViewActions.click())
    // Collapse the search view to go to the full list of languages
    Espresso.onView(ViewMatchers.withContentDescription("Collapse")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Search")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withId(R.id.search_src_text))
      .perform(ViewActions.replaceText(language2), ViewActions.closeSoftKeyboard())
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    checkBox1.perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Clear query")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Collapse")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Search")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withId(R.id.search_src_text))
      .perform(ViewActions.replaceText(language1), ViewActions.closeSoftKeyboard())
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    checkBox1.check(ViewAssertions.matches(ViewMatchers.isChecked()))
    Espresso.onView(ViewMatchers.withContentDescription("Clear query")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Collapse")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Search")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withId(R.id.search_src_text))
      .perform(ViewActions.replaceText(language2), ViewActions.closeSoftKeyboard())
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    checkBox1.check(ViewAssertions.matches(ViewMatchers.isChecked()))
    Espresso.onView(ViewMatchers.withContentDescription("Clear query")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Collapse")).perform(ViewActions.click())

    // Verify that the new state of the languages is not saved in case the "X" button is pressed
    Espresso.onView(ViewMatchers.withContentDescription("Navigate up")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Choose a language")).perform(
      ViewActions.click()
    )
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    Espresso.onView(ViewMatchers.withContentDescription("Search")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withId(R.id.search_src_text))
      .perform(ViewActions.replaceText(language1), ViewActions.closeSoftKeyboard())
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    checkBox1.check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isChecked())))
    Espresso.onView(ViewMatchers.withContentDescription("Clear query")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Collapse")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Search")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withId(R.id.search_src_text))
      .perform(ViewActions.replaceText(language2), ViewActions.closeSoftKeyboard())
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    checkBox1.check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isChecked())))
    Espresso.onView(ViewMatchers.withContentDescription("Clear query")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Collapse")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Navigate up")).perform(ViewActions.click())

    // Verify that the new state of the languages saved in case the "save" button is pressed
    Espresso.onView(ViewMatchers.withContentDescription("Choose a language")).perform(
      ViewActions.click()
    )
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    Espresso.onView(ViewMatchers.withContentDescription("Search")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withId(R.id.search_src_text))
      .perform(ViewActions.replaceText(language1), ViewActions.closeSoftKeyboard())
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    checkBox1.perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Clear query")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Collapse")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Search")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withId(R.id.search_src_text))
      .perform(ViewActions.replaceText(language2), ViewActions.closeSoftKeyboard())
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    checkBox1.perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Clear query")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Collapse")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Save languages"))
      .perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Choose a language")).perform(
      ViewActions.click()
    )
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    Espresso.onView(ViewMatchers.withContentDescription("Search")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withId(R.id.search_src_text))
      .perform(ViewActions.replaceText(language1), ViewActions.closeSoftKeyboard())
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    checkBox1.check(ViewAssertions.matches(ViewMatchers.isChecked()))
    Espresso.onView(ViewMatchers.withContentDescription("Clear query")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Collapse")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Search")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withId(R.id.search_src_text))
      .perform(ViewActions.replaceText(language2), ViewActions.closeSoftKeyboard())
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    checkBox1.check(ViewAssertions.matches(ViewMatchers.isChecked()))
    Espresso.onView(ViewMatchers.withContentDescription("Clear query")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Collapse")).perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withContentDescription("Navigate up")).perform(ViewActions.click())
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
  }

  @After fun endTest() {
    // IdlingRegistry.getInstance().unregister(LibraryFragment.IDLING_RESOURCE);
    Intents.release()
  }
}
