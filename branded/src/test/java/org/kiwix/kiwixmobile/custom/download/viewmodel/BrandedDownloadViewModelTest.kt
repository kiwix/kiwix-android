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
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.downloader.model.Seconds
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.custom.download.Action.ClickedDownload
import org.kiwix.kiwixmobile.custom.download.Action.ClickedRetry
import org.kiwix.kiwixmobile.custom.download.Action.DatabaseEmission
import org.kiwix.kiwixmobile.custom.download.BrandedDownloadViewModel
import org.kiwix.kiwixmobile.custom.download.State.DownloadComplete
import org.kiwix.kiwixmobile.custom.download.State.DownloadFailed
import org.kiwix.kiwixmobile.custom.download.State.DownloadInProgress
import org.kiwix.kiwixmobile.custom.download.State.DownloadRequired
import org.kiwix.kiwixmobile.custom.download.effects.DownloadBranded
import org.kiwix.kiwixmobile.custom.download.effects.NavigateToBrandedReader
import org.kiwix.kiwixmobile.core.utils.effects.RequestNotificationPermission
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
  val dispatcherRule = MainDispatcherRule()
  private lateinit var brandedDownloadViewModel: BrandedDownloadViewModel

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

  @BeforeEach
  fun setUp() {
    every { downloadRoomDao.downloads() } returns MutableSharedFlow()
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

  @Test
  internal fun effects_whenViewModelStarts_emitsStorageEffect() = runTest {
    brandedDownloadViewModel.effects.test {
      val effect = awaitItem()
      assertEquals(setPreferredStorageWithMostSpace, effect)
      cancelAndIgnoreRemainingEvents()
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
    fun withPermission_emitsClickedDownloadAction() = runTest {
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
  }

  @Nested
  inner class OnRetryButtonClick {
    @Test
    fun withPermission_emitsClickedRetryAction() = runTest {
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
          brandedDownloadViewModel.getStateForTesting().value = DownloadRequired

          val item = createDownloadItem(DownloadState.Running)

          brandedDownloadViewModel.actions.emit(DatabaseEmission(listOf(item)))

          advanceUntilIdle()

          assertEquals(DownloadInProgress(listOf(item)), brandedDownloadViewModel.state.value)
        }

        @Test
        fun emptyDownloads_staysDownloadRequired() = runTest {
          brandedDownloadViewModel.getStateForTesting().value = DownloadRequired

          brandedDownloadViewModel.actions.emit(DatabaseEmission(emptyList()))

          advanceUntilIdle()

          assertEquals(DownloadRequired, brandedDownloadViewModel.state.value)
        }
      }

      @Nested
      inner class DownloadFailed {
        @Test
        fun nonEmptyDownloads_movesToDownloadInProgress() = runTest {
          val failed = DownloadState.Failed(Error.HTTP_NOT_FOUND, null)

          brandedDownloadViewModel.getStateForTesting().value = DownloadFailed(failed)

          val item = createDownloadItem(DownloadState.Running)

          brandedDownloadViewModel.actions.emit(DatabaseEmission(listOf(item)))

          advanceUntilIdle()

          assertEquals(
            DownloadInProgress(listOf(item)),
            brandedDownloadViewModel.state.value
          )
        }

        @Test
        fun emptyDownloads_staysDownloadFailed() = runTest {
          val failed = DownloadState.Failed(Error.UNKNOWN, null)

          brandedDownloadViewModel.getStateForTesting().value = DownloadFailed(failed)

          brandedDownloadViewModel.actions.emit(DatabaseEmission(emptyList()))

          advanceUntilIdle()

          assertEquals(
            DownloadFailed(failed),
            brandedDownloadViewModel.state.value
          )
        }
      }

      @Nested
      inner class DownloadInProgress {
        @Test
        fun failedDownload_movesToDownloadFailed() = runTest {
          val runningDownload = createDownloadItem(DownloadState.Running)

          brandedDownloadViewModel.getStateForTesting().value =
            DownloadInProgress(listOf(runningDownload))

          val failedDownload = createDownloadItem(DownloadState.Failed(Error.UNKNOWN, null))

          brandedDownloadViewModel.actions.emit(DatabaseEmission(listOf(failedDownload)))

          advanceUntilIdle()

          assertEquals(
            DownloadFailed(failedDownload.downloadState),
            brandedDownloadViewModel.state.value
          )
        }

        @Test
        fun activeDownload_staysDownloadInProgress() = runTest {
          val running = createDownloadItem(DownloadState.Running, downloadId = 1)

          brandedDownloadViewModel.getStateForTesting().value = DownloadInProgress(listOf(running))

          val updatedRunning = createDownloadItem(DownloadState.Running, downloadId = 2)

          brandedDownloadViewModel.actions.emit(DatabaseEmission(listOf(updatedRunning)))

          advanceUntilIdle()

          assertEquals(
            DownloadInProgress(listOf(updatedRunning)),
            brandedDownloadViewModel.state.value
          )
        }

        @Test
        fun emptyDownloads_movesToDownloadComplete() = runTest {
          val running = createDownloadItem(DownloadState.Running)
          brandedDownloadViewModel.getStateForTesting().value = DownloadInProgress(listOf(running))

          brandedDownloadViewModel.actions.emit(DatabaseEmission(emptyList()))
          advanceUntilIdle()

          assertEquals(DownloadComplete, brandedDownloadViewModel.state.value)
        }

        @Test
        fun emptyDownloads_emitsNavigateToBrandedReader() = runTest {
          val running = createDownloadItem(DownloadState.Running)
          brandedDownloadViewModel.getStateForTesting().value = DownloadInProgress(listOf(running))

          brandedDownloadViewModel.effects.test {
            skipItems(1)
            brandedDownloadViewModel.actions.emit(DatabaseEmission(emptyList()))
            advanceUntilIdle()
            assertEquals(navigateToBrandedReader, awaitItem())
            cancelAndIgnoreRemainingEvents()
          }
        }
      }

      @Nested
      inner class DownloadComplete {
        @Test
        fun databaseEmission_staysDownloadComplete() = runTest {
          brandedDownloadViewModel.getStateForTesting().value = DownloadComplete

          brandedDownloadViewModel.actions.emit(DatabaseEmission(emptyList()))

          advanceUntilIdle()

          assertEquals(
            DownloadComplete,
            brandedDownloadViewModel.state.value
          )
        }
      }
    }

    @Nested
    inner class ClickedRetry {
      @Test
      fun emitsDownloadEffect() = runTest {
        brandedDownloadViewModel.effects.test {
          skipItems(1)

          brandedDownloadViewModel.actions.emit(ClickedRetry)

          val effect = awaitItem()

          assertEquals(downloadBranded, effect)
          cancelAndIgnoreRemainingEvents()
        }
      }
    }

    @Nested
    inner class ClickDownload {
      @Test
      fun emitsDownloadEffect() = runTest {
        brandedDownloadViewModel.effects.test {
          skipItems(1)

          brandedDownloadViewModel.actions.emit(ClickedDownload)

          advanceUntilIdle()
          val effect = awaitItem()
          assertEquals(downloadBranded, effect)
          cancelAndIgnoreRemainingEvents()
        }
      }
    }
  }
}
