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

package org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Application
import android.net.ConnectivityManager
import android.os.Build
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ObserveNetworkState
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ObserveOnlineLibrary
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ObserveOnlineLibraryItems
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.CancelDownload
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.DisableStorageSelection
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.NoInternet
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.NotEnoughSpace
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.PauseResume
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.RequestManageExternalFilesPermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.RequestNotificationPermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.RequestStoragePermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.RetryDownload
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.ShowWifiOnlyDialog
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.StartDownload
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction.Result.NoInternetWithContent
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction.Result.NoInternetWithEmptyContent
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction.Result.Proceed
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction.Result.WifiOnlyBlocked
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Idle
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Loading
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.NoInternetConnection
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Parsing
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Success
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.WifiOnlyException
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.NavigateToAppSettings
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.NavigateToSettings
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.RequestPermission
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.ShowDialog
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.ShowNoSpaceSnackbar
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.ShowSnackbar
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(InstantExecutorExtension::class)
class OnlineLibraryViewModelTest {
  @RegisterExtension
  val dispatcherRule = MainDispatcherRule()
  private val downloader: Downloader = mockk(relaxed = true)
  private val kiwixDataStore: KiwixDataStore = mockk(relaxed = true)
  private val bookUtils: BookUtils = mockk(relaxed = true)
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk = mockk(relaxed = true)
  private val downloadDao: DownloadRoomDao = mockk(relaxed = true)
  private val availableSpaceCalculator: AvailableSpaceCalculator = mockk(relaxed = true)
  private val permissionChecker: KiwixPermissionChecker = mockk(relaxed = true)
  private val context: Application = mockk(relaxed = true)
  private val connectivityReceiver: ConnectivityBroadcastReceiver = mockk(relaxed = true)
  private val connectivityManager: ConnectivityManager = mockk(relaxed = true)
  private val observeItems: ObserveOnlineLibraryItems = mockk(relaxed = true)
  private val resolveClick: ResolveBookClickAction = mockk(relaxed = true)
  private val observeLibrary: ObserveOnlineLibrary = mockk(relaxed = true)
  private val refreshAction: ResolveRefreshLibraryAction = mockk(relaxed = true)
  private val observeNetwork: ObserveNetworkState = mockk(relaxed = true)
  private lateinit var viewModel: OnlineLibraryViewModel

