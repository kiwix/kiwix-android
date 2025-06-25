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
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.uiautomator.UiDevice
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.search.SEARCH_FIELD_TESTING_TAG
import org.kiwix.kiwixmobile.core.search.SEARCH_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.NAVIGATION_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout

fun search(func: SearchRobot.() -> Unit) = SearchRobot().applyWithViewHierarchyPrinting(func)

class SearchRobot : BaseRobot() {
  val searchUnitTestingQuery = "Unit testi"
  val searchUnitTestResult = "Unit testing - Wikipedia"
  val searchQueryForDownloadedZimFile = "A Fool"
  val searchResultForDownloadedZimFile = "A Fool for You"

  fun clickOnSearchItemInSearchList(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntilTimeout()
      onAllNodesWithTag(SEARCH_ITEM_TESTING_TAG)[0].performClick()
    }
  }

  fun checkZimFileSearchSuccessful(readerFragment: Int) {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
    isVisible(ViewId(readerFragment))
  }

  fun searchWithFrequentlyTypedWords(
    query: String,
    wait: Long = 0L,
    composeTestRule: ComposeContentTestRule
  ) {
    testFlakyView({
      composeTestRule.apply {
        waitUntilTimeout()
        val searchView = onNodeWithTag(SEARCH_FIELD_TESTING_TAG)
        searchView.performTextInput("")
        for (char in query) {
          searchView.performTextInput(char.toString())
          if (wait != 0L) {
            waitUntilTimeout(wait)
          }
        }
      }
    })
  }

  fun assertSearchSuccessful(searchResult: String, composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntil(
        timeoutMillis = TEST_PAUSE_MS.toLong(),
        condition = {
          onAllNodesWithTag(SEARCH_ITEM_TESTING_TAG)
            .fetchSemanticsNodes().isNotEmpty()
        }
      )
      onAllNodesWithTag(SEARCH_ITEM_TESTING_TAG)[0]
        .assert(hasText(searchResult))
    }
  }

  fun deleteSearchedQueryFrequently(
    textToDelete: String,
    uiDevice: UiDevice,
    wait: Long = 0L,
    composeTestRule: ComposeContentTestRule
  ) {
    repeat(textToDelete.length) {
      uiDevice.pressKeyCode(KeyEvent.KEYCODE_DEL)
      if (wait != 0L) {
        BaristaSleepInteractions.sleep(wait)
      }
    }

    // clear search query if any remains due to any condition not to affect any other test scenario
    composeTestRule.onNodeWithTag(SEARCH_FIELD_TESTING_TAG).performTextClearance()
  }

  fun clickOnNavigationIcon(composeTestRule: ComposeContentTestRule) {
    composeTestRule.onNodeWithTag(NAVIGATION_ICON_TESTING_TAG).performClick()
  }

  private fun openSearchScreen(composeTestRule: ComposeContentTestRule) {
    testFlakyView(
      {
        composeTestRule.onNodeWithTag(SEARCH_ICON_TESTING_TAG).performClick()
      }
    )
  }

  fun searchAndClickOnArticle(searchString: String, composeTestRule: ComposeContentTestRule) {
    openSearchScreen(composeTestRule)
    searchWithFrequentlyTypedWords(searchString, composeTestRule = composeTestRule)
    clickOnSearchItemInSearchList(composeTestRule)
    checkZimFileSearchSuccessful(org.kiwix.kiwixmobile.R.id.readerFragment)
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
