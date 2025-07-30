/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.initial.download

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.ui.components.STORAGE_DEVICE_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.main.BOTTOM_NAV_DOWNLOADS_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.online.DOWNLOADING_STOP_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.storage.STORAGE_SELECTION_DIALOG_TITLE_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout

fun initialDownload(func: InitialDownloadRobot.() -> Unit) =
  InitialDownloadRobot().applyWithViewHierarchyPrinting(func)

class InitialDownloadRobot : BaseRobot() {
  fun clickDownloadOnBottomNav(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntilTimeout()
      onNodeWithTag(BOTTOM_NAV_DOWNLOADS_ITEM_TESTING_TAG).performClick()
    }
  }

  fun assertStorageConfigureDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    pauseForBetterTestPerformance()
    testFlakyView({
      composeTestRule.apply {
        waitUntilTimeout()
        onNodeWithTag(STORAGE_SELECTION_DIALOG_TITLE_TESTING_TAG)
          .assertTextEquals(context.getString(string.choose_storage_to_download_book))
      }
    })
  }

  fun clickOnInternalStorage(composeTestRule: ComposeContentTestRule) {
    pauseForBetterTestPerformance()
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onAllNodesWithTag(STORAGE_DEVICE_ITEM_TESTING_TAG)[0].performClick()
      }
    })
  }

  fun assertDownloadStop(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntilTimeout()
      try {
        onAllNodesWithTag(DOWNLOADING_STOP_BUTTON_TESTING_TAG)[0].assertExists()
        throw IllegalStateException("Could not stop download")
      } catch (_: AssertionError) {
        // no nothing if the stop button is not visible.
      }
    }
  }

  private fun pauseForBetterTestPerformance() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
  }
}
