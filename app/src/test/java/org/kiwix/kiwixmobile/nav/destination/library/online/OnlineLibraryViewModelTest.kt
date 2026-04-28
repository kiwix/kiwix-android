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

import android.content.Context
import app.cash.turbine.test
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.language.viewmodel.flakyTest
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.LibraryDownloadItem
import org.kiwix.sharedFunctions.InstantExecutorExtension

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(InstantExecutorExtension::class)
class OnlineLibraryViewModelTest {
  private val downloader: Downloader = mockk(relaxed = true)
  private val kiwixDataStore: KiwixDataStore = mockk(relaxed = true)
  private val bookUtils: BookUtils = mockk(relaxed = true)
  private val availableSpaceCalculator: AvailableSpaceCalculator = mockk(relaxed = true)
  private val permissionChecker: KiwixPermissionChecker = mockk(relaxed = true)

  private lateinit var viewModel: OnlineLibraryViewModel

  @BeforeEach
  fun setup() {
    clearAllMocks()
    viewModel = OnlineLibraryViewModel(
      downloader,
      kiwixDataStore,
      bookUtils,
      availableSpaceCalculator,
      permissionChecker
    )
  }

  @Test
  fun `emitNoInternetSnackbar emits ShowSnackbar event`() = flakyTest {
    runTest {
      val context = mockk<Context>()
      every { context.getString(R.string.no_network_connection) } returns "No network connection"
      every { context.getString(R.string.menu_settings) } returns "Settings"

      viewModel.uiEvents.test {
        viewModel.emitNoInternetSnackbar(context)

        val event = awaitItem() as OnlineLibraryViewModel.UiEvent.ShowSnackbar
        assertThat(event.message).isEqualTo("No network connection")
        assertThat(event.actionLabel).isEqualTo("Settings")
        assertThat(event.actionIntent).isNotNull()
      }
    }
  }

  @Test
  fun `emitNoSpaceSnackbar emits ShowNoSpaceSnackbar event`() = flakyTest {
    runTest {
      val context = mockk<Context>()
      every { context.getString(R.string.download_no_space) } returns "No space available"
      every { context.getString(R.string.space_available) } returns "Space available:"
      every { context.getString(R.string.change_storage) } returns "Change Storage"

      viewModel.uiEvents.test {
        var clicked = false
        viewModel.emitNoSpaceSnackbar(context, "10 MB") { clicked = true }

        val event = awaitItem() as OnlineLibraryViewModel.UiEvent.ShowNoSpaceSnackbar
        assertThat(event.message).contains("No space available")
        assertThat(event.message).contains("Space available: 10 MB")
        assertThat(event.actionLabel).isEqualTo("Change Storage")

        event.onAction()
        assertThat(clicked).isTrue()
      }
    }
  }

  @Test
  fun `emitToast emits ShowToast event`() = flakyTest {
    runTest {
      viewModel.uiEvents.test {
        viewModel.emitToast("Test toast message")
        val event = awaitItem() as OnlineLibraryViewModel.UiEvent.ShowToast
        assertThat(event.message).isEqualTo("Test toast message")
      }
    }
  }

  @Test
  fun `downloadFile triggers downloader and clears downloadBookItem`() = runTest {
    val book = mockk<org.kiwix.kiwixmobile.core.entity.LibkiwixBook>()
    val bookItem = mockk<LibraryListItem.BookItem>()
    every { bookItem.book } returns book

    viewModel.setDownloadBookItem(bookItem)
    assertThat(viewModel.downloadBookItem).isEqualTo(bookItem)

    viewModel.downloadFile()

    verify(exactly = 1) { downloader.download(book) }
    assertThat(viewModel.downloadBookItem).isNull()
  }

