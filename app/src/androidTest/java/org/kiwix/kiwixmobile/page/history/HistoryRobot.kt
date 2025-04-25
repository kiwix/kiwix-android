/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import applyWithViewHierarchyPrinting
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.page.DELETE_MENU_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.SWITCH_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_TITLE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView

fun history(func: HistoryRobot.() -> Unit) = HistoryRobot().applyWithViewHierarchyPrinting(func)

class HistoryRobot : BaseRobot() {
  fun assertHistoryDisplayed(composeTestRule: ComposeTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(SWITCH_TEXT_TESTING_TAG)
        .assertTextEquals(context.getString(R.string.history_from_current_book))
    }
  }

  fun clickOnTrashIcon(composeTestRule: ComposeTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(DELETE_MENU_ICON_TESTING_TAG)
        .performClick()
    }
  }

  fun assertDeleteHistoryDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(ALERT_DIALOG_TITLE_TEXT_TESTING_TAG)
          .assertTextEquals(context.getString(R.string.delete_history))
      }
    })
  }
}
