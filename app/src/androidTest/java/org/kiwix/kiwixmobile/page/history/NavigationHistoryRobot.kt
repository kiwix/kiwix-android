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

import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.testutils.TestUtils

fun navigationHistory(func: NavigationHistoryRobot.() -> Unit) =
  NavigationHistoryRobot().applyWithViewHierarchyPrinting(func)

class NavigationHistoryRobot : BaseRobot() {

  fun checkZimFileLoadedSuccessful(readerFragment: Int) {
    isVisible(ViewId(readerFragment))
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

  fun longClickOnBackwardButton() {
    longClickOn(ViewId(R.id.bottom_toolbar_arrow_back))
  }

  fun longClickOnForwardButton() {
    longClickOn(ViewId(R.id.bottom_toolbar_arrow_forward))
  }

  fun assertBackwardNavigationHistoryDialogDisplayed() {
    isVisible(TextId(R.string.backward_history))
  }

  fun clickOnBackwardButton() {
    clickOn(ViewId(R.id.bottom_toolbar_arrow_back))
  }

  fun assertForwardNavigationHistoryDialogDisplayed() {
    isVisible(TextId(R.string.forward_history))
  }

  fun clickOnDeleteHistory() {
    clickOn(ViewId(R.id.menu_pages_clear))
  }

  fun assertDeleteDialogDisplayed() {
    isVisible(TextId(R.string.clear_all_history_dialog_title))
  }

  private fun pauseForBetterTestPerformance() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
  }
}
