/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.reader

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.core.main.reader.CLOSE_ALL_TABS_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.READER_SCREEN_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.TAB_MENU_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.TAB_TITLE_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout

fun reader(func: ReaderRobot.() -> Unit) = ReaderRobot().applyWithViewHierarchyPrinting(func)

class ReaderRobot : BaseRobot() {
  private var retryCountForClickOnUndoButton = 5

  fun checkZimFileLoadedSuccessful(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntilTimeout()
      onNodeWithTag(READER_SCREEN_TESTING_TAG).assertExists()
    }
  }

  fun clickOnTabIcon(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntilTimeout()
      testFlakyView({
        onNodeWithTag(TAB_MENU_ITEM_TESTING_TAG).performClick()
      })
    }
  }

  fun clickOnClosedAllTabsButton(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntilTimeout()
      testFlakyView({
        onNodeWithTag(CLOSE_ALL_TABS_BUTTON_TESTING_TAG).performClick()
      })
    }
  }

  fun clickOnUndoButton(composeTestRule: ComposeContentTestRule) {
    try {
      composeTestRule.apply {
        onNodeWithText("UNDO", useUnmergedTree = true)
          .performClick()
      }
    } catch (_: AssertionError) {
      if (retryCountForClickOnUndoButton > 0) {
        retryCountForClickOnUndoButton--
        clickOnUndoButton(composeTestRule)
      }
    }
  }

  fun assertTabRestored(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntilTimeout()
      onAllNodesWithTag(TAB_TITLE_TESTING_TAG)[0].assertTextEquals("Test Zim")
    }
  }

  fun clickOnArticle(articleTitle: String) {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    testFlakyView({
      onWebView()
        .withElement(
          findElement(
            Locator.XPATH,
            "//*[contains(text(), '$articleTitle')]"
          )
        )
        .perform(webClick())
    })
  }

  fun assertArticleLoaded(articlePageContent: String) {
    testFlakyView({
      onWebView()
        .withElement(
          findElement(
            Locator.XPATH,
            "//*[contains(text(), '$articlePageContent')]"
          )
        )
    })
  }
}
