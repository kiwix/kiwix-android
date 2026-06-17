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

import app.cash.turbine.test
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.effects.RequestNotificationPermission
import org.kiwix.kiwixmobile.custom.download.BrandedDownloadViewModel
import org.kiwix.kiwixmobile.custom.download.State.DownloadComplete
import org.kiwix.kiwixmobile.custom.download.State.DownloadFailed
import org.kiwix.kiwixmobile.custom.download.State.DownloadInProgress
import org.kiwix.kiwixmobile.custom.download.State.DownloadRequired
import org.kiwix.kiwixmobile.custom.download.effects.DownloadBranded
import org.kiwix.kiwixmobile.custom.download.effects.NavigateToBrandedReader
import org.kiwix.kiwixmobile.custom.download.effects.SetPreferredStorageWithMostSpace
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
internal class BrandedDownloadViewModelTest {
  private val downloadRoomDao: DownloadRoomDao = mockk()
  private val setPreferredStorageWithMostSpace: SetPreferredStorageWithMostSpace = mockk()
  private val downloadBranded: DownloadBranded = mockk()
  private val navigateToBrandedReader: NavigateToBrandedReader = mockk()

  private val kiwixPermissionChecker: KiwixPermissionChecker = mockk()
  private val requestNotificationPermission: RequestNotificationPermission = mockk()

  @RegisterExtension
  @JvmField
  val dispatcherRule = MainDispatcherRule()
  private lateinit var brandedDownloadViewModel: BrandedDownloadViewModel
  private lateinit var downloadsFlow: MutableSharedFlow<List<DownloadModel>>

  private fun createDownloadItem(
    downloadState: Status,
    error: Error = Error.NONE,
    downloadId: Long = 1,
    progress: Int = 10
  ) = DownloadModel(
    databaseId = 0L,
    downloadId = downloadId,
    file = null,
    etaInMilliSeconds = 0L,
    bytesDownloaded = 100,
    totalSizeOfDownload = 1000,
    progress = progress,
    state = downloadState,
    error = error,
    book = LibkiwixBook()
  )

  private fun createViewModel() {
    brandedDownloadViewModel = BrandedDownloadViewModel(
      downloadRoomDao,
      setPreferredStorageWithMostSpace,
      downloadBranded,
      navigateToBrandedReader,
      kiwixPermissionChecker,
      requestNotificationPermission,
      dispatcherRule.dispatcher
    )
  }

  @BeforeEach
  fun setUp() {
    downloadsFlow = MutableSharedFlow()
    every { downloadRoomDao.downloads() } returns downloadsFlow
    every { kiwixPermissionChecker.isAndroid13orAbove() } returns true
    createViewModel()
  }