  @BeforeEach
  fun setup() {
    every { connectivityReceiver.networkStates } returns MutableStateFlow(NetworkState.NOT_CONNECTED)
    every { observeItems.invoke(any(), any(), any(), any(), any()) } returns emptyFlow()
    every { observeLibrary.invoke(any(), any()) } returns flowOf(mockk(relaxed = true))
    every { observeNetwork.invoke(any()) } returns emptyFlow()
    every { kiwixDataStore.selectedOnlineContentCategory } returns MutableStateFlow("")
    every { kiwixDataStore.selectedOnlineContentLanguage } returns MutableStateFlow("")
    every { permissionChecker.isAndroid13orAbove() } returns true
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      every { context.registerReceiver(any(), any(), any()) } returns mockk()
    } else {
      @Suppress("UnspecifiedRegisterReceiverFlag")
      every { context.registerReceiver(any(), any()) } returns mockk()
    }
    viewModel = OnlineLibraryViewModel(
      downloader,
      kiwixDataStore,
      bookUtils,
      libkiwixBookOnDisk,
      downloadDao,
      availableSpaceCalculator,
      permissionChecker,
      context,
      connectivityReceiver,
      connectivityManager,
      observeItems,
      resolveClick,
      observeLibrary,
      refreshAction,
      observeNetwork,
      dispatcherRule.dispatcher
    )
    viewModel.networkBooks.tryEmit(emptyList())
  }

  @AfterEach
  fun dispose() {
    viewModel.onClearedExposed()
  }

  @Nested
  inner class Context {
    @Test
    fun `registers broadcastReceiver in init`() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        verify {
          context.registerReceiver(connectivityReceiver, any(), any())
        }
      } else {
        @Suppress("UnspecifiedRegisterReceiverFlag")
        verify {
          context.registerReceiver(connectivityReceiver, any())
        }
      }
    }

    @Test
    fun `unregisters broadcastReceiver in onCleared`() {
      every { context.unregisterReceiver(any()) } returns mockk()
      viewModel.onClearedExposed()
      verify {
        context.unregisterReceiver(connectivityReceiver)
      }
    }
  }

  @Nested
  inner class RefreshScreen {
    @Test
    fun `given proceed action when refreshScreen then show scanning`() = runTest {
      coEvery { refreshAction.invoke(any()) } returns Proceed

      viewModel.refreshScreen(isExplicitRefresh = true)

      advanceUntilIdle()

      assertTrue(viewModel.uiState.value.showScanningProgressBar)
    }

    @Test
    fun `given no internet with content when refreshScreen then emit snackbar`() = runTest {
      coEvery { refreshAction.invoke(any()) } returns NoInternetWithContent
      viewModel.uiEvents.test {
        viewModel.refreshScreen(true)
        advanceUntilIdle()

        assertTrue(awaitItem() is ShowSnackbar)
      }
    }

    @Test
    fun `given wifi only restriction when refreshScreen then show wifi dialog`() = runTest {
      coEvery { refreshAction.invoke(any()) } returns WifiOnlyBlocked
      viewModel.uiEvents.test {
        viewModel.refreshScreen(true)
        advanceUntilIdle()
        val dialog = awaitItem() as ShowDialog
        assertTrue(dialog.dialog is KiwixDialog.YesNoDialog.WifiOnly)
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `given no internet and empty content when refreshScreen then show no content`() = runTest {
      coEvery { refreshAction.invoke(any()) } returns NoInternetWithEmptyContent

      viewModel.refreshScreen(true)
      advanceUntilIdle()

      assertTrue(viewModel.uiState.value.showNoContent)
    }
  }

  @Nested
  inner class OnBookItemClick {
    @Test
    fun `when action is StartDownload then triggers download`() = runTest {
      val item = mockk<LibraryListItem.BookItem>(relaxed = true)
      val activity = mockk<KiwixMainActivity>(relaxed = true)

      coEvery { resolveClick.onBookItemClick(item, any()) } returns
        StartDownload(item)

      viewModel.onBookItemClick(item, activity)

      advanceUntilIdle()

      verify { downloader.download(item.book) }
    }

    @Test
    fun `when action is NoInternet then emits snackbar`() = runTest {
      val item = mockk<LibraryListItem.BookItem>(relaxed = true)
      val activity = mockk<KiwixMainActivity>(relaxed = true)

      coEvery { resolveClick.onBookItemClick(any(), any()) } returns
        NoInternet
      viewModel.uiEvents.test {
        viewModel.onBookItemClick(item, activity)

        advanceUntilIdle()
        val snackBar = awaitItem() as ShowSnackbar
        assertTrue(snackBar.message == context.getString(R.string.no_network_connection))

        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `when action is RequestStoragePermission then emits permission event`() = runTest {
      val item = mockk<LibraryListItem.BookItem>(relaxed = true)
      val activity = mockk<KiwixMainActivity>(relaxed = true)

      coEvery { resolveClick.onBookItemClick(any(), any()) } returns RequestStoragePermission

      viewModel.uiEvents.test {
        viewModel.onBookItemClick(item, activity)
        advanceUntilIdle()
        val permission = awaitItem() as RequestPermission
        assertTrue(permission.permission == WRITE_EXTERNAL_STORAGE)

        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `when action is RequestNotificationPermission then emits permission event`() = runTest {
      val item = mockk<LibraryListItem.BookItem>(relaxed = true)
      val activity = mockk<KiwixMainActivity>(relaxed = true)
      coEvery { resolveClick.onBookItemClick(any(), any()) } returns RequestNotificationPermission

      viewModel.uiEvents.test {
        viewModel.onBookItemClick(item, activity)
        advanceUntilIdle()
        val permission = awaitItem() as RequestPermission
        assertTrue(permission.permission == POST_NOTIFICATIONS)

        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `when action is RequestManageExternalFilesPermission then emits permission event`() =
      runTest {
        val item = mockk<LibraryListItem.BookItem>(relaxed = true)
        val activity = mockk<KiwixMainActivity>(relaxed = true)
        coEvery {
          resolveClick.onBookItemClick(
            any(),
            any()
          )
        } returns RequestManageExternalFilesPermission

        viewModel.uiEvents.test {
          viewModel.onBookItemClick(item, activity)
          advanceUntilIdle()
          val dialog = awaitItem() as ShowDialog
          assertTrue(dialog.dialog == KiwixDialog.ManageExternalFilesPermissionDialog)
          dialog.positiveAction.invoke()
          assertTrue(awaitItem() is NavigateToSettings)
          cancelAndIgnoreRemainingEvents()
        }
      }

    @Test
    fun `when action is ShowWifiOnlyDialog then shows dialog`() = runTest {
      val item = mockk<LibraryListItem.BookItem>(relaxed = true)
      val activity = mockk<KiwixMainActivity>(relaxed = true)

      coEvery { resolveClick.onBookItemClick(any(), any()) } returns ShowWifiOnlyDialog

      viewModel.uiEvents.test {
        viewModel.onBookItemClick(item, activity)
        advanceUntilIdle()

        val dialog = awaitItem() as ShowDialog
        assertTrue(dialog.dialog is KiwixDialog.YesNoDialog.WifiOnly)
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `when action is NotEnoughSpace then shows no space snackbar`() = runTest {
      val item = mockk<LibraryListItem.BookItem>(relaxed = true)
      val activity = mockk<KiwixMainActivity>(relaxed = true)

      coEvery { resolveClick.onBookItemClick(any(), any()) } returns NotEnoughSpace("100MB")

      viewModel.uiEvents.test {
        viewModel.onBookItemClick(item, activity)
        advanceUntilIdle()
        val snackBar = awaitItem() as ShowNoSpaceSnackbar
        assertTrue(snackBar.message.contains("100MB"))
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `when action is DisableStorageSelection then it sets the showStorageOption`() = runTest {
      val item = mockk<LibraryListItem.BookItem>(relaxed = true)
      val activity = mockk<KiwixMainActivity>(relaxed = true)

      coEvery { resolveClick.onBookItemClick(any(), any()) } returnsMany listOf(
        DisableStorageSelection,
        StartDownload(item)
      )
      viewModel.onBookItemClick(item, activity)
      advanceUntilIdle()
      coVerify { kiwixDataStore.setShowStorageOption(false) }
      verify { downloader.download(item.book) }
    }
  }

  @Nested
  inner class OnPauseResumeButtonClick {
    @Test
    fun `when action is PauseResume then triggers downloader`() {
      val item = mockk<LibraryListItem.LibraryDownloadItem>(relaxed = true)

      every { resolveClick.onPauseResumeButtonClick(item) } returns PauseResume(1, true)

      viewModel.onPauseResumeButtonClick(item)

      verify { downloader.pauseResumeDownload(1, true) }
    }

    @Test
    fun `when action is NoInternet then emits snackbar`() = runTest {
      val item = mockk<LibraryListItem.LibraryDownloadItem>(relaxed = true)

      every { resolveClick.onPauseResumeButtonClick(item) } returns NoInternet

      viewModel.uiEvents.test {
        viewModel.onPauseResumeButtonClick(item)
        assertTrue(awaitItem() is ShowSnackbar)
      }
    }
  }

  @Nested
  inner class OnStopButtonClick {
    @Test
    fun `when action is RetryDownload then retries`() {
      val item = mockk<LibraryListItem.LibraryDownloadItem>(relaxed = true)

      every { resolveClick.onStopButtonClick(item) } returns RetryDownload(1)

      viewModel.onStopButtonClick(item)

      verify { downloader.retryDownload(1) }
    }

    @Test
    fun `when action is CancelDownload then shows confirmation dialog`() = runTest {
      val item = mockk<LibraryListItem.LibraryDownloadItem>(relaxed = true)

      every { resolveClick.onStopButtonClick(item) } returns CancelDownload(1)

      viewModel.uiEvents.test {
        viewModel.onStopButtonClick(item)

        val event = awaitItem() as OnlineLibraryViewModel.UiEvent.ShowDialog
        assertTrue(event.dialog is KiwixDialog.YesNoDialog.StopDownload)
        event.positiveAction.invoke()
        verify { downloader.cancelDownload(1) }
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `when action is NoInternet then emits snackbar`() = runTest {
      val item = mockk<LibraryListItem.LibraryDownloadItem>(relaxed = true)

      every { resolveClick.onStopButtonClick(item) } returns NoInternet

      viewModel.uiEvents.test {
        viewModel.onStopButtonClick(item)
        assertTrue(awaitItem() is ShowSnackbar)
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Test
  fun `onSearchQueryChanged updates state`() {
    viewModel.onSearchQueryChanged("hello")

    assertEquals("hello", viewModel.uiState.value.searchQuery)

    viewModel.onSearchQueryChanged("   hello  ")
    assertEquals("hello", viewModel.uiState.value.searchQuery)
  }

  @Test
  fun `openSearchView sets active`() {
    viewModel.openSearchView()
    assertTrue(viewModel.uiState.value.isSearchActive)
  }

  @Test
  fun `closeSearchView resets state`() {
    viewModel.closeSearchView()

    assertFalse(viewModel.uiState.value.isSearchActive)
    assertEquals("", viewModel.uiState.value.searchQuery)
  }

  @Test
  fun `clearSearch resets state`() {
    viewModel.clearSearch()
    assertEquals("", viewModel.uiState.value.searchQuery)
  }

  @Nested
  inner class HandleLibraryState {
    @Test
    fun `when state is Idle then updates progress`() = runTest {
      viewModel.handleLibraryState(Idle)
      assertEquals(
        context.getString(R.string.reaching_remote_library),
        viewModel.uiState.value.scanningProgressBarMessage
      )
    }

    @Test
    fun `when state is WifiOnlyException then stops loading and shows dialog`() = runTest {
      viewModel.uiEvents.test {
        viewModel.handleLibraryState(WifiOnlyException)

        val state = viewModel.uiState.value
        assertFalse(state.showScanningProgressBar)
        assertFalse(state.isLoadingMore)

        val dialog = awaitItem() as ShowDialog
        assertTrue(dialog.dialog is KiwixDialog.YesNoDialog.WifiOnly)
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `when state is NoInternetConnection then stops loading`() = runTest {
      viewModel.handleLibraryState(NoInternetConnection)

      val state = viewModel.uiState.value
      assertFalse(state.showScanningProgressBar)
      assertFalse(state.isLoadingMore)
    }

    @Test
    fun `when state is Loading then updates loading flags`() = runTest {
      viewModel.handleLibraryState(Loading(isLoadMore = false))

      val state = viewModel.uiState.value
      assertTrue(state.showScanningProgressBar)
      assertEquals(
        context.getString(R.string.reaching_remote_library),
        state.scanningProgressBarMessage
      )
    }

    @Test
    fun `when state is Parsing then updates progress`() = runTest {
      viewModel.handleLibraryState(Parsing)
      assertEquals(
        context.getString(R.string.parsing_remote_library),
        viewModel.uiState.value.scanningProgressBarMessage
      )
    }

    @Test
    fun `when success with fresh load then replaces books and scrolls to top`() = runTest {
      val books = listOf(mockk<LibkiwixBook>(), mockk())

      val request = mockk<OnlineLibraryViewModel.OnlineLibraryRequest> {
        every { isLoadMoreItem } returns false
      }

      val state = Success(
        books = books,
        totalPages = 10,
        request = request
      )

      viewModel.uiEvents.test {
        viewModel.handleLibraryState(state)

        val result = viewModel.networkBooks.first()
        assertEquals(books, result)

        assertTrue(awaitItem() is OnlineLibraryViewModel.UiEvent.ScrollToTop)
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `when success with load more then appends books`() = runTest {
      val existing = listOf(mockk<LibkiwixBook>())
      val newBooks = listOf(mockk<LibkiwixBook>())

      viewModel.networkBooks.emit(existing)

      val request = mockk<OnlineLibraryViewModel.OnlineLibraryRequest> {
        every { isLoadMoreItem } returns true
      }

      val state = Success(
        books = newBooks,
        totalPages = 10,
        request = request
      )

      viewModel.handleLibraryState(state)

      val result = viewModel.networkBooks.first()
      assertEquals(existing + newBooks, result)
    }

    @Test
    fun `when success with empty books but existing data then keeps old list`() = runTest {
      val existing = listOf(mockk<LibkiwixBook>())
      viewModel.networkBooks.emit(existing)

      val request = mockk<OnlineLibraryViewModel.OnlineLibraryRequest> {
        every { isLoadMoreItem } returns false
      }

      val state = Success(
        books = emptyList(),
        totalPages = 10,
        request = request
      )

      viewModel.handleLibraryState(state)

      val result = viewModel.networkBooks.first()
      assertEquals(existing, result)
    }

    @Test
    fun `when error and no existing books then emits empty list`() = runTest {
      viewModel.networkBooks.emit(emptyList())
      val error = OnlineLibraryViewModel.OnlineLibraryState.Error(
        OnlineLibraryViewModel.OnlineLibraryRequest(
          page = 1,
          isLoadMoreItem = false
        )
      )
      viewModel.handleLibraryState(error)

      val result = viewModel.networkBooks.first()
      assertTrue(result.isEmpty())
    }
  }

  @Nested
  inner class HandleLoadMore {
    @Test
    fun `when already loading more then does nothing`() = runTest {
      viewModel.totalPages = 10
      viewModel.setUiStateForTest(
        viewModel.uiState.value.copy(isLoadingMore = true)
      )
      viewModel.onlineLibraryRequest.test {
        viewModel.handleLoadMore(count = 20)
        expectNoEvents()
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `when next page is available then updates filters with load more request`() {
      viewModel.setUiStateForTest(
        viewModel.uiState.value.copy(isLoadingMore = false)
      )
      viewModel.totalPages = 5

      viewModel.handleLoadMore(count = 20)

      assertEquals(1, viewModel.currentRequest.page)
      assertTrue(viewModel.currentRequest.isLoadMoreItem)
    }

    @Test
    fun `when next page exceeds total pages then does nothing`() = runTest {
      viewModel.totalPages = 1
      viewModel.onlineLibraryRequest.test {
        viewModel.handleLoadMore(count = 20)
        expectNoEvents()
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `when count is zero then loads first page`() {
      viewModel.totalPages = 5

      viewModel.handleLoadMore(count = 0)

      assertEquals(1, viewModel.currentRequest.page)
      assertTrue(viewModel.currentRequest.isLoadMoreItem)
    }
  }

  @Nested
  inner class NotificationPermissionResult {
    @Test
    fun `when permission granted then retries book click`() = runTest {
      val item = mockk<LibraryListItem.BookItem>(relaxed = true)
      val activity = mockk<KiwixMainActivity>(relaxed = true)

      viewModel.setDownloadBookItem(item)

      val spyVm = spyk(viewModel)
      every { spyVm.onBookItemClick(item, activity) } just Runs

      spyVm.onNotificationPermissionResult(true, activity)

      verify { spyVm.onBookItemClick(item, activity) }
    }

    @Test
    fun `when denied and should not show rationale then shows settings dialog`() = runTest {
      val activity = mockk<KiwixMainActivity>(relaxed = true)

      every {
        permissionChecker.shouldShowRationale(activity, POST_NOTIFICATIONS)
      } returns false

      viewModel.uiEvents.test {
        viewModel.onNotificationPermissionResult(false, activity)

        val event = awaitItem() as ShowDialog
        assertTrue(event.dialog is KiwixDialog.NotificationPermissionDialog)
        event.positiveAction.invoke()

        val next = awaitItem()
        assertTrue(next is NavigateToAppSettings)
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `when denied and should show rationale then does nothing`() = runTest {
      val activity = mockk<KiwixMainActivity>(relaxed = true)

      every {
        permissionChecker.shouldShowRationale(activity, POST_NOTIFICATIONS)
      } returns true

      viewModel.uiEvents.test {
        viewModel.onNotificationPermissionResult(false, activity)

        expectNoEvents()
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Nested
  inner class StoragePermissionResult {
    @Test
    fun `when permission granted then retries book click`() = runTest {
      val item = mockk<LibraryListItem.BookItem>(relaxed = true)
      val activity = mockk<KiwixMainActivity>(relaxed = true)

      viewModel.setDownloadBookItem(item)

      val spyVm = spyk(viewModel)
      every { spyVm.onBookItemClick(item, activity) } just Runs

      spyVm.onStoragePermissionResult(true, activity)

      verify { spyVm.onBookItemClick(item, activity) }
    }

    @Test
    fun `when denied and should show rationale then requests permission`() = runTest {
      val activity = mockk<KiwixMainActivity>(relaxed = true)

      every {
        permissionChecker.shouldShowRationale(activity, WRITE_EXTERNAL_STORAGE)
      } returns true

      viewModel.uiEvents.test {
        viewModel.onStoragePermissionResult(false, activity)

        val event = awaitItem() as ShowDialog
        assertTrue(event.dialog is KiwixDialog.WriteStoragePermissionRationale)
        event.positiveAction.invoke()

        val next = awaitItem()
        assertTrue(next is OnlineLibraryViewModel.UiEvent.RequestPermission)
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `when denied and should not show rationale then navigates to settings`() = runTest {
      val activity = mockk<KiwixMainActivity>(relaxed = true)

      every {
        permissionChecker.shouldShowRationale(activity, WRITE_EXTERNAL_STORAGE)
      } returns false

      viewModel.uiEvents.test {
        viewModel.onStoragePermissionResult(false, activity)

        val event = awaitItem() as ShowDialog
        assertTrue(event.dialog is KiwixDialog.WriteStoragePermissionRationale)

        event.positiveAction.invoke()

        val next = awaitItem()
        assertTrue(next is NavigateToAppSettings)
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Nested
  inner class HandleNetworkState {
    @Test
    fun `when WifiAvailable then refreshScreen is called`() = runTest {
      val spyVm = spyk(viewModel)
      every { spyVm.refreshScreen(false) } just Runs

      spyVm.handleNetworkState(ObserveNetworkState.Result.WifiAvailable)

      verify { spyVm.refreshScreen(false) }
    }

    @Test
    fun `when ShowWifiOnlyMessage then updates UI`() = runTest {
      viewModel.handleNetworkState(ObserveNetworkState.Result.ShowWifiOnlyMessage)

      val state = viewModel.uiState.value
      assertTrue(state.showNoContent)
      assertFalse(state.showScanningProgressBar)
    }

    @Test
    fun `when no internet and no items then shows no content message`() = runTest {
      viewModel.setUiStateForTest(viewModel.uiState.value.copy(items = emptyList()))

      viewModel.handleNetworkState(ObserveNetworkState.Result.ShowNoInternetSnackBar)

      val state = viewModel.uiState.value
      assertTrue(state.showNoContent)
      assertFalse(state.showScanningProgressBar)
    }

    @Test
    fun `when no internet and items exist then emits snackbar`() = runTest {
      viewModel.setUiStateForTest(viewModel.uiState.value.copy(items = listOf(mockk())))

      viewModel.uiEvents.test {
        viewModel.handleNetworkState(ObserveNetworkState.Result.ShowNoInternetSnackBar)

        assertTrue(awaitItem() is OnlineLibraryViewModel.UiEvent.ShowSnackbar)

        val state = viewModel.uiState.value
        assertFalse(state.showScanningProgressBar)
        assertFalse(state.isRefreshing)
      }
    }

    @Test
    fun `when mobile internet and no items then triggers loading`() = runTest {
      val spyVm = spyk(viewModel)
      spyVm.setUiStateForTest(viewModel.uiState.value.copy(items = emptyList()))

      every { spyVm.updateOnlineLibraryFilters(any()) } just Runs

      spyVm.handleNetworkState(ObserveNetworkState.Result.MobileInternet)

      verify { spyVm.updateOnlineLibraryFilters(any()) }

      val state = spyVm.uiState.value
      assertTrue(state.showScanningProgressBar)
      assertFalse(state.showNoContent)
    }

    @Test
    fun `when mobile internet and items exist then does nothing`() = runTest {
      val spyVm = spyk(viewModel)
      spyVm.setUiStateForTest(viewModel.uiState.value.copy(items = listOf(mockk())))

      spyVm.handleNetworkState(ObserveNetworkState.Result.MobileInternet)

      verify(exactly = 0) { spyVm.updateOnlineLibraryFilters(any()) }
    }
  }
}
