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
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import junit.framework.AssertionFailedError
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.testutils.TestUtils

fun navigationHistory(func: NavigationHistoryRobot.() -> Unit) =
  NavigationHistoryRobot().applyWithViewHierarchyPrinting(func)

class NavigationHistoryRobot : BaseRobot() {

  private var retryCountForClearNavigationHistory = 5
  private var retryCountForBackwardNavigationHistory = 5
  private var retryCountForForwardNavigationHistory = 5

  fun checkZimFileLoadedSuccessful(readerFragment: Int) {
    pauseForBetterTestPerformance()
    isVisible(ViewId(readerFragment))
  }

  fun closeTabSwitcherIfVisible() {
    try {
      pauseForBetterTestPerformance()
      isVisible(ViewId(R.id.tab_switcher_close_all_tabs))
      pressBack()
    } catch (ignore: Exception) {
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

  fun longClickOnBackwardButton() {
    pauseForBetterTestPerformance()
    longClickOn(ViewId(R.id.bottom_toolbar_arrow_back))
  }

  fun longClickOnForwardButton() {
    pauseForBetterTestPerformance()
    longClickOn(ViewId(R.id.bottom_toolbar_arrow_forward))
  }

  fun assertBackwardNavigationHistoryDialogDisplayed() {
    try {
      isVisible(TextId(R.string.backward_history))
    } catch (ignore: AssertionFailedError) {
      pauseForBetterTestPerformance()
      if (retryCountForBackwardNavigationHistory > 0) {
        retryCountForBackwardNavigationHistory--
        assertBackwardNavigationHistoryDialogDisplayed()
      }
    }
  }

  fun clickOnBackwardButton() {
    pauseForBetterTestPerformance()
    clickOn(ViewId(R.id.bottom_toolbar_arrow_back))
  }

  fun assertForwardNavigationHistoryDialogDisplayed() {
    try {
      isVisible(TextId(R.string.forward_history))
    } catch (ignore: AssertionFailedError) {
      pauseForBetterTestPerformance()
      if (retryCountForForwardNavigationHistory > 0) {
        retryCountForForwardNavigationHistory--
        assertForwardNavigationHistoryDialogDisplayed()
      }
    }
  }

  fun clickOnDeleteHistory() {
    pauseForBetterTestPerformance()
    clickOn(ViewId(R.id.menu_pages_clear))
  }

  fun assertDeleteDialogDisplayed() {
    try {
      isVisible(TextId(R.string.clear_all_history_dialog_title))
    } catch (ignore: AssertionFailedError) {
      pauseForBetterTestPerformance()
      if (retryCountForClearNavigationHistory > 0) {
        retryCountForClearNavigationHistory--
        assertDeleteDialogDisplayed()
      }
    }
  }

  private fun pauseForBetterTestPerformance() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
  }
}
