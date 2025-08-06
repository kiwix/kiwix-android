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
package org.kiwix.kiwixmobile.page.history

import android.util.Log
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import junit.framework.AssertionFailedError
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.reader.READER_BOTTOM_BAR_NEXT_SCREEN_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.READER_SCREEN_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.TAB_SWITCHER_VIEW_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.DELETE_MENU_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.TOOLBAR_TITLE_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_DISMISS_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_TITLE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.main.BOTTOM_NAV_READER_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout

fun navigationHistory(func: NavigationHistoryRobot.() -> Unit) =
  NavigationHistoryRobot().applyWithViewHierarchyPrinting(func)

class NavigationHistoryRobot : BaseRobot() {
  private var retryCountForClearNavigationHistory = 5
  private var retryCountForBackwardNavigationHistory = 5
  private var retryCountForForwardNavigationHistory = 5

  fun checkZimFileLoadedSuccessful(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntilTimeout()
      onNodeWithTag(READER_SCREEN_TESTING_TAG).assertExists()
    }
  }

  fun closeTabSwitcherIfVisible(composeTestRule: ComposeContentTestRule) {
    try {
      composeTestRule.apply {
        waitUntilTimeout()
        onNodeWithTag(TAB_SWITCHER_VIEW_TESTING_TAG).assertExists()
        pressBack()
      }
    } catch (_: AssertionError) {
      Log.i(
        "NAVIGATION_HISTORY_TEST",
        "Couldn't found tab switcher, probably it is not visible"
      )
    }
  }

  fun clickOnAndroidArticle() {
    pauseForBetterTestPerformance()
    onWebView()
      .withElement(
        findElement(
          Locator.XPATH,
          "//*[contains(text(), 'Android_(operating_system)')]"
        )
      )
      .perform(webClick())
  }

  fun assertZimFileLoaded() {
    pauseForBetterTestPerformance()
    onWebView()
      .withElement(
        findElement(
          Locator.XPATH,
          "//*[contains(text(), 'Android_(operating_system)')]"
        )
      )
  }

  fun longClickOnBackwardButton(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      testFlakyView({
        onNodeWithTag(READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG)
          .performTouchInput { longClick() }
      })
    }
  }

  fun longClickOnForwardButton(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      testFlakyView({
        onNodeWithTag(READER_BOTTOM_BAR_NEXT_SCREEN_BUTTON_TESTING_TAG)
          .performTouchInput { longClick() }
      })
    }
  }

  fun assertBackwardNavigationHistoryDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    try {
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(TOOLBAR_TITLE_TESTING_TAG)
          .assertTextEquals(context.getString(R.string.backward_history))
      }
    } catch (_: AssertionError) {
      pauseForBetterTestPerformance()
      if (retryCountForBackwardNavigationHistory > 0) {
        retryCountForBackwardNavigationHistory--
        assertBackwardNavigationHistoryDialogDisplayed(composeTestRule)
      }
    }
  }

  fun clickOnBackwardButton(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG)
        .performClick()
    }
  }

  fun assertForwardNavigationHistoryDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    try {
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(TOOLBAR_TITLE_TESTING_TAG)
          .assertTextEquals(context.getString(R.string.forward_history))
      }
    } catch (_: AssertionError) {
      pauseForBetterTestPerformance()
      if (retryCountForForwardNavigationHistory > 0) {
        retryCountForForwardNavigationHistory--
        assertForwardNavigationHistoryDialogDisplayed(composeTestRule)
      }
    }
  }

  fun clickOnDeleteHistory(composeTestRule: ComposeContentTestRule) {
    pauseForBetterTestPerformance()
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(DELETE_MENU_ICON_TESTING_TAG).performClick()
      }
    })
  }

  fun assertDeleteDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    try {
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(ALERT_DIALOG_TITLE_TEXT_TESTING_TAG)
          .assertTextEquals(context.getString(R.string.clear_all_history_dialog_title))
      }
    } catch (ignore: AssertionFailedError) {
      pauseForBetterTestPerformance()
      if (retryCountForClearNavigationHistory > 0) {
        retryCountForClearNavigationHistory--
        assertDeleteDialogDisplayed(composeTestRule)
      } else {
        throw RuntimeException("Could not found the NavigationHistoryDeleteDialog. Original exception = $ignore")
      }
    }
  }

  private fun pauseForBetterTestPerformance() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
  }

  fun clickOnReaderFragment(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitUntilTimeout()
        onNodeWithTag(BOTTOM_NAV_READER_ITEM_TESTING_TAG).performClick()
      }
    })
  }

  fun clickOnCancelButton(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitUntilTimeout()
        onNodeWithTag(ALERT_DIALOG_DISMISS_BUTTON_TESTING_TAG).performClick()
      }
    })
  }
}
