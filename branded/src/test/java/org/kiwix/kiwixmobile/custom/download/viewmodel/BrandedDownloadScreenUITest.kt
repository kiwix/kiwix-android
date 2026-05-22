/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.custom.download.viewmodel

import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.tonyodev.fetch2.Error
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.downloader.model.Seconds
import org.kiwix.kiwixmobile.core.ui.components.CONTENT_LOADING_PROGRESS_BAR_TESTING_TAG
import org.kiwix.kiwixmobile.custom.download.State
import org.kiwix.kiwixmobile.custom.download.BrandedDownloadScreen
import org.kiwix.kiwixmobile.custom.download.BrandedDownloadScreenTags
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class BrandedDownloadScreenUITest {
  @get:Rule
  val composeRule = createComposeRule()

  private fun setScreenContent(
    state: State,
    onDownloadClick: () -> Unit = {},
    onRetryClick: () -> Unit = {}
  ) {
    composeRule.setContent {
      BrandedDownloadScreen(
        state = state,
        onDownloadClick = onDownloadClick,
        onRetryClick = onRetryClick
      )
    }
    composeRule.waitForIdle()
  }

  private fun createDownloadItem(
    downloadState: DownloadState,
    downloadId: Long = 1,
    progress: Int = 10
  ) = DownloadItem(
    downloadId = downloadId,
    favIconUrl = "",
    title = "Book",
    description = "",
    bytesDownloaded = 100,
    totalSizeBytes = 1000,
    progress = progress,
    eta = Seconds(0),
    downloadState = downloadState
  )

  private val failItem = State.DownloadFailed(DownloadState.Failed(Error.UNKNOWN, null))

  @Test
  fun downloadRequired_showsDownloadRequiredButton() {
    setScreenContent(State.DownloadRequired)

    composeRule.onNodeWithTag(BrandedDownloadScreenTags.DOWNLOAD_REQUIRED_BUTTON).assertExists()
  }

  @Test
  fun clickDownloadRequiredButton_invokesCallback() {
    var clicked = false

    setScreenContent(State.DownloadRequired, onDownloadClick = { clicked = true })

    composeRule.onNodeWithTag(BrandedDownloadScreenTags.DOWNLOAD_REQUIRED_BUTTON).performClick()

    assertTrue(clicked)
  }

  @Test
  fun downloadInProgress_showsProgressBar() {
    val item = createDownloadItem(DownloadState.Running)

    setScreenContent(State.DownloadInProgress(listOf(item)))

    composeRule.onNodeWithTag(CONTENT_LOADING_PROGRESS_BAR_TESTING_TAG).assertExists()
  }

  @Test
  fun downloadInProgress_showsEta() {
    val item = createDownloadItem(DownloadState.Running)

    setScreenContent(State.DownloadInProgress(listOf(item)))

    composeRule.onNodeWithTag(BrandedDownloadScreenTags.DOWNLOAD_ETA_TEXT).assertExists()
  }

  @Test
  fun downloadInProgress_showsDownloadState() {
    val item = createDownloadItem(DownloadState.Running)

    setScreenContent(State.DownloadInProgress(listOf(item)))

    composeRule.onNodeWithTag(BrandedDownloadScreenTags.DOWNLOAD_STATE_TEXT).assertExists()
  }

  @Test
  fun downloadFailed_showsErrorText() {
    setScreenContent(failItem)

    composeRule.onNodeWithTag(BrandedDownloadScreenTags.ERROR_MESSAGE_TEXT).assertExists()
  }

  @Test
  fun downloadFailed_showsRetryButton() {
    setScreenContent(failItem)

    composeRule.onNodeWithTag(BrandedDownloadScreenTags.RETRY_BUTTON).assertExists()
  }

  @Test
  fun retryButton_invokesCallback() {
    var clicked = false

    setScreenContent(failItem, onRetryClick = { clicked = true })

    composeRule.onNodeWithTag(BrandedDownloadScreenTags.RETRY_BUTTON).performClick()

    assertTrue(clicked)
  }

  @Test
  fun downloadCompleted_showsDownloadCompletedText() {
    setScreenContent(State.DownloadComplete)

    composeRule.onNodeWithTag(BrandedDownloadScreenTags.DOWNLOAD_COMPLETE_TEXT).assertExists()
  }
}
