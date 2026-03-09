/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import applyWithViewHierarchyPrinting
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_MESSAGE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_TITLE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout

fun update(func: UpdateRobot.() -> Unit) = UpdateRobot().applyWithViewHierarchyPrinting(func)
class UpdateRobot : BaseRobot() {
  fun assertUpdateDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntilTimeout()
      onNodeWithTag(ALERT_DIALOG_TITLE_TEXT_TESTING_TAG)
        .assertIsDisplayed()
        .assertTextEquals(context.getString(string.new_update_available_title))
    }
  }

  // need research on how to test this.
  fun assertUpdateDialogIsNotDisplayed(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      composeTestRule.apply {
        waitUntilTimeout()
        onNodeWithTag(ALERT_DIALOG_MESSAGE_TEXT_TESTING_TAG).assertDoesNotExist()
      }
    }
  }
}
