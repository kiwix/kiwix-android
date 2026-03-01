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
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import applyWithViewHierarchyPrinting
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.TOOLBAR_TITLE_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_TITLE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.download.DownloadTest.Companion.KIWIX_DOWNLOAD_TEST
import org.kiwix.kiwixmobile.main.BOTTOM_NAV_DOWNLOADS_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.main.BOTTOM_NAV_LIBRARY_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.main.KiwixMainActivity
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
  private val searchZIMFileTitle = "Zapping Sauvage"
  fun clickLibraryOnBottomNav(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntilTimeout()
      onNodeWithTag(BOTTOM_NAV_LIBRARY_ITEM_TESTING_TAG).performClick()
    }
  }

  fun clickDownloadOnBottomNav(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntilTimeout()
      onNodeWithTag(BOTTOM_NAV_DOWNLOADS_ITEM_TESTING_TAG).performClick()
    }
  }

  // Increasing the default timeout for data loading because, on the Android 16 Emulator,
  // the internet connection is slow, and the library download takes longer.
  fun waitForDataToLoad(
    retryCountForDataToLoad: Int = 20,
    composeTestRule: ComposeContentTestRule
  ) {
    try {
      composeTestRule.waitUntil(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
        composeTestRule.onAllNodesWithTag(ONLINE_BOOK_ITEM_TESTING_TAG)
          .fetchSemanticsNodes().isNotEmpty()
      }
    } catch (e: ComposeTimeoutException) {
      Log.e(
        KIWIX_DOWNLOAD_TEST,
        "Timeout waiting for data to load. Nodes with tag $ONLINE_BOOK_ITEM_TESTING_TAG: " +
          "${composeTestRule.onAllNodesWithTag(ONLINE_BOOK_ITEM_TESTING_TAG).fetchSemanticsNodes().size}"
      )
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

  fun searchZappingSauvageFile(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle() // let the compose settle.
        runCatching {
          // if searchView already opened do nothing.
          onNodeWithTag(ONLINE_LIBRARY_SEARCH_VIEW_CLOSE_BUTTON_TESTING_TAG).assertExists()
        }.onFailure {
          // if searchView is not opened then open it.
          onNodeWithTag(SEARCH_ICON_TESTING_TAG).performClick()
        }
        waitForIdle()
        onNodeWithTag(ONLINE_LIBRARY_SEARCH_VIEW_TESTING_TAG).apply {
          performTextClearance()
          performTextInput(searchZIMFileTitle)
        }
        waitUntilTimeout(1000) // Reduced from 10s as it's sufficient for searching an item.
      }
    })
  }

  private fun refreshOnlineList(composeTestRule: ComposeContentTestRule) {
    composeTestRule.refresh()
  }

  fun downloadZimFile(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitUntil(TestUtils.TEST_PAUSE_MS.toLong()) {
          onAllNodesWithTag(ONLINE_BOOK_ITEM_TESTING_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        onAllNodesWithTag(ONLINE_BOOK_ITEM_TESTING_TAG).onFirst().performClick()
      }
    })
  }

  fun assertDownloadStart(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitUntil(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
          onAllNodesWithTag(DOWNLOADING_STOP_BUTTON_TESTING_TAG)
            .fetchSemanticsNodes().isNotEmpty()
        }
      }
    })
  }

  fun pauseDownload(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      runCatching {
        waitUntil(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
          onAllNodesWithTag(DOWNLOADING_PAUSE_BUTTON_TESTING_TAG)
            .fetchSemanticsNodes().isNotEmpty()
        }
        onAllNodesWithTag(DOWNLOADING_PAUSE_BUTTON_TESTING_TAG).onFirst().performClick()
      }.onFailure {
        Log.e(KIWIX_DOWNLOAD_TEST, "Failed to pause download - it might have finished already: ${it.message}")
      }
      waitForIdle()
    }
  }

  fun resumeDownload(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      runCatching {
        waitUntil(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
          val resumeNodes = onAllNodesWithTag(DOWNLOADING_PAUSE_BUTTON_TESTING_TAG)
            .fetchSemanticsNodes()
          val stopNodes = onAllNodesWithTag(DOWNLOADING_STOP_BUTTON_TESTING_TAG)
            .fetchSemanticsNodes()
          resumeNodes.isNotEmpty() || stopNodes.isEmpty()
        }
        val nodes = onAllNodesWithTag(DOWNLOADING_PAUSE_BUTTON_TESTING_TAG).fetchSemanticsNodes()
        if (nodes.isNotEmpty()) {
          onAllNodesWithTag(DOWNLOADING_PAUSE_BUTTON_TESTING_TAG).onFirst().performClick()
        }
      }.onFailure {
        Log.e(KIWIX_DOWNLOAD_TEST, "Failed to resume download - it might have finished already: ${it.message}")
      }
      waitForIdle()
    }
  }

  fun assertDownloadResumed(
    composeTestRule: ComposeContentTestRule,
    kiwixMainActivity: KiwixMainActivity
  ) {
    composeTestRule.apply {
      waitForIdle()
      val pauseState = kiwixMainActivity.getString(org.kiwix.kiwixmobile.core.R.string.paused_state)

      // Wait for the state text to NO LONGER be "paused"
      // Or for the node to disappear entirely (meaning it finished downloading!)
      waitUntil(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
        try {
          val nodes = onAllNodesWithTag(DOWNLOADING_STATE_TEXT_TESTING_TAG).fetchSemanticsNodes()
          if (nodes.isEmpty()) {
            true // Node disappeared, likely download finished.
          } else {
            // Check if text is NOT pauseState
            try {
              onAllNodesWithTag(DOWNLOADING_STATE_TEXT_TESTING_TAG).onFirst().assertTextEquals(pauseState)
              false // Still paused
            } catch (_: AssertionError) {
              true // Text changed!
            }
          }
        } catch (_: Exception) {
          true // Any other error, we treat as "it's not paused anymore" or "it's gone"
        }
      }

      // Final sanity check: if it still exists, it shouldn't be "paused"
      runCatching {
        onAllNodesWithTag(DOWNLOADING_STATE_TEXT_TESTING_TAG).onFirst().assertTextEquals(pauseState)
      }.onSuccess {
        throw IllegalStateException("Download is still in paused state after resume call")
      }.onFailure {
        // Correct - either doesn't exist or text is not paused.
      }
    }
  }

  fun assertDownloadPaused(
    composeTestRule: ComposeContentTestRule,
    kiwixMainActivity: KiwixMainActivity
  ) {
    composeTestRule.apply {
      waitForIdle()
      val pauseState = kiwixMainActivity.getString(org.kiwix.kiwixmobile.core.R.string.paused_state)

      // Wait for the state text to BE "paused"
      waitUntil(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
        try {
          onAllNodesWithTag(DOWNLOADING_STATE_TEXT_TESTING_TAG)
            .onFirst()
            .assertTextEquals(pauseState)
          true
        } catch (_: Exception) {
          false
        }
      }
      onAllNodesWithTag(DOWNLOADING_STATE_TEXT_TESTING_TAG).onFirst().assertIsDisplayed()
      onAllNodesWithTag(DOWNLOADING_STATE_TEXT_TESTING_TAG).onFirst().assertTextEquals(pauseState)
    }
  }

  // wait for 5 minutes for downloading the ZIM file
  fun waitUntilDownloadComplete(
    retryCountForDownloadingZimFile: Int = 30,
    composeTestRule: ComposeContentTestRule,
    kiwixMainActivity: KiwixMainActivity
  ) {
    val nodes = composeTestRule.onAllNodesWithTag(DOWNLOADING_STOP_BUTTON_TESTING_TAG).fetchSemanticsNodes()
    if (nodes.isEmpty()) {
      Log.e(KIWIX_DOWNLOAD_TEST, "Download complete (Stop button not found)")
      return
    }

    if (retryCountForDownloadingZimFile > 0) {
      resumeDownloadIfPaused(composeTestRule, kiwixMainActivity)
      Log.e(KIWIX_DOWNLOAD_TEST, "Downloading in progress, retryCount = $retryCountForDownloadingZimFile")
      composeTestRule.waitUntilTimeout(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong())
      waitUntilDownloadComplete(
        retryCountForDownloadingZimFile - 1,
        composeTestRule,
        kiwixMainActivity
      )
    } else {
      throw RuntimeException("Couldn't download the ZIM file within timeout.")
    }
  }

  private fun resumeDownloadIfPaused(
    composeTestRule: ComposeContentTestRule,
    kiwixMainActivity: KiwixMainActivity
  ) {
    try {
      val pauseState = kiwixMainActivity.getString(org.kiwix.kiwixmobile.core.R.string.paused_state)
      val nodes = composeTestRule.onAllNodesWithTag(DOWNLOADING_STATE_TEXT_TESTING_TAG).fetchSemanticsNodes()
      if (nodes.isNotEmpty()) {
        val text = nodes[0].config[androidx.compose.ui.semantics.SemanticsProperties.Text][0].text
        if (text == pauseState) {
          Log.e(KIWIX_DOWNLOAD_TEST, "Download paused, resuming...")
          resumeDownload(composeTestRule)
        }
      }
    } catch (_: Exception) {
      // Ignore errors during check
    }
  }

  fun assertStopDownloadDialogDisplayed(
    composeTestRule: ComposeContentTestRule,
    kiwixMainActivity: KiwixMainActivity
  ) {
    testFlakyView({
      composeTestRule.apply {
        waitUntil(TestUtils.TEST_PAUSE_MS.toLong()) {
          onAllNodesWithTag(ALERT_DIALOG_TITLE_TEXT_TESTING_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag(ALERT_DIALOG_TITLE_TEXT_TESTING_TAG)
          .assertTextEquals(kiwixMainActivity.getString(string.confirm_stop_download_title))
      }
    })
  }

  fun clickOnYesButton(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitUntil(TestUtils.TEST_PAUSE_MS.toLong()) {
          onAllNodesWithTag(ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag(ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG).performClick()
      }
    })
  }

  fun stopDownloadIfAlreadyStarted(
    composeTestRule: ComposeContentTestRule,
    kiwixMainActivity: KiwixMainActivity
  ) {
    try {
      val nodes = composeTestRule.onAllNodesWithTag(DOWNLOADING_STOP_BUTTON_TESTING_TAG).fetchSemanticsNodes()
      if (nodes.isNotEmpty()) {
        stopDownload(composeTestRule)
        assertStopDownloadDialogDisplayed(composeTestRule, kiwixMainActivity)
        clickOnYesButton(composeTestRule)
        composeTestRule.waitForIdle()
      }
    } catch (e: Exception) {
      Log.e(KIWIX_DOWNLOAD_TEST, "Failed to stop downloading: ${e.message}")
    }
  }

  fun stopDownload(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntil(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
        onAllNodesWithTag(DOWNLOADING_STOP_BUTTON_TESTING_TAG)
          .fetchSemanticsNodes().isNotEmpty()
      }
      onAllNodesWithTag(DOWNLOADING_STOP_BUTTON_TESTING_TAG).onFirst().performClick()
      waitForIdle()
    }
  }

  fun clickOnSearchIcon(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntilTimeout()
      onNodeWithTag(SEARCH_ICON_TESTING_TAG).performClick()
    }
  }

  fun clickOnClearSearchIcon(composeTestRule: ComposeContentTestRule) {
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

  fun assertSearchViewIsNotActive(
    composeTestRule: ComposeContentTestRule,
    kiwixMainActivity: KiwixMainActivity
  ) {
    testFlakyView({
      composeTestRule.apply {
        waitUntilTimeout()
        onNodeWithTag(TOOLBAR_TITLE_TESTING_TAG)
          .assertTextEquals(kiwixMainActivity.getString(string.download))
      }
    })
  }
}
