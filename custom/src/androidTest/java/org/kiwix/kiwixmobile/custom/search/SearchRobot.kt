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
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.hamcrest.CoreMatchers.containsString
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.LEFT_DRAWER_NOTES_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.READER_BOTTOM_BAR_HOME_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.search.SEARCH_FIELD_TESTING_TAG
import org.kiwix.kiwixmobile.core.search.SEARCH_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.custom.testutils.TestUtils
import org.kiwix.kiwixmobile.custom.testutils.TestUtils.TEST_PAUSE_MS
import org.kiwix.kiwixmobile.custom.testutils.TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST
import org.kiwix.kiwixmobile.custom.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.custom.testutils.TestUtils.waitUntilTimeout

fun search(searchRobot: SearchRobot.() -> Unit) = SearchRobot().searchRobot()

class SearchRobot {
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

  private fun openSearchScreen(composeTestRule: ComposeContentTestRule) {
    testFlakyView(
      {
        composeTestRule.onNodeWithTag(SEARCH_ICON_TESTING_TAG).performClick()
      }
    )
  }

  fun searchAndClickOnArticle(searchString: String, composeTestRule: ComposeContentTestRule) {
    // wait a bit to properly load the ZIM file in the reader
    composeTestRule.waitUntilTimeout()
    openSearchScreen(composeTestRule)
    // Wait a bit to properly visible the search screen.
    composeTestRule.waitUntilTimeout()
    searchWithFrequentlyTypedWords(searchString, composeTestRule = composeTestRule)
    clickOnSearchItemInSearchList(composeTestRule)
  }

  private fun clickOnSearchItemInSearchList(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntilTimeout()
      onAllNodesWithTag(SEARCH_ITEM_TESTING_TAG)[0].performClick()
    }
  }

  fun assertArticleLoaded() {
    testFlakyView({
      Web.onWebView()
        .withElement(
          DriverAtoms.findElement(
            Locator.XPATH,
            "//*[contains(text(), 'Forum Category')]"
          )
        )
    })
  }

  fun clickOnHomeButton(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      waitUntil(TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong()) {
        onNodeWithTag(READER_BOTTOM_BAR_HOME_BUTTON_TESTING_TAG).isDisplayed()
      }
      onNodeWithTag(READER_BOTTOM_BAR_HOME_BUTTON_TESTING_TAG)
        .performClick()
    }
  }

  fun clickOnAFoolForYouArticle() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    testFlakyView({
      onWebView()
        .withElement(
          findElement(
            Locator.XPATH,
            "//*[contains(text(), 'A Fool for You')]"
          )
        ).perform(webClick())
    })
  }

  fun assertAFoolForYouArticleLoaded() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    testFlakyView({
      onWebView()
        .withElement(
          findElement(
            Locator.XPATH,
            "//*[contains(text(), '\"A Fool for You\"')]"
          )
        ).check(webMatches(getText(), containsString("\"A Fool for You\"")))
    })
  }

  fun openNoteFragment(
    coreMainActivity: CoreMainActivity,
    composeTestRule: ComposeContentTestRule
  ) {
    coreMainActivity.openNavigationDrawer()
    testFlakyView({
      composeTestRule.apply {
        waitUntilTimeout()
        onNodeWithTag(LEFT_DRAWER_NOTES_ITEM_TESTING_TAG).performClick()
      }
    })
  }
}
