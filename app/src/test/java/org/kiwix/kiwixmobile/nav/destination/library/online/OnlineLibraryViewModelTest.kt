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

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Application
import android.net.ConnectivityManager
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Idle
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.NoInternetConnection
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.WifiOnlyException
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Success
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Parsing
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Loading
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.RequestPermission
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.ShowDialog
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.ShowNoSpaceSnackbar
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.ShowSnackbar
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem
import org.kiwix.libkiwix.Book
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

  @Nested
  inner class RefreshScreen {
    @Test
    fun `given proceed action when refreshScreen then show scanning`() = runTest {
      coEvery { refreshAction.invoke(any()) } returns Proceed

      viewModel.refreshScreen(isExplicitRefresh = true)

      advanceUntilIdle()

      assertTrue(viewModel.uiState.value.showScanning)
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

        val event = awaitItem() as ShowDialog
        assertTrue(event.dialog is KiwixDialog.YesNoDialog.WifiOnly)
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `when action is NotEnoughSpace then shows no space snackbar`() = runTest {
      val item = mockk<LibraryListItem.BookItem>(relaxed = true)
      val activity = mockk<KiwixMainActivity>(relaxed = true)

      coEvery { resolveClick.onBookItemClick(any(), any()) } returns NotEnoughSpace("100")

      viewModel.uiEvents.test {
        viewModel.onBookItemClick(item, activity)
        advanceUntilIdle()
        assertTrue(awaitItem() is ShowNoSpaceSnackbar)
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

        val event = awaitItem() as ShowDialog
        assertTrue(event.dialog is KiwixDialog.YesNoDialog.StopDownload)
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
        viewModel.uiState.value.scanningMessage
      )
    }

    @Test
    fun `when state is WifiOnlyException then stops loading and shows dialog`() = runTest {
      viewModel.uiEvents.test {
        viewModel.handleLibraryState(WifiOnlyException)

        val state = viewModel.uiState.value
        assertFalse(state.showScanning)
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
      assertFalse(state.showScanning)
      assertFalse(state.isLoadingMore)
    }

    @Test
    fun `when state is Loading then updates loading flags`() = runTest {
      viewModel.handleLibraryState(Loading(isLoadMore = false))

      val state = viewModel.uiState.value
      assertTrue(state.showScanning)
      assertEquals(
        context.getString(R.string.reaching_remote_library),
        state.scanningMessage
      )
    }

    @Test
    fun `when state is Parsing then updates progress`() = runTest {
      viewModel.handleLibraryState(Parsing)
      assertEquals(
        context.getString(R.string.parsing_remote_library),
        viewModel.uiState.value.scanningMessage
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
}
