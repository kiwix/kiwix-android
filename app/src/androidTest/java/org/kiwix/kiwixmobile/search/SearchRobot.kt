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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.uiautomator.UiDevice
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import com.adevinta.android.barista.internal.matcher.HelperMatchers.atPosition
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView

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
    testFlakyView({
      val searchView = onView(withId(R.id.search_src_text))
      for (char in query) {
        searchView.perform(typeText(char.toString()))
        if (wait != 0L) {
          BaristaSleepInteractions.sleep(wait)
        }
      }
    })
  }

  fun assertSearchSuccessful(searchResult: String) {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
    val recyclerViewId = R.id.search_list

    onView(withId(recyclerViewId)).check(
      matches(
        atPosition(0, hasDescendant(withText(searchResult)))
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

  private fun openSearchScreen() {
    testFlakyView({ onView(withId(R.id.menu_search)).perform(click()) })
  }

  fun searchAndClickOnArticle(searchString: String) {
    // wait a bit to properly load the ZIM file in the reader
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
    openSearchScreen()
    searchWithFrequentlyTypedWords(searchString)
    clickOnSearchItemInSearchList()
  }

  fun assertArticleLoaded() {
    testFlakyView({
      onWebView()
        .withElement(
          findElement(
            Locator.XPATH,
            "//*[contains(text(), 'Big Baby DRAM')]"
          )
        )
    })
  }
}
