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

package org.kiwix.kiwixmobile.download

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import applyWithViewHierarchyPrinting
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.TOOLBAR_TITLE_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_TITLE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.download.DownloadTest.Companion.KIWIX_DOWNLOAD_TEST
import org.kiwix.kiwixmobile.nav.destination.library.local.NO_FILE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.online.DOWNLOADING_PAUSE_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.online.DOWNLOADING_STATE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.online.DOWNLOADING_STOP_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.online.NO_CONTENT_VIEW_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.online.ONLINE_BOOK_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.online.ONLINE_DIVIDER_ITEM_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.online.ONLINE_LIBRARY_SEARCH_VIEW_CLOSE_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.online.ONLINE_LIBRARY_SEARCH_VIEW_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.online.SHOW_FETCHING_LIBRARY_LAYOUT_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.refresh
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout

fun downloadRobot(func: DownloadRobot.() -> Unit) =
  DownloadRobot().applyWithViewHierarchyPrinting(func)

class DownloadRobot : BaseRobot() {
  fun clickLibraryOnBottomNav() {
    clickOn(ViewId(R.id.libraryFragment))
  }

  fun clickDownloadOnBottomNav() {
    clickOn(ViewId(R.id.downloadsFragment))
  }

  // Increasing the default timeout for data loading because, on the Android 16 Emulator,
  // the internet connection is slow, and the library download takes longer.
  fun waitForDataToLoad(
    retryCountForDataToLoad: Int = 20,
    composeTestRule: ComposeContentTestRule
  ) {
    try {
      composeTestRule.waitUntil(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
        composeTestRule.onAllNodesWithTag(ONLINE_BOOK_ITEM_TESTING_TAG)[0].isDisplayed()
      }
    } catch (e: ComposeTimeoutException) {
      if (retryCountForDataToLoad > 0) {
        // refresh the data if there is "Swipe Down for Library" visible on the screen.
        refreshOnlineListIfSwipeDownForLibraryTextVisible(composeTestRule)
        waitForDataToLoad(retryCountForDataToLoad - 1, composeTestRule)
        return
      }
      // throw the exception when there is no more retry left.
      throw RuntimeException("Couldn't load the online library list.\n Original exception = $e")
    }
  }

  fun checkLanguageFilterAppliedToOnlineContent(
    composeTestRule: ComposeContentTestRule,
    language: String
  ) {
    composeTestRule.apply {
      onNodeWithTag(ONLINE_DIVIDER_ITEM_TEXT_TESTING_TAG).assertTextEquals(language)
    }
  }

  private fun refreshOnlineListIfSwipeDownForLibraryTextVisible(composeTestRule: ComposeContentTestRule) {
    try {
      composeTestRule.onNodeWithTag(NO_CONTENT_VIEW_TEXT_TESTING_TAG).assertIsDisplayed()
      refreshOnlineList(composeTestRule)
    } catch (_: AssertionError) {
      try {
        // do nothing as currently downloading the online library.
        composeTestRule
          .onNodeWithTag(SHOW_FETCHING_LIBRARY_LAYOUT_TESTING_TAG)
          .assertIsDisplayed()
      } catch (_: AssertionError) {
        // if not visible try to get the online library.
        refreshOnlineList(composeTestRule)
      }
    }
  }

  fun checkIfZimFileDownloaded(composeTestRule: ComposeContentTestRule) {
    try {
      testFlakyView({
        composeTestRule.apply {
          waitUntilTimeout()
          onNodeWithTag(NO_FILE_TEXT_TESTING_TAG).assertIsDisplayed()
        }
      })
      // if the "No files here" text found that means it failed to download the ZIM file.
      throw RuntimeException("Couldn't download the zim file. The [No files here] text is visible on screen")
    } catch (_: AssertionError) {
      // check if "No files here" text is not visible on
      // screen that means zim file is downloaded successfully.
    }
  }

  private fun refreshOnlineList(composeTestRule: ComposeContentTestRule) {
    composeTestRule.refresh()
  }

