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

package org.kiwix.kiwixmobile.nav.destination.library.online

import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.content.ContextCompat
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.downloader.model.Seconds
import org.kiwix.kiwixmobile.core.zim_manager.Byte
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.LibraryDownloadItem
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class DownloadBookItemUITest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val testIndex = 0
  private val context get() = RuntimeEnvironment.getApplication()

  private lateinit var mockOnPauseResumeClick: (LibraryDownloadItem) -> Unit
  private lateinit var mockOnStopClick: (LibraryDownloadItem) -> Unit

  @Before
  fun setUp() {
    mockOnPauseResumeClick = mockk(relaxed = true)
    mockOnStopClick = mockk(relaxed = true)

    mockkObject(CoreApp.Companion)
    mockkStatic(ContextCompat::class)
    every { CoreApp.instance } returns mockk(relaxed = true)
    every { ContextCompat.getContextForLanguage(any()) } returns context
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  private fun mockLibraryDownloadItem(
    downloadId: Long = 1L,
    favIconUrl: String = "https://kiwix.org/favicon.png",
    title: String = "Wikipedia",
    description: String? = "The free encyclopedia",
    bytesDownloaded: Long = 512L,
    totalSizeBytes: Long = 1024L,
    progress: Int = 50,
    eta: Seconds = Seconds(0L),
    downloadState: DownloadState = DownloadState.Running,
    id: Long = 1L,
    currentDownloadState: Status = Status.DOWNLOADING,
    downloadError: Error = Error.NONE
  ) = LibraryDownloadItem(
    downloadId = downloadId,
    favIconUrl = favIconUrl,
    title = title,
    description = description,
    bytesDownloaded = bytesDownloaded,
    totalSizeBytes = totalSizeBytes,
    progress = progress,
    eta = eta,
    downloadState = downloadState,
    id = id,
    currentDownloadState = currentDownloadState,
    downloadError = downloadError
  )

  private fun setContent(
    item: LibraryDownloadItem = mockLibraryDownloadItem(),
    onPauseResumeClick: (LibraryDownloadItem) -> Unit = mockOnPauseResumeClick,
    onStopClick: (LibraryDownloadItem) -> Unit = mockOnStopClick
  ) {
    composeTestRule.setContent {
      DownloadBookItem(
        index = 0,
        item = item,
        onPauseResumeClick = onPauseResumeClick,
        onStopClick = onStopClick
      )
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun downloadBookItem_whenRendered_cardIsDisplayed() {
    setContent()
    composeTestRule
      .onNodeWithTag(DOWNLOAD_BOOK_ITEM_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun downloadBookItem_bookTitleIsDisplayed() {
    setContent(item = mockLibraryDownloadItem(title = "Wikipedia"))
    composeTestRule
      .onNode(hasContentDescription("Wikipedia$testIndex"))
      .assertIsDisplayed()
  }

  @Test
  fun downloadBookItem_bookDescriptionIsDisplayed() {
    setContent(item = mockLibraryDownloadItem(description = "The free encyclopedia"))
    composeTestRule
      .onNode(hasContentDescription("The free encyclopedia$testIndex"))
      .assertIsDisplayed()
  }

  @Test
  fun downloadBookItem_whenDescriptionNull_orEmptyBranchRendersWithoutCrash() {
    setContent(item = mockLibraryDownloadItem(description = null))
    composeTestRule
      .onNodeWithTag(DOWNLOAD_BOOK_ITEM_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun downloadBookItem_downloadStateTextTagIsDisplayed() {
    setContent()
    composeTestRule
      .onNodeWithTag(DOWNLOADING_STATE_TEXT_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun downloadBookItem_whenRunningAndEtaPositive_etaStringIsShownAsStateText() {
    val item = mockLibraryDownloadItem(
      downloadState = DownloadState.Running,
      eta = Seconds(90L)
    )
    setContent(item = item)
    val expectedDesc = item.readableEta.toString() + item.hashCode()
    composeTestRule
      .onNode(hasContentDescription(expectedDesc))
      .assertIsDisplayed()
  }

  @Test
  fun downloadBookItem_whenRunningAndEtaIsZero_emptyEtaShownAsStateText() {
    val item = mockLibraryDownloadItem(downloadState = DownloadState.Running, eta = Seconds(0L))
    setContent(item = item)
    composeTestRule
      .onNodeWithTag(DOWNLOADING_STATE_TEXT_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun downloadBookItem_whenStatePending_pendingTextIsShown() {
    val item = mockLibraryDownloadItem(
      downloadState = DownloadState.Pending,
      currentDownloadState = Status.QUEUED
    )
    setContent(item = item)
    val expectedDesc = DownloadState.Pending.toReadableState(context).toString() + item.hashCode()
    composeTestRule
      .onNode(hasContentDescription(expectedDesc))
      .assertIsDisplayed()
  }

  @Test
  fun downloadBookItem_whenStatePaused_pausedTextIsShown() {
    val item = mockLibraryDownloadItem(
      downloadState = DownloadState.Paused,
      currentDownloadState = Status.PAUSED
    )
    setContent(item = item)
    val expectedDesc = DownloadState.Paused.toReadableState(context).toString() + item.hashCode()
    composeTestRule
      .onNode(hasContentDescription(expectedDesc))
      .assertIsDisplayed()
  }

  @Test
  fun downloadBookItem_whenStateSuccessful_completeTextIsShown() {
    val item = mockLibraryDownloadItem(
      downloadState = DownloadState.Successful,
      currentDownloadState = Status.COMPLETED
    )
    setContent(item = item)
    val expectedDesc =
      DownloadState.Successful.toReadableState(context).toString() + item.hashCode()
    composeTestRule
      .onNode(hasContentDescription(expectedDesc))
      .assertIsDisplayed()
  }

  @Test
  fun downloadBookItem_whenStateFailed_failedTextIsShown() {
    val item = mockLibraryDownloadItem(
      downloadState = DownloadState.Failed(reason = Error.UNKNOWN, zimUrl = null),
      currentDownloadState = Status.FAILED,
      downloadError = Error.NONE
    )
    setContent(item = item)
    val expectedDesc =
      DownloadState.Failed(Error.UNKNOWN, null).toReadableState(context).toString() +
        item.hashCode()
    composeTestRule
      .onNode(hasContentDescription(expectedDesc))
      .assertIsDisplayed()
  }

  @Test
  fun downloadBookItem_whenTotalSizeBytesIsZero_sizeTextIsEmpty() {
    setContent(item = mockLibraryDownloadItem(bytesDownloaded = 512L, totalSizeBytes = 0L))
    composeTestRule
      .onNodeWithTag(DOWNLOAD_BOOK_ITEM_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun downloadBookItem_whenBytesDownloadedIsZero_sizeTextIsEmpty() {
    setContent(item = mockLibraryDownloadItem(bytesDownloaded = 0L, totalSizeBytes = 1024L))
    composeTestRule
      .onNodeWithTag(DOWNLOAD_BOOK_ITEM_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun downloadBookItem_whenBothBytesPositive_sizeTextWithSlashIsDisplayed() {
    val downloaded = 1024L
    val total = 2048L
    setContent(item = mockLibraryDownloadItem(bytesDownloaded = downloaded, totalSizeBytes = total))
    val expectedText =
      Byte(downloaded.toString()).humanReadable + "/" + Byte(total.toString()).humanReadable
    composeTestRule
      .onNode(hasText(expectedText))
      .assertIsDisplayed()
  }

  @Test
  fun downloadBookItem_whenDownloadPaused_pauseResumeButtonIsDisplayed() {
    setContent(item = mockLibraryDownloadItem(downloadState = DownloadState.Paused))
    composeTestRule
      .onNode(hasTestTag(DOWNLOADING_PAUSE_BUTTON_TESTING_TAG))
      .assertIsDisplayed()
  }

  @Test
  fun downloadBookItem_whenDownloadRunning_pauseResumeButtonIsDisplayed() {
    setContent(item = mockLibraryDownloadItem(downloadState = DownloadState.Running))
    composeTestRule
      .onNode(hasTestTag(DOWNLOADING_PAUSE_BUTTON_TESTING_TAG))
      .assertIsDisplayed()
  }

  @Test
  fun downloadBookItem_whenPauseResumeButtonClicked_callbackIsInvoked() {
    val item = mockLibraryDownloadItem()
    setContent(item = item)
    composeTestRule
      .onNode(hasTestTag(DOWNLOADING_PAUSE_BUTTON_TESTING_TAG))
      .performClick()
    verify(exactly = 1) { mockOnPauseResumeClick.invoke(item) }
  }

  @Test
  fun downloadBookItem_stopButtonIsDisplayed() {
    setContent()
    composeTestRule
      .onNode(hasTestTag(DOWNLOADING_STOP_BUTTON_TESTING_TAG))
      .assertIsDisplayed()
  }

  @Test
  fun downloadBookItem_whenStopButtonClicked_callbackIsInvoked() {
    val item = mockLibraryDownloadItem(currentDownloadState = Status.DOWNLOADING)
    setContent(item = item)
    composeTestRule
      .onNode(hasTestTag(DOWNLOADING_STOP_BUTTON_TESTING_TAG))
      .performClick()
    verify(exactly = 1) { mockOnStopClick.invoke(item) }
  }

  @Test
  fun downloadBookItem_whenStatusNotFailed_onStopClickNotCalledByLaunchedEffect() {
    setContent(item = mockLibraryDownloadItem(currentDownloadState = Status.DOWNLOADING))
    verify(exactly = 0) { mockOnStopClick.invoke(any()) }
  }

  @Test
  fun downloadBookItem_whenFailedWithUnknownIoError_onStopClickCalledByLaunchedEffect() {
    setContent(
      item = mockLibraryDownloadItem(
        currentDownloadState = Status.FAILED,
        downloadState = DownloadState.Failed(Error.UNKNOWN_IO_ERROR, null),
        downloadError = Error.UNKNOWN_IO_ERROR
      )
    )
    verify(exactly = 1) { mockOnStopClick.invoke(any()) }
  }

  @Test
  fun downloadBookItem_whenFailedWithConnectionTimedOut_onStopClickCalledByLaunchedEffect() {
    setContent(
      item = mockLibraryDownloadItem(
        currentDownloadState = Status.FAILED,
        downloadState = DownloadState.Failed(Error.CONNECTION_TIMED_OUT, null),
        downloadError = Error.CONNECTION_TIMED_OUT
      )
    )
    verify(exactly = 1) { mockOnStopClick.invoke(any()) }
  }

  @Test
  fun downloadBookItem_whenFailedWithUnknown_onStopClickCalledByLaunchedEffect() {
    setContent(
      item = mockLibraryDownloadItem(
        currentDownloadState = Status.FAILED,
        downloadState = DownloadState.Failed(Error.UNKNOWN, null),
        downloadError = Error.UNKNOWN
      )
    )
    verify(exactly = 1) { mockOnStopClick.invoke(any()) }
  }

  @Test
  fun downloadBookItem_whenFailedWithOtherError_onStopClickNotCalledByLaunchedEffect() {
    setContent(
      item = mockLibraryDownloadItem(
        currentDownloadState = Status.FAILED,
        downloadState = DownloadState.Failed(Error.NONE, null),
        downloadError = Error.NONE
      )
    )
    verify(exactly = 0) { mockOnStopClick.invoke(any()) }
  }

  @Test
  fun downloadBookItem_bookIconIsDisplayed() {
    val favicon = "https://kiwix.org/favicon.png"
    setContent(item = mockLibraryDownloadItem(favIconUrl = favicon))
    val expectedDesc =
      context.getString(R.string.fav_icon) + favicon.hashCode()
    composeTestRule
      .onNode(hasContentDescription(expectedDesc))
      .assertIsDisplayed()
  }
}
