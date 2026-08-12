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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.error.ERROR_REPORT_DETAILS_LIST_TESTING_TAG
import org.kiwix.kiwixmobile.core.help.SEND_DIAGNOSTIC_REPORT_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_TITLE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils.FIFTEEN_SECOND_DELAY
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView

fun errorActivity(func: ErrorActivityRobot.() -> Unit) = ErrorActivityRobot().apply(func)

class ErrorActivityRobot : BaseRobot() {
  fun assertSendDiagnosticReportDisplayed(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntil(FIFTEEN_SECOND_DELAY) {
        onAllNodesWithTag(SEND_DIAGNOSTIC_REPORT_TESTING_TAG)
          .fetchSemanticsNodes()
          .isNotEmpty()
      }
      onNodeWithTag(SEND_DIAGNOSTIC_REPORT_TESTING_TAG).assertIsDisplayed()
    }
  }

  fun clickOnSendDiagnosticReport(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(SEND_DIAGNOSTIC_REPORT_TESTING_TAG).performClick()
    }
  }

  fun assertErrorActivityDisplayed(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntil(FIFTEEN_SECOND_DELAY) {
        onAllNodesWithText(context.getString(R.string.diagnostic_report))
          .fetchSemanticsNodes()
          .isNotEmpty()
      }
      onNodeWithText(context.getString(R.string.diagnostic_report))
        .assertExists()
    }
  }

  fun clickOnNoThanksButton(composeTestRule: ComposeContentTestRule) {
    composeTestRule.onNodeWithText(context.getString(R.string.no_thanks).uppercase()).performClick()
  }

  fun assertDetailsIncludedInErrorReportDisplayed(composeTestRule: ComposeContentTestRule) {
    val detailsList = composeTestRule.onNodeWithTag(ERROR_REPORT_DETAILS_LIST_TESTING_TAG)
    listOf(
      R.string.crash_checkbox_language,
      R.string.crash_checkbox_logs,
      R.string.crash_checkbox_zimfiles,
      R.string.crash_checkbox_device,
      R.string.crash_checkbox_file_system,
      R.string.validate_zim_files
    ).forEach { stringId ->
      val text = context.getString(stringId)

      detailsList.performScrollToNode(hasText(text))

      composeTestRule
        .onNodeWithText(text)
        .assertIsDisplayed()
    }
  }

  fun clickOnSendDetailsButton(composeTestRule: ComposeContentTestRule) {
    composeTestRule.onNodeWithText(context.getString(R.string.crash_button_confirm).uppercase())
      .performClick()
  }

  fun assertZimFileValidationDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    runCatching {
      testFlakyView({
        composeTestRule.apply {
          onNodeWithTag(ALERT_DIALOG_TITLE_TEXT_TESTING_TAG)
            .assertTextEquals(context.getString(string.validating_zim_file))
        }
      })
    }
  }
}
