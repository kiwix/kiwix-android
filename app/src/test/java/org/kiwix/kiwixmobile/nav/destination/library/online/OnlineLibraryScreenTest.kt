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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.navigation.NavHostController
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.kiwix.kiwixmobile.core.R.string
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.ui.components.CONTENT_LOADING_PROGRESS_BAR_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.SWIPE_REFRESH_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryUiState
import org.kiwix.sharedFunctions.TestApplication
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class OnlineLibraryScreenTest {
  @Rule
  @JvmField
  val composeTestRule = createComposeRule()

  private val context get() = RuntimeEnvironment.getApplication()

  private fun createMockViewModel(): OnlineLibraryViewModel = mockk(relaxed = true)

  private fun createUiState(
    items: List<LibraryListItem> = emptyList(),
    isRefreshing: Boolean = false,
    isLoadingMore: Boolean = false,
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    scanningProgressBarMessage: String = "",
    showScanningProgressBar: Boolean = false,
    noContentMessage: String = "",
    showNoContent: Boolean = false
  ): OnlineLibraryUiState = OnlineLibraryUiState(
    items = items,
    isRefreshing = isRefreshing,
    isLoadingMore = isLoadingMore,
    searchQuery = searchQuery,
    isSearchActive = isSearchActive,
    scanningProgressBarMessage = scanningProgressBarMessage,
    showScanningProgressBar = showScanningProgressBar,
    noContentMessage = noContentMessage,
    showNoContent = showNoContent,
  )

  private fun renderScreen(
    uiState: OnlineLibraryUiState,
    viewModel: OnlineLibraryViewModel = createMockViewModel(),
    actionMenuItems: List<ActionMenuItem> = emptyList(),
    lazyListState: LazyListState = LazyListState(),
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
    onBackPressed: () -> FragmentActivityExtensions.Super = { FragmentActivityExtensions.Super.ShouldCall },
    navHostController: NavHostController = mockk(relaxed = true)
  ) {
    composeTestRule.setContent {
      OnlineLibraryScreen(
        uiState = uiState,
        onlineLibraryViewModel = viewModel,
        actionMenuItems = actionMenuItems,
        listState = lazyListState,
        snackBarHostState = snackbarHostState,
        bottomAppBarScrollBehaviour = null,
        onUserBackPressed = onBackPressed,
        navHostController = navHostController,
        activity = mockk(relaxed = true),
        navigationIcon = {}
      )
    }
  }

  @Test
  fun `shows no content view when showNoContent is true`() {
    renderScreen(createUiState(showNoContent = true, noContentMessage = "No data"))
    composeTestRule
      .onNodeWithTag(NO_CONTENT_VIEW_TEXT_TESTING_TAG)
      .assertExists()
      .assertTextEquals("No data")
  }

  @Test
  fun `shows loading when scanning`() {
    renderScreen(
      createUiState(showScanningProgressBar = true, scanningProgressBarMessage = "Loading...")
    )

    composeTestRule
      .onNodeWithTag(SHOW_FETCHING_LIBRARY_LAYOUT_TESTING_TAG)
      .assertExists()
  }

  @Test
  fun `swipe refresh triggers refreshScreen`() {
    val viewModel = createMockViewModel()
    renderScreen(createUiState(), viewModel = viewModel)
    composeTestRule.apply {
      onNodeWithTag(SWIPE_REFRESH_TESTING_TAG).performTouchInput { swipeDown() }
      waitForIdle()
    }
    verify { viewModel.refreshScreen(true) }
  }

  @Test
  fun `shows search bar when search active`() {
    renderScreen(createUiState(isSearchActive = true))
    composeTestRule
      .onNodeWithTag(ONLINE_LIBRARY_SEARCH_VIEW_TESTING_TAG)
      .assertExists()
  }

  @Test
  fun `search bar is not shown when search inactive`() {
    renderScreen(createUiState(isSearchActive = false))

    composeTestRule
      .onNodeWithTag(ONLINE_LIBRARY_SEARCH_VIEW_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun `clicking clear search triggers clearSearch`() {
    val viewModel = createMockViewModel()

    renderScreen(
      createUiState(isSearchActive = true, searchQuery = "hello"),
      viewModel = viewModel
    )

    composeTestRule
      .onNodeWithTag(ONLINE_LIBRARY_SEARCH_VIEW_CLOSE_BUTTON_TESTING_TAG)
      .performClick()

    verify { viewModel.clearSearch() }
  }

  @Test
  fun `calls handleLoadMore when reaching end of list`() {
    val viewModel = createMockViewModel()
    val items = List(20) { mockk<LibraryListItem.BookItem>(relaxed = true) }
    val listState = LazyListState()
    composeTestRule.mainClock.autoAdvance = false
    renderScreen(createUiState(items = items), lazyListState = listState, viewModel = viewModel)
    composeTestRule.apply {
      runOnIdle {
        runBlocking {
          listState.scrollToItem(18)
        }
      }
      mainClock.advanceTimeBy(500)
      waitForIdle()
    }
    verify { viewModel.handleLoadMore(20) }
  }

  @Test
  fun `does not trigger load more when not near end`() {
    val viewModel = createMockViewModel()
    val items = List(50) { mockk<LibraryListItem.BookItem>(relaxed = true) }
    val listState = LazyListState()

    composeTestRule.mainClock.autoAdvance = false

    renderScreen(createUiState(items = items), lazyListState = listState, viewModel = viewModel)

    composeTestRule.runOnIdle {
      runBlocking { listState.scrollToItem(5) }
    }

    composeTestRule.mainClock.advanceTimeBy(500)
    composeTestRule.waitForIdle()

    verify(exactly = 0) { viewModel.handleLoadMore(any()) }
  }

  @Test
  fun `shows back to top button when scrolled past threshold`() {
    val listState = LazyListState()
    val items = List(20) { mockk<LibraryListItem.BookItem>(relaxed = true) }
    renderScreen(createUiState(items = items), lazyListState = listState)

    composeTestRule.apply {
      runOnIdle {
        runBlocking { listState.scrollToItem(6) }
      }
      waitForIdle()
      onNodeWithContentDescription(context.getString(string.pref_back_to_top))
        .assertExists()
    }
  }

  @Test
  fun `clicking back to top scrolls list to top`() {
    val listState = LazyListState()
    val items = List(20) { mockk<LibraryListItem.BookItem>(relaxed = true) }

    renderScreen(createUiState(items = items), lazyListState = listState)
    composeTestRule.apply {
      runOnIdle { runBlocking { listState.scrollToItem(10) } }
      onNodeWithContentDescription(context.getString(string.pref_back_to_top))
        .performClick()
      waitForIdle()
    }
    assert(listState.firstVisibleItemIndex == ZERO)
  }

  @Test
  fun `swipe refresh does NOT trigger when scanning`() {
    val viewModel = createMockViewModel()
    renderScreen(createUiState(showScanningProgressBar = true), viewModel = viewModel)
    composeTestRule.apply {
      onNodeWithTag(SWIPE_REFRESH_TESTING_TAG).performTouchInput { swipeDown() }
      waitForIdle()
    }

    verify(exactly = ZERO) { viewModel.refreshScreen(true) }
  }

  @Test
  fun `does not call handleLoadMore when already loading`() {
    val viewModel = createMockViewModel()
    val items = List(20) { mockk<LibraryListItem.BookItem>(relaxed = true) }
    val listState = LazyListState()

    composeTestRule.mainClock.autoAdvance = false
    renderScreen(
      createUiState(items = items, isLoadingMore = true),
      lazyListState = listState,
      viewModel = viewModel
    )

    composeTestRule.apply {
      runOnIdle { runBlocking { listState.scrollToItem(18) } }
      mainClock.advanceTimeBy(500)
      waitForIdle()
    }

    verify(exactly = ZERO) { viewModel.handleLoadMore(any()) }
  }

  @Test
  fun `shows divider item`() {
    val sectionTitle = "Section A"
    val items = listOf(LibraryListItem.DividerItem(id = 0L, sectionTitle = sectionTitle))
    renderScreen(createUiState(items = items))

    composeTestRule
      .onNodeWithTag(ONLINE_DIVIDER_ITEM_TEXT_TESTING_TAG)
      .assertExists()
      .assertTextEquals(sectionTitle)
  }

  @Test
  fun `shows both divider and book items`() {
    val items = listOf(
      LibraryListItem.DividerItem(1, "Section"),
      mockk<LibraryListItem.BookItem>(relaxed = true)
    )

    renderScreen(createUiState(items = items))

    composeTestRule.onNodeWithTag(ONLINE_DIVIDER_ITEM_TEXT_TESTING_TAG).assertExists()
    composeTestRule.onNodeWithTag(ONLINE_BOOK_ITEM_TESTING_TAG).assertExists()
  }

  @Test
  fun `shows load more progress when loading more`() {
    renderScreen(createUiState(isLoadingMore = true))

    composeTestRule
      .onNodeWithTag(CONTENT_LOADING_PROGRESS_BAR_TESTING_TAG)
      .assertExists()
  }

  @Test
  fun `typing in search triggers query update`() {
    val viewModel = createMockViewModel()

    renderScreen(
      createUiState(isSearchActive = true),
      viewModel = viewModel
    )

    composeTestRule
      .onNodeWithTag(ONLINE_LIBRARY_SEARCH_VIEW_TESTING_TAG)
      .performTextInput("hello")
    composeTestRule.waitForIdle()
    verify { viewModel.onSearchQueryChanged("hello") }
  }

  @Test
  fun `does not show list when no content is visible`() {
    val items = List(5) { mockk<LibraryListItem.BookItem>(relaxed = true) }

    renderScreen(createUiState(items = items, showNoContent = true))

    composeTestRule
      .onNodeWithTag(NO_CONTENT_VIEW_TEXT_TESTING_TAG)
      .assertExists()
    composeTestRule
      .onAllNodes(hasTestTag(ONLINE_DIVIDER_ITEM_TEXT_TESTING_TAG))
      .assertCountEquals(ZERO)
  }

  @Test
  fun `clicking book item triggers onBookItemClick`() {
    val item = mockk<LibraryListItem.BookItem>(relaxed = true) {
      every { canBeDownloaded } returns true
      every { book } returns mockk(relaxed = true)
    }

    val availableSpaceCalculator = mockk<AvailableSpaceCalculator> {
      coEvery { hasAvailableSpaceForBook(any()) } returns true
    }

    val viewModel = createMockViewModel().apply {
      every { this@apply.availableSpaceCalculator } returns availableSpaceCalculator
      every { bookUtils } returns mockk(relaxed = true)
    }

    renderScreen(createUiState(items = listOf(item)), viewModel = viewModel)
    composeTestRule.waitForIdle()
    composeTestRule
      .onAllNodesWithTag(ONLINE_BOOK_ITEM_TESTING_TAG)[0]
      .performClick()

    verify { viewModel.onBookItemClick(item, any()) }
  }

  @Test
  fun `clicking disabled book item does not trigger click`() {
    val viewModel = createMockViewModel()

    val item = mockk<LibraryListItem.BookItem>(relaxed = true) {
      every { canBeDownloaded } returns false
    }

    renderScreen(createUiState(items = listOf(item)), viewModel = viewModel)

    composeTestRule
      .onAllNodesWithTag(ONLINE_BOOK_ITEM_TESTING_TAG)[0]
      .performClick()

    verify(exactly = ZERO) { viewModel.onBookItemClick(any(), any()) }
  }

  @Test
  fun `clicking pause resume triggers callback`() {
    val viewModel = createMockViewModel()
    val item = mockk<LibraryListItem.LibraryDownloadItem>(relaxed = true)

    renderScreen(createUiState(items = listOf(item)), viewModel = viewModel)

    composeTestRule
      .onNodeWithTag(DOWNLOADING_PAUSE_BUTTON_TESTING_TAG)
      .performClick()

    verify { viewModel.onPauseResumeButtonClick(item) }
  }

  @Test
  fun `clicking stop triggers callback`() {
    val viewModel = createMockViewModel()
    val item = mockk<LibraryListItem.LibraryDownloadItem>(relaxed = true)

    renderScreen(createUiState(items = listOf(item)), viewModel = viewModel)

    composeTestRule
      .onNodeWithTag(DOWNLOADING_STOP_BUTTON_TESTING_TAG)
      .performClick()

    verify { viewModel.onStopButtonClick(item) }
  }

  @Test
  fun `failed download triggers stop automatically`() {
    val viewModel = createMockViewModel()

    val item = mockk<LibraryListItem.LibraryDownloadItem>(relaxed = true) {
      every { currentDownloadState } returns Status.FAILED
      every { downloadError } returns Error.UNKNOWN_IO_ERROR
    }

    renderScreen(createUiState(items = listOf(item)), viewModel = viewModel)

    composeTestRule.waitForIdle()

    verify { viewModel.onStopButtonClick(item) }
  }

  @Test
  fun `failed download with unsupported error does not trigger stop`() {
    val viewModel = createMockViewModel()

    val item = mockk<LibraryListItem.LibraryDownloadItem>(relaxed = true) {
      every { currentDownloadState } returns Status.FAILED
      every { downloadError } returns Error.REQUEST_NOT_SUCCESSFUL
    }

    renderScreen(createUiState(items = listOf(item)), viewModel = viewModel)

    composeTestRule.waitForIdle()

    verify(exactly = ZERO) { viewModel.onStopButtonClick(any()) }
  }
}
