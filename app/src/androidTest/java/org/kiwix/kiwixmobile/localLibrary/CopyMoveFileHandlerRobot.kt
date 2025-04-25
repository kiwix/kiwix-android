/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.localLibrary

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.web.sugar.Web
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.Locator
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable
import org.kiwix.kiwixmobile.R.id
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_DISMISS_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_MESSAGE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_NATURAL_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.local.NO_FILE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.utils.RecyclerViewMatcher

fun copyMoveFileHandler(func: CopyMoveFileHandlerRobot.() -> Unit) =
  CopyMoveFileHandlerRobot().applyWithViewHierarchyPrinting(func)

class CopyMoveFileHandlerRobot : BaseRobot() {
  fun assertCopyMoveDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    pauseForBetterTestPerformance()
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(ALERT_DIALOG_MESSAGE_TEXT_TESTING_TAG)
        .assertTextEquals(context.getString(R.string.copy_move_files_dialog_description))
    }
  }

  fun assertCopyMoveDialogNotDisplayed(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(ALERT_DIALOG_MESSAGE_TEXT_TESTING_TAG)
          .assertDoesNotExist()
      }
    })
  }

  fun assertStorageSelectionDialogDisplayed() {
    testFlakyView({
      onView(withText(R.string.choose_storage_to_copy_move_zim_file))
    })
  }

  fun clickOnInternalStorage() {
    pauseForBetterTestPerformance()
    testFlakyView({
      onView(
        RecyclerViewMatcher(R.id.device_list).atPosition(
          0
        )
      ).perform(click())
    })
  }

  fun clickOnCopy(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG)
          .assertTextEquals(context.getString(R.string.action_copy).uppercase())
          .performClick()
      }
    })
  }

  fun clickOnMove(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(ALERT_DIALOG_DISMISS_BUTTON_TESTING_TAG)
          .assertTextEquals(context.getString(R.string.move).uppercase())
          .performClick()
      }
    })
  }

  fun clickOnCancel(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(ALERT_DIALOG_NATURAL_BUTTON_TESTING_TAG)
          .assertTextEquals(context.getString(R.string.cancel).uppercase())
          .performClick()
      }
    })
  }

  fun assertZimFileCopiedAndShowingIntoTheReader() {
    pauseForBetterTestPerformance()
    isVisible(Findable.ViewId(id.readerFragment))
    testFlakyView({
      Web.onWebView()
        .withElement(
          DriverAtoms.findElement(
            Locator.XPATH,
            "//*[contains(text(), 'Android_(operating_system)')]"
          )
        )
    })
  }

  fun assertZimFileAddedInTheLocalLibrary(composeTestRule: ComposeContentTestRule) {
    try {
      composeTestRule.onNodeWithTag(NO_FILE_TEXT_TESTING_TAG).assertIsDisplayed()
      throw RuntimeException("ZimFile not added in the local library")
    } catch (_: AssertionError) {
      // do nothing zim file is added in the local library
    }
  }

  fun pauseForBetterTestPerformance() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
  }
}
