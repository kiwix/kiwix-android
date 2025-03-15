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

package org.kiwix.kiwixmobile.error

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.testutils.TestUtils

fun errorActivity(func: ErrorActivityRobot.() -> Unit) = ErrorActivityRobot().apply(func)

class ErrorActivityRobot : BaseRobot() {
  fun assertSendDiagnosticReportDisplayed() {
    // Wait a bit for properly visible the HelpFragment.
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    isVisible(TextId(R.string.send_report))
  }

  fun clickOnSendDiagnosticReport() {
    clickOn(TextId(R.string.send_report))
  }

  fun assertErrorActivityDisplayed(composeTestRule: ComposeContentTestRule) {
    // Wait a bit for properly visible the ErrorActivity.
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    composeTestRule.onNodeWithText(context.getString(R.string.diagnostic_report))
      .assertIsDisplayed()
  }

  fun clickOnNoThanksButton(composeTestRule: ComposeContentTestRule) {
    composeTestRule.onNodeWithText(context.getString(R.string.no_thanks).uppercase()).performClick()
  }

  fun assertCheckBoxesDisplayed(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      onNodeWithText(context.getString(R.string.crash_checkbox_language))
        .assertIsDisplayed()
      onNodeWithText(context.getString(R.string.crash_checkbox_logs))
        .assertIsDisplayed()
      onNodeWithText(context.getString(R.string.crash_checkbox_zimfiles))
        .assertIsDisplayed()
    }
  }

  fun clickOnSendDetailsButton(composeTestRule: ComposeContentTestRule) {
    composeTestRule.onNodeWithText(context.getString(R.string.crash_button_confirm).uppercase())
      .performClick()
  }
}
