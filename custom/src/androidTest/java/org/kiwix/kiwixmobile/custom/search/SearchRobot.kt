/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.custom.search

import android.view.KeyEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.web.sugar.Web
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import com.adevinta.android.barista.internal.matcher.HelperMatchers
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.custom.testutils.TestUtils
import org.kiwix.kiwixmobile.custom.testutils.TestUtils.testFlakyView

fun search(searchRobot: SearchRobot.() -> Unit) = SearchRobot().searchRobot()

class SearchRobot {
  fun searchWithFrequentlyTypedWords(query: String, wait: Long = 0L) {
    testFlakyView({
      val searchView = Espresso.onView(ViewMatchers.withId(R.id.search_src_text))
      searchView.perform(ViewActions.clearText())
      for (char in query) {
        searchView.perform(ViewActions.typeText(char.toString()))
        if (wait != 0L) {
          BaristaSleepInteractions.sleep(wait)
        }
      }
    })
  }

  fun assertSearchSuccessful(searchResult: String) {
    testFlakyView({
      BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
      Espresso.onView(ViewMatchers.withId(R.id.search_list)).check(
        ViewAssertions.matches(
          HelperMatchers.atPosition(
            0,
            ViewMatchers.hasDescendant(ViewMatchers.withText(searchResult))
          )
        )
      )
    })
  }

  fun deleteSearchedQueryFrequently(textToDelete: String, uiDevice: UiDevice, wait: Long = 0L) {
    testFlakyView({
      for (i in textToDelete.indices) {
        uiDevice.pressKeyCode(KeyEvent.KEYCODE_DEL)
        if (wait != 0L) {
          BaristaSleepInteractions.sleep(wait)
        }
      }

      // clear search query if any remains due to any condition not to affect any other test scenario
      val searchView = Espresso.onView(ViewMatchers.withId(R.id.search_src_text))
      searchView.perform(ViewActions.clearText())
    })
  }

  private fun openSearchScreen() {
    testFlakyView({
      Espresso.onView(ViewMatchers.withId(R.id.menu_search))
        .perform(ViewActions.click())
    })
  }

  fun searchAndClickOnArticle(searchString: String) {
    // wait a bit to properly load the ZIM file in the reader
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
    openSearchScreen()
    searchWithFrequentlyTypedWords(searchString)
    clickOnSearchItemInSearchList()
  }

  fun clickOnSearchItemInSearchList() {
    testFlakyView({
      BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
      Espresso.onView(ViewMatchers.withId(R.id.search_list)).perform(
        RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
          0,
          ViewActions.click()
        )
      )
    })
  }

  fun assertArticleLoaded() {
    testFlakyView({
      Web.onWebView()
        .withElement(
          DriverAtoms.findElement(
            Locator.XPATH,
            "//*[contains(text(), 'Big Baby DRAM')]"
          )
        )
    })
  }
}