  @Test
  fun `refreshFragment emits NoInternet when network is unavailable`() = runTest {
    val context = mockk<Context>()
    val activity = mockk<KiwixMainActivity>()
    val zimManageViewModel = mockk<ZimManageViewModel>()

    every { context.getString(R.string.no_network_connection) } returns "No network"
    every { context.getString(R.string.menu_settings) } returns "Settings"
    every { context.getString(R.string.reaching_remote_library) } returns "Reaching remote"

    // Given network is unavailable
    mockkStatic(org.kiwix.kiwixmobile.core.utils.NetworkUtils::class)
    every { org.kiwix.kiwixmobile.core.utils.NetworkUtils.isNetworkAvailable(activity) } returns false

    var isRefreshing = true

    viewModel.refreshScreen(
      activity,
      zimManageViewModel,
      isExplicitRefresh = true,
      context = context,
      isListEmpty = true,
      onRefreshingChanged = { isRefreshing = it }
    )

    assertThat(isRefreshing).isFalse()
    assertThat(viewModel.noContentState.value).isEqualTo(Pair("No network", true))
    assertThat(viewModel.scanningProgress.value).isEqualTo(Pair(false, "Reaching remote"))
  }

  @Test
  fun `handleLoadMore updates onlineLibraryRequest correctly`() = runTest {
    val zimManageViewModel = mockk<ZimManageViewModel>(relaxed = true)

    // 50 total results, 10 per page -> 5 pages. Current page depends on count
    every { zimManageViewModel.onlineLibraryManager.totalResult } returns 50
    every { zimManageViewModel.onlineLibraryManager.calculateTotalPages(any(), any()) } returns 5

    val currentRequest = ZimManageViewModel.OnlineLibraryRequest(null, "", "", false, 1)
    val flowRequest = kotlinx.coroutines.flow.MutableStateFlow(currentRequest)
    every { zimManageViewModel.onlineLibraryRequest } returns flowRequest

    // loading more when 10 items have been loaded
    val loadedCount = org.kiwix.kiwixmobile.core.data.remote.KiwixService.ITEMS_PER_PAGE
    viewModel.handleLoadMore(zimManageViewModel, loadedCount)

    verify {
      zimManageViewModel.updateOnlineLibraryFilters(
        match {
          it.page == 1 && it.isLoadMoreItem
        }
      )
    }
  }

  @Test
  fun `onNavigateToSettingsClicked emits NavigateToSettings event`() = runTest {
    viewModel.uiEvents.test {
      viewModel.onNavigateToSettingsClicked()
      assertThat(awaitItem()).isInstanceOf(OnlineLibraryViewModel.UiEvent.NavigateToSettings::class.java)
    }
  }

  @Test
  fun `onNavigateToAppSettingsClicked emits NavigateToAppSettings event`() = runTest {
    viewModel.uiEvents.test {
      viewModel.onNavigateToAppSettingsClicked()
      assertThat(awaitItem()).isInstanceOf(OnlineLibraryViewModel.UiEvent.NavigateToAppSettings::class.java)
    }
  }

  @Test
  fun `pauseResumeDownload triggers downloader with correct isResumeAction`() = runTest {
    val downloadId = 123L
    val item = mockk<LibraryDownloadItem>()
    every { item.downloadId } returns downloadId

    // When state is Paused -> isResumeAction should be true
    every { item.downloadState } returns DownloadState.Paused
    viewModel.pauseResumeDownload(item)
    verify { downloader.pauseResumeDownload(downloadId, true) }

    // When state is Downloading (or anything else) -> isResumeAction should be false
    every { item.downloadState } returns mockk<DownloadState>()
    viewModel.pauseResumeDownload(item)
    verify { downloader.pauseResumeDownload(downloadId, false) }
  }

  @Test
  fun `emitDialog emits ShowDialog event`() = runTest {
    val dialog = KiwixDialog.ManageExternalFilesPermissionDialog
    val neutralAction = {}
    val positiveAction = {}

    viewModel.uiEvents.test {
      viewModel.emitDialog(dialog, neutralAction, positiveAction)
      val event = awaitItem() as OnlineLibraryViewModel.UiEvent.ShowDialog
      assertThat(event.dialog).isEqualTo(dialog)
      assertEquals(neutralAction, event.negativeAction)
      assertEquals(positiveAction, event.positiveAction)
    }
  }
}
