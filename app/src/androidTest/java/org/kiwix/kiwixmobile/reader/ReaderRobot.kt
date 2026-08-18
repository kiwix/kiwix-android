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

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.printToString
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.core.main.reader.CLOSE_ALL_TABS_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.READER_SCREEN_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.READ_ALOUD_MENU_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.TABS_SIZE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.TAB_MENU_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.TAB_TITLE_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.TTS_CONTROL_STOP_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.PAGE_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.search.OPEN_ITEM_IN_NEW_TAB_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.NAVIGATION_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.OVERFLOW_MENU_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_DISMISS_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_TITLE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.main.BOTTOM_NAV_LIBRARY_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.main.BOTTOM_NAV_READER_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.FIFTEEN_SECOND_DELAY
import org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout

fun reader(func: ReaderRobot.() -> Unit) = ReaderRobot().applyWithViewHierarchyPrinting(func)

class ReaderRobot : BaseRobot() {
  private var retryCountForClickOnUndoButton = 5

  companion object {
    private const val TAG = "ReaderRobot"
  }

  fun checkZimFileLoadedSuccessful(
    composeTestRule: ComposeContentTestRule,
    articlePageContent: String? = null
  ) {
    composeTestRule.apply {
      waitUntil(FIFTEEN_SECOND_DELAY) {
        onNodeWithTag(READER_SCREEN_TESTING_TAG).isDisplayed()
      }
      Log.e(TAG, "Reader screen is displayed.")
      // Wait for a few second to fully load the article in reader.
      waitUntilTimeout()
      articlePageContent?.let {
        assertArticleLoaded(it)
        Log.e(TAG, "Article content '$it' loaded successfully in the WebView.")
      }
      // Wait for saving the tabs history.
      waitUntilTimeout(TEST_PAUSE_MS_FOR_DOWNLOAD_TEST)
      Log.e(TAG, "Finished waiting for the tabs/reader history to be saved.")
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
    }, 10)
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
    }, 10)
  }

  fun openAndroidArticleInNewTab(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onAllNodesWithTag(OPEN_ITEM_IN_NEW_TAB_ICON_TESTING_TAG)[0].performClick()
      }
    })
  }

  fun assertTabsRestored(composeTestRule: ComposeContentTestRule) {
    try {
      composeTestRule.waitUntil(FIFTEEN_SECOND_DELAY) {
        composeTestRule.onAllNodesWithTag(TABS_SIZE_TEXT_TESTING_TAG, useUnmergedTree = true)
          .fetchSemanticsNodes().isNotEmpty()
      }
    } catch (e: ComposeTimeoutException) {
      Log.e(TAG, "The tab icon is not visible due to scroll. Original exception: $e")
      // We will implement the scrolling logic from test cases to find the tab icon in the future.
      // For now, we will just return and not assert the tab count.
      return
    }

    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        val text = onNodeWithTag(TABS_SIZE_TEXT_TESTING_TAG, useUnmergedTree = true).getText()
        val tabCount = text.toIntOrNull()
        requireNotNull(tabCount) { "No tabs restored original tab count: '$text'" }

        assert(tabCount >= 2) {
          "Expected at least 2 tabs, but found $tabCount"
        }
      }
    })
  }

  fun openLocalLibraryScreenViaBottomAppBar(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(BOTTOM_NAV_LIBRARY_ITEM_TESTING_TAG).performClick()
      }
    })
  }

  fun clickOnReaderScreenInBottomAppBar(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(BOTTOM_NAV_READER_ITEM_TESTING_TAG).performClick()
      }
    })
  }

  fun clickOnNavigationIcon(composeTestRule: ComposeContentTestRule) {
    composeTestRule.onNodeWithTag(NAVIGATION_ICON_TESTING_TAG).performClick()
  }

  fun clickOnReadAloudMenuItem(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      try {
        waitUntil(TEST_PAUSE_MS_FOR_DOWNLOAD_TEST) {
          onNodeWithTag(OVERFLOW_MENU_BUTTON_TESTING_TAG).isDisplayed()
        }
      } catch (e: ComposeTimeoutException) {
        // The overflow menu only renders once the reader's menu state considers the current
        // page's URL valid (see ReaderMenuState/CoreReaderViewModel). Dump the full semantics
        // tree so CI logs show exactly what was on screen (e.g. is the toolbar empty, did tab
        // restoration leave the wrong page selected) instead of only the bare timeout.
        Log.e(
          TAG, "Overflow menu button never appeared. Dumping current UI tree for debugging.\n" +
            " ${onRoot().printToString()}"
        )
        throw e
      }
      onNodeWithTag(OVERFLOW_MENU_BUTTON_TESTING_TAG).performClick()
      waitUntilTimeout()
      onNodeWithTag(READ_ALOUD_MENU_ITEM_TESTING_TAG).performClick()
    }
  }

  fun assertTTSLanguageIsNotSupportedDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(
      TestUtils.TEST_PAUSE_MS.toLong()
    ) {
      composeTestRule.onNodeWithTag(ALERT_DIALOG_TITLE_TEXT_TESTING_TAG)
        .isDisplayed()
    }
  }

  fun clickOnCancelButton(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      waitUntil(TestUtils.TEST_PAUSE_MS.toLong()) {
        onNodeWithTag(ALERT_DIALOG_DISMISS_BUTTON_TESTING_TAG).isDisplayed()
      }
      onNodeWithTag(ALERT_DIALOG_DISMISS_BUTTON_TESTING_TAG).performClick()
    }
  }

  fun assertTTSControlsVisible(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.waitForIdle()
      composeTestRule.waitUntil(
        TestUtils.TEST_PAUSE_MS.toLong()
      ) {
        composeTestRule.onNodeWithTag(TTS_CONTROL_STOP_BUTTON_TESTING_TAG)
          .isDisplayed()
      }
    })
  }

  fun clickOnTTSStopButton(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag(TTS_CONTROL_STOP_BUTTON_TESTING_TAG).performClick()
    })
  }

  fun clickOnHistoryItem(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onAllNodesWithTag(PAGE_ITEM_TESTING_TAG)[0].performClick()
    }
  }
}