  fun downloadZimFile(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitUntilTimeout()
        onAllNodesWithTag(ONLINE_BOOK_ITEM_TESTING_TAG)[0].performClick()
      }
    })
  }

  fun assertDownloadStart(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitUntil(TestUtils.TEST_PAUSE_MS.toLong()) {
          onAllNodesWithTag(DOWNLOADING_STOP_BUTTON_TESTING_TAG)[0].isDisplayed()
        }
      }
    })
  }

  fun pauseDownload(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        onAllNodesWithTag(DOWNLOADING_PAUSE_BUTTON_TESTING_TAG)[0].performClick()
      }
    })
  }

  fun assertDownloadPaused(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntilTimeout(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong())
      waitUntil(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
        composeTestRule.onAllNodesWithTag(DOWNLOADING_STATE_TEXT_TESTING_TAG)[0]
          .assertTextEquals(context.getString(org.kiwix.kiwixmobile.core.R.string.paused_state))
          .isDisplayed()
      }
    }
  }

  fun resumeDownload(composeTestRule: ComposeContentTestRule) {
    pauseDownload(composeTestRule)
  }

  fun assertDownloadResumed(composeTestRule: ComposeContentTestRule) {
    try {
      composeTestRule.apply {
        waitUntilTimeout()
        onAllNodesWithTag(DOWNLOADING_STATE_TEXT_TESTING_TAG)[0]
          .assertTextEquals(context.getString(org.kiwix.kiwixmobile.core.R.string.paused_state))
      }
      throw IllegalStateException("Could not resume the download")
    } catch (_: AssertionError) {
      // do nothing since download is resumed.
    }
  }

  // wait for 5 minutes for downloading the ZIM file
  fun waitUntilDownloadComplete(
    retryCountForDownloadingZimFile: Int = 30,
    composeTestRule: ComposeContentTestRule
  ) {
    try {
      composeTestRule.onAllNodesWithTag(DOWNLOADING_STOP_BUTTON_TESTING_TAG)[0].assertDoesNotExist()
      Log.e(KIWIX_DOWNLOAD_TEST, "Download complete")
    } catch (e: AssertionError) {
      if (retryCountForDownloadingZimFile > 0) {
        resumeDownloadIfPaused(composeTestRule)
        composeTestRule.waitUntilTimeout(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong())
        Log.e(KIWIX_DOWNLOAD_TEST, "Downloading in progress")
        waitUntilDownloadComplete(retryCountForDownloadingZimFile - 1, composeTestRule)
        return
      }
      // throw the exception when there is no more retry left.
      throw RuntimeException("Couldn't download the ZIM file.\n Original exception = $e")
    }
  }

  private fun resumeDownloadIfPaused(composeTestRule: ComposeContentTestRule) {
    try {
      composeTestRule
        .onAllNodesWithTag(DOWNLOADING_STATE_TEXT_TESTING_TAG)[0]
        .assertTextEquals(context.getString(org.kiwix.kiwixmobile.core.R.string.paused_state))
      resumeDownload(composeTestRule)
    } catch (_: AssertionError) {
      // do nothing since downloading is In Progress.
    } catch (_: RuntimeException) {
      // do nothing since downloading is In Progress.
    }
  }

  fun assertStopDownloadDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitUntilTimeout()
        onNodeWithTag(ALERT_DIALOG_TITLE_TEXT_TESTING_TAG)
          .assertTextEquals(context.getString(string.confirm_stop_download_title))
      }
    })
  }

  fun clickOnYesButton(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG).performClick()
      }
    })
  }

  fun stopDownloadIfAlreadyStarted(composeTestRule: ComposeContentTestRule) {
    try {
      composeTestRule.waitUntilTimeout()
      stopDownload(composeTestRule)
      assertStopDownloadDialogDisplayed(composeTestRule)
      clickOnYesButton(composeTestRule)
      composeTestRule.waitUntilTimeout()
    } catch (_: AssertionError) {
      Log.e(
        KIWIX_DOWNLOAD_TEST,
        "Failed to stop downloading. Probably because it is not downloading the zim file"
      )
    } catch (_: ComposeTimeoutException) {
      Log.e(
        KIWIX_DOWNLOAD_TEST,
        "Failed to stop downloading. Probably because it is not downloading the zim file"
      )
    }
  }

  fun stopDownload(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      val stopButton = onAllNodesWithTag(DOWNLOADING_STOP_BUTTON_TESTING_TAG)[0]
      waitUntil(TestUtils.TEST_PAUSE_MS.toLong()) { stopButton.isDisplayed() }
      stopButton.performClick()
    }
  }

  fun clickOnSearchIcon(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntilTimeout()
      onNodeWithTag(SEARCH_ICON_TESTING_TAG).performClick()
    }
  }

  fun clickOnClearSearchIcon(composeTestRule: ComposeContentTestRule) {
    ONLINE_LIBRARY_SEARCH_VIEW_CLOSE_BUTTON_TESTING_TAG
    testFlakyView({
      composeTestRule.apply {
        waitUntilTimeout()
        onNodeWithTag(ONLINE_LIBRARY_SEARCH_VIEW_CLOSE_BUTTON_TESTING_TAG).performClick()
      }
    })
  }

  fun searchWikipediaZIMFiles(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitUntilTimeout()
        val searchView = onNodeWithTag(ONLINE_LIBRARY_SEARCH_VIEW_TESTING_TAG)
        searchView.performTextInput("")
        searchView.performTextInput("Wikipedia")
      }
    })
  }

  fun assertPreviousSearchRemainsActive(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitUntilTimeout()
        onNodeWithTag(ONLINE_LIBRARY_SEARCH_VIEW_TESTING_TAG)
          .assertTextEquals("Wikipedia")
      }
    })
  }

  fun assertSearchViewIsNotActive(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitUntilTimeout()
        onNodeWithTag(TOOLBAR_TITLE_TESTING_TAG)
          .assertTextEquals(context.getString(string.download))
      }
    })
  }
}
