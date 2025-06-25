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

package org.kiwix.kiwixmobile.nav.destination.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.main.reader.CONTENT_LOADING_PROGRESSBAR_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.localFileTransfer.LocalFileTransferRobot
import org.kiwix.kiwixmobile.localFileTransfer.localFileTransfer
import org.kiwix.kiwixmobile.nav.destination.library.local.BOOK_LIST_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.local.LOCAL_FILE_TRANSFER_MENU_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.local.NO_FILE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.refresh
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.ui.BOOK_ITEM_TESTING_TAG

fun library(func: LibraryRobot.() -> Unit) = LibraryRobot().applyWithViewHierarchyPrinting(func)

class LibraryRobot : BaseRobot() {
  private val zimFileTitle = "Test_Zim"

  fun assertGetZimNearbyDeviceDisplayed(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(LOCAL_FILE_TRANSFER_MENU_BUTTON_TESTING_TAG).assertIsDisplayed()
      }
    })
  }

  fun clickFileTransferIcon(
    composeTestRule: ComposeContentTestRule,
    func: LocalFileTransferRobot.() -> Unit
  ) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(LOCAL_FILE_TRANSFER_MENU_BUTTON_TESTING_TAG).performClick()
    }
    localFileTransfer(func)
  }

  fun assertLibraryListDisplayed(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag(BOOK_LIST_TESTING_TAG).assertIsDisplayed()
    })
  }

  private fun assertNoFilesTextDisplayed(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag(NO_FILE_TEXT_TESTING_TAG).assertIsDisplayed()
    })
  }

  fun refreshList(composeTestRule: ComposeContentTestRule) {
    try {
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag(NO_FILE_TEXT_TESTING_TAG).assertIsDisplayed()
      composeTestRule.refresh()
    } catch (_: AssertionError) {
      try {
        composeTestRule.onNodeWithTag(BOOK_LIST_TESTING_TAG).assertIsDisplayed()
        composeTestRule.refresh()
      } catch (_: AssertionError) {
        Log.i(
          "LOCAL_LIBRARY",
          "No need to refresh the data, since there is no files found"
        )
      }
    }
  }

  fun waitUntilZimFilesRefreshing(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.waitUntilTimeout()
      composeTestRule.onNodeWithTag(CONTENT_LOADING_PROGRESSBAR_TESTING_TAG)
        .assertIsNotDisplayed()
    })
  }

  fun deleteZimIfExists(composeTestRule: ComposeContentTestRule) {
    try {
      try {
        composeTestRule.onNodeWithTag(NO_FILE_TEXT_TESTING_TAG).assertIsDisplayed()
        // if this view is displaying then we do not need to run the further code.
        return
      } catch (_: AssertionError) {
        Log.e("DELETE_ZIM_FILE", "Zim files found in local library so we are deleting them")
      }
      val zimFileNodes = composeTestRule.onAllNodesWithTag(BOOK_ITEM_TESTING_TAG)
      val itemCount = zimFileNodes.fetchSemanticsNodes().size
      repeat(itemCount) { index ->
        zimFileNodes[index].performTouchInput { longClick() }
      }
      clickOnFileDeleteIcon()
      clickOnDeleteZimFile(composeTestRule)
      pauseForBetterTestPerformance()
      assertNoFilesTextDisplayed(composeTestRule)
    } catch (e: Exception) {
      Log.i(
        "TEST_DELETE_ZIM",
        "Failed to delete ZIM file with title [" + zimFileTitle + "]... " +
          "Probably because it doesn't exist. \nOriginal Exception = $e"
      )
    }
  }

  private fun clickOnFileDeleteIcon() {
    pauseForBetterTestPerformance()
    testFlakyView({ clickOn(ViewId(R.id.zim_file_delete_item)) })
  }

  private fun clickOnDeleteZimFile(composeTestRule: ComposeContentTestRule) {
    // This code is flaky since the DELETE button is inside the dialog, and sometimes it visible
    // on window but espresso unable to find it so we are adding a retrying mechanism here.
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG).performClick()
      }
    })
  }

  private fun pauseForBetterTestPerformance() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
  }
}
