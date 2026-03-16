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

package org.kiwix.kiwixmobile.update

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import applyWithViewHierarchyPrinting
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.core.ui.components.NAVIGATION_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_TITLE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.update.composables.APK_CANCEL_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.update.composables.INSTALL_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.update.composables.UPDATE_BUTTON_TESTING_TAG

fun updateScreenRobot(func: UpdateScreenRobot.() -> Unit) =
  UpdateScreenRobot().applyWithViewHierarchyPrinting(func)

class UpdateScreenRobot : BaseRobot() {
  fun navigateToUpdateScreen(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG)
        .performClick()
    }
  }

  fun clickUpdateButton(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      onNodeWithTag(UPDATE_BUTTON_TESTING_TAG)
        .performClick()
    }
  }

  fun clickNavigationButton(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      onNodeWithTag(NAVIGATION_ICON_TESTING_TAG)
        .performClick()
    }
  }

  fun assertDownloadApkStart(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitUntilTimeout()
        waitUntil(TestUtils.TEST_PAUSE_MS.toLong()) {
          onAllNodesWithTag(APK_CANCEL_BUTTON_TESTING_TAG)[0].isDisplayed()
        }
      }
    })
  }

  fun waitForApkInfoToLoad(
    composeTestRule: ComposeContentTestRule
  ) {
    composeTestRule.waitUntil(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
      composeTestRule.onAllNodesWithTag(UPDATE_BUTTON_TESTING_TAG)[0].isDisplayed()
    }
  }

  fun waitForInstallToLoad(
    composeTestRule: ComposeContentTestRule
  ) {
    composeTestRule.waitUntil(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
      composeTestRule.onAllNodesWithTag(INSTALL_BUTTON_TESTING_TAG)[0].isDisplayed()
    }
  }

  fun assertStopDownloadDialogDisplayed(
    composeTestRule: ComposeContentTestRule
  ) {
    testFlakyView({
      composeTestRule.apply {
        waitUntilTimeout()
        onNodeWithTag(ALERT_DIALOG_TITLE_TEXT_TESTING_TAG)
          .assertTextEquals("Stop download?")
      }
    })
  }
}
