/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.search

import android.view.KeyEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.UiDevice
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.hamcrest.Matchers.allOf
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.testutils.TestUtils

fun search(func: SearchRobot.() -> Unit) = SearchRobot().applyWithViewHierarchyPrinting(func)

class SearchRobot : BaseRobot() {

  val searchUnitTestingQuery = "Unit testi"
  val searchUnitTestResult = "Unit testing - Wikipedia"
  val searchQueryForDownloadedZimFile = "A Fool"
  val searchResultForDownloadedZimFile = "A Fool for You"

  fun clickOnSearchItemInSearchList() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
    isVisible(ViewId(R.id.search_list))
    onView(withId(R.id.search_list)).perform(
      actionOnItemAtPosition<RecyclerView.ViewHolder>(
        0,
        click()
      )
    )
  }

  fun checkZimFileSearchSuccessful(readerFragment: Int) {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
    isVisible(ViewId(readerFragment))
  }

  fun searchWithFrequentlyTypedWords(query: String, wait: Long = 0L) {
    val searchView = onView(withId(R.id.search_src_text))
    for (char in query) {
      searchView.perform(typeText(char.toString()))
      if (wait != 0L) {
        BaristaSleepInteractions.sleep(wait)
      }
    }
  }

  fun assertSearchSuccessful(searchResult: String) {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
    val recyclerViewId = R.id.search_list

    // Scroll to the first position in the RecyclerView
    onView(withId(recyclerViewId)).perform(scrollToPosition<ViewHolder>(0))

    // Match the view at the first position in the RecyclerView
    onView(withText(searchResult)).check(
      matches(
        allOf(
          isDisplayed(),
          isDescendantOfA(withId(recyclerViewId))
        )
      )
    )
  }

  fun deleteSearchedQueryFrequently(textToDelete: String, uiDevice: UiDevice, wait: Long = 0L) {
    for (i in textToDelete.indices) {
      uiDevice.pressKeyCode(KeyEvent.KEYCODE_DEL)
      if (wait != 0L) {
        BaristaSleepInteractions.sleep(wait)
      }
    }

    // clear search query if any remains due to any condition not to affect any other test scenario
    val searchView = onView(withId(R.id.search_src_text))
    searchView.perform(clearText())
  }
}