  @Nested
  inner class Initial {
    @Test
    fun initialState_isDownloadRequired() {
      assertEquals(
        DownloadRequired,
        brandedDownloadViewModel.state.value
      )
    }

    @Test
    fun effects_whenViewModelStarts_emitsStorageEffect() = runTest {
      brandedDownloadViewModel.effects.test {
        val effect = awaitItem()
        assertEquals(setPreferredStorageWithMostSpace, effect)
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Nested
  inner class KiwixPermissionCheckerTest {
    @Test
    fun isAndroid13OrAbove_returnsValueFromPermissionChecker() {
      every { kiwixPermissionChecker.isAndroid13orAbove() } returns false
      createViewModel()
      assertEquals(false, brandedDownloadViewModel.isAndroid13OrAbove)
    }

    @Test
    fun isAndroid13OrAbove_returnsTrueWhenAndroid13OrAbove() {
      every { kiwixPermissionChecker.isAndroid13orAbove() } returns true
      createViewModel()
      assertEquals(true, brandedDownloadViewModel.isAndroid13OrAbove)
    }
  }

  @Nested
  inner class OnNotificationPermissionResult {
    @Test
    fun granted_emitsDownloadEffect() = runTest {
      brandedDownloadViewModel.effects.test {
        skipItems(1)

        brandedDownloadViewModel.onNotificationPermissionResult(true)
        advanceUntilIdle()

        assertEquals(downloadBranded, awaitItem())
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun denied_doesNotEmitEffect() = runTest {
      brandedDownloadViewModel.effects.test {
        skipItems(1)

        brandedDownloadViewModel.onNotificationPermissionResult(false)
        advanceUntilIdle()

        expectNoEvents()
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Nested
  inner class OnDownloadButtonClick {
    @Test
    fun withPermission_emitsDownloadEffect() = runTest {
      coEvery { kiwixPermissionChecker.hasNotificationPermission() } returns true

      brandedDownloadViewModel.effects.test {
        skipItems(1)
        brandedDownloadViewModel.onDownloadButtonClick()
        advanceUntilIdle()
        assertEquals(downloadBranded, awaitItem())
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun withoutPermission_emitsRequestNotificationPermissionEffect() = runTest {
      coEvery { kiwixPermissionChecker.hasNotificationPermission() } returns false

      brandedDownloadViewModel.effects.test {
        skipItems(1)
        brandedDownloadViewModel.onDownloadButtonClick()
        advanceUntilIdle()
        assertEquals(requestNotificationPermission, awaitItem())
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun permissionGrantedAfterRequest_startsDownload() = runTest {
      coEvery { kiwixPermissionChecker.hasNotificationPermission() } returns false

      brandedDownloadViewModel.effects.test {
        skipItems(1)

        brandedDownloadViewModel.onDownloadButtonClick()
        assertEquals(requestNotificationPermission, awaitItem())

        brandedDownloadViewModel.onNotificationPermissionResult(true)
        advanceUntilIdle()
        assertEquals(downloadBranded, awaitItem())
      }
    }
  }

  @Nested
  inner class OnRetryButtonClick {
    @Test
    fun withPermission_emitsDownloadEffect() = runTest {
      coEvery { kiwixPermissionChecker.hasNotificationPermission() } returns true

      brandedDownloadViewModel.effects.test {
        skipItems(1)
        brandedDownloadViewModel.onRetryButtonClick()
        advanceUntilIdle()
        assertEquals(downloadBranded, awaitItem())
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun withoutPermission_emitsRequestNotificationPermissionEffect() = runTest {
      coEvery { kiwixPermissionChecker.hasNotificationPermission() } returns false

      brandedDownloadViewModel.effects.test {
        skipItems(1)
        brandedDownloadViewModel.onRetryButtonClick()
        advanceUntilIdle()
        assertEquals(requestNotificationPermission, awaitItem())
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Nested
  inner class Reduce {
    @Nested
    inner class DatabaseEmission {
      @Nested
      inner class DownloadRequired {
        @Test
        fun nonEmptyDownloads_movesToDownloadInProgress() = runTest {
          val item = createDownloadItem(Status.DOWNLOADING)
          downloadsFlow.emit(listOf(item))
          advanceUntilIdle()
          assertEquals(
            DownloadInProgress(listOf(DownloadItem(item))),
            brandedDownloadViewModel.state.value
          )
        }

        @Test
        fun emptyDownloads_staysDownloadRequired() = runTest {
          downloadsFlow.emit(emptyList())
          advanceUntilIdle()
          assertEquals(DownloadRequired, brandedDownloadViewModel.state.value)
        }
      }

      @Nested
      inner class DownloadFailed {
        @Test
        fun newDownloadAfterFailure_movesToDownloadInProgress() = runTest {
          // Move to DownloadInProgress
          val runningDownload = createDownloadItem(Status.DOWNLOADING)

          downloadsFlow.emit(listOf(runningDownload))
          advanceUntilIdle()

          // Move to DownloadFailed
          val failedDownload =
            createDownloadItem(downloadState = Status.FAILED, error = Error.HTTP_NOT_FOUND)

          downloadsFlow.emit(listOf(failedDownload))
          advanceUntilIdle()

          val expectedFailure =
            DownloadFailed(DownloadState.Failed(Error.HTTP_NOT_FOUND, null))

          assertEquals(expectedFailure, brandedDownloadViewModel.state.value)

          // New download starts
          val restartedDownload =
            createDownloadItem(downloadState = Status.DOWNLOADING, downloadId = 2)

          downloadsFlow.emit(listOf(restartedDownload))
          advanceUntilIdle()

          assertEquals(
            DownloadInProgress(listOf(DownloadItem(restartedDownload))),
            brandedDownloadViewModel.state.value
          )
        }
      }

      @Nested
      inner class DownloadInProgress {
        @Test
        fun failedDownload_movesToDownloadFailed() = runTest {
          val runningDownload = createDownloadItem(Status.DOWNLOADING)
          downloadsFlow.emit(listOf(runningDownload))
          advanceUntilIdle()

          assertEquals(
            DownloadInProgress(listOf(DownloadItem(runningDownload))),
            brandedDownloadViewModel.state.value
          )

          val failedDownload =
            createDownloadItem(error = Error.UNKNOWN, downloadState = Status.FAILED)
          downloadsFlow.emit(listOf(failedDownload))

          advanceUntilIdle()

          assertEquals(
            DownloadFailed(DownloadState.Failed(Error.UNKNOWN, null)),
            brandedDownloadViewModel.state.value
          )
        }

        @Test
        fun activeDownload_updatesDownloadInProgressState() = runTest {
          val runningDownload =
            createDownloadItem(downloadState = Status.DOWNLOADING, downloadId = 1)
          downloadsFlow.emit(listOf(runningDownload))
          advanceUntilIdle()

          assertEquals(
            DownloadInProgress(listOf(DownloadItem(runningDownload))),
            brandedDownloadViewModel.state.value
          )

          val updatedDownload =
            createDownloadItem(downloadState = Status.DOWNLOADING, downloadId = 2, progress = 50)

          downloadsFlow.emit(listOf(updatedDownload))
          advanceUntilIdle()

          assertEquals(
            DownloadInProgress(listOf(DownloadItem(updatedDownload))),
            brandedDownloadViewModel.state.value
          )
        }

        @Test
        fun emptyDownloads_movesToDownloadComplete() = runTest {
          val running = createDownloadItem(Status.DOWNLOADING)
          downloadsFlow.emit(listOf(running))
          advanceUntilIdle()
          downloadsFlow.emit(emptyList())
          advanceUntilIdle()
          assertEquals(DownloadComplete, brandedDownloadViewModel.state.value)
        }

        @Test
        fun emptyDownloads_emitsNavigateToBrandedReader() = runTest {
          val runningDownload = createDownloadItem(Status.DOWNLOADING)

          brandedDownloadViewModel.effects.test {
            skipItems(1)

            downloadsFlow.emit(listOf(runningDownload))
            advanceUntilIdle()

            downloadsFlow.emit(emptyList())
            advanceUntilIdle()

            assertEquals(navigateToBrandedReader, awaitItem())
            cancelAndIgnoreRemainingEvents()
          }
        }
      }

      @Nested
      inner class DownloadComplete {
        @Test
        fun afterCompletion_futureDatabaseUpdates_areIgnored() = runTest {
          val running =
            createDownloadItem(Status.DOWNLOADING)

          downloadsFlow.emit(listOf(running))
          advanceUntilIdle()
          downloadsFlow.emit(emptyList())
          advanceUntilIdle()
          assertEquals(DownloadComplete, brandedDownloadViewModel.state.value)
          val anotherDownload = createDownloadItem(Status.DOWNLOADING)

          downloadsFlow.emit(listOf(anotherDownload))
          advanceUntilIdle()
          assertEquals(DownloadComplete, brandedDownloadViewModel.state.value)
        }
      }
    }
  }
}
