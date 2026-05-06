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

package org.kiwix.kiwixmobile.nav.destination.library.local

import android.os.Build
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.navigation.compose.rememberNavController
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.components.SWIPE_REFRESH_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.CONTENT_LOADING_PROGRESS_BAR_TESTING_TAG
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.ui.BOOK_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.utils.TestApplication
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class LocalLibraryScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val context get() = RuntimeEnvironment.getApplication()

  private fun createTestState(
    bookOnDiskListItems: List<BooksOnDiskListItem> = emptyList(),
    selectionMode: SelectionMode = SelectionMode.NORMAL,
    isScanning: Boolean = false,
    scanningProgress: Int = 0,
    noFileViewVisible: Boolean = false,
    noFileViewTitle: String = "",
    noFileViewButtonText: String = ""
  ): LocalLibraryViewModel.LocalLibraryUiState = LocalLibraryViewModel.LocalLibraryUiState(
    fileSelectListState = FileSelectListState(bookOnDiskListItems, selectionMode),
    scanning = LocalLibraryViewModel.ScanningState(isScanning, scanningProgress),
    noFileView = LocalLibraryViewModel.NoFileView(
      noFileViewTitle,
      noFileViewButtonText,
      noFileViewVisible
    )
  )

  private fun renderScreen(
    state: LocalLibraryViewModel.LocalLibraryUiState,
    onRefresh: () -> Unit = {},
    onDownloadButtonClick: () -> Unit = {},
    onClick: (BookOnDisk) -> Unit = {},
    onLongClick: (BookOnDisk) -> Unit = {},
    onMultiSelect: (BookOnDisk) -> Unit = {},
    listState: LazyListState? = null
  ) {
    composeTestRule.setContent {
      LocalLibraryScreen(
        state = state,
        actionMenuItems = emptyList(),
        listState = listState ?: rememberLazyListState(),
        onRefresh = onRefresh,
        onDownloadButtonClick = onDownloadButtonClick,
        onClick = onClick,
        onLongClick = onLongClick,
        onMultiSelect = onMultiSelect,
        bottomAppBarScrollBehaviour = null,
        onUserBackPressed = { mockk(relaxed = true) },
        navHostController = rememberNavController(),
        snackbarHostState = remember { SnackbarHostState() },
        navigationIcon = {}
      )
    }
  }

  @Test
  fun localLibraryScreen_showsNoFilesView_whenEmpty() {
    renderScreen(
      createTestState(
        noFileViewVisible = true,
        noFileViewTitle = "No files here",
        noFileViewButtonText = "Download"
      )
    )
    composeTestRule
      .onNodeWithText("No files here")
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText("Download", ignoreCase = true)
      .assertIsDisplayed()
  }

  @Test
  fun localLibraryScreen_showsNoFilesView_whenListIsEmpty_evenIfIsVisibleIsFalse() {
    renderScreen(
      createTestState(
        bookOnDiskListItems = emptyList(),
        noFileViewVisible = false,
        noFileViewTitle = "No files here",
        noFileViewButtonText = "Download"
      )
    )
    composeTestRule
      .onNodeWithText("No files here")
      .assertIsDisplayed()
  }

  @Test
  fun localLibraryScreen_showsBookList_whenBooksPresent() {
    val book = mockk<BookOnDisk>(relaxed = true)
    renderScreen(createTestState(bookOnDiskListItems = listOf(book)))
    composeTestRule
      .onNodeWithTag(BOOK_LIST_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun localLibraryScreen_showsLanguageHeader_whenLanguageItemPresent() {
    val languageItem = BooksOnDiskListItem.LanguageItem("en", "English")
    renderScreen(createTestState(bookOnDiskListItems = listOf(languageItem)))
    composeTestRule
      .onNodeWithText("English")
      .assertIsDisplayed()
  }

  @Test
  fun localLibraryScreen_showsScanningProgressBar_withProgress() {
    renderScreen(createTestState(isScanning = true, scanningProgress = 50))
    composeTestRule
      .onNodeWithTag(CONTENT_LOADING_PROGRESS_BAR_TESTING_TAG)
      .assertIsDisplayed()
    // Note: verifying the exact progress value on a horizontal progress bar
    // is usually done via custom semantics or by checking the bounds,
    // but assertIsDisplayed is a good start.
  }

  @Test
  fun localLibraryScreen_downloadButtonClick_triggersCallback() {
    var clicked = false
    renderScreen(
      createTestState(
        noFileViewVisible = true,
        noFileViewTitle = "Empty",
        noFileViewButtonText = "Download"
      ),
      onDownloadButtonClick = { clicked = true }
    )
    composeTestRule
      .onNodeWithTag(DOWNLOAD_BUTTON_TESTING_TAG)
      .performClick()
    assert(clicked)
  }

  @Test
  fun localLibraryScreen_swipeToRefresh_triggersCallback() {
    var refreshed = false
    renderScreen(
      createTestState(bookOnDiskListItems = emptyList()),
      onRefresh = { refreshed = true }
    )
    composeTestRule
      .onNodeWithTag(SWIPE_REFRESH_TESTING_TAG)
      .performTouchInput {
        swipeDown(startY = 0f, endY = 800f)
      }
    composeTestRule.waitForIdle()
    assert(refreshed)
  }

  @Test
  fun localLibraryScreen_showsSwipeDownToScan_whenNoFiles() {
    renderScreen(
      createTestState(
        noFileViewVisible = true,
        noFileViewTitle = "Empty",
        noFileViewButtonText = "Download"
      )
    )
    composeTestRule
      .onNodeWithTag(SHOW_SWIPE_DOWN_TO_SCAN_FILE_SYSTEM_TEXT_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun localLibraryScreen_showsSwipeDownToScan_atBottomOfBookList() {
    val book = mockk<BookOnDisk>(relaxed = true)
    renderScreen(createTestState(bookOnDiskListItems = listOf(book)))
    composeTestRule
      .onNodeWithTag(BOOK_LIST_TESTING_TAG)
      .performScrollToIndex(1) // Scroll to the bottom item (SwipeDown text is the last item)
    composeTestRule
      .onNodeWithTag(SHOW_SWIPE_DOWN_TO_SCAN_FILE_SYSTEM_TEXT_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun localLibraryScreen_appBarTitle_showsSelectionCountInMultiMode() {
    val book1 = mockk<BookOnDisk>(relaxed = true) {
      every { isSelected } returns true
    }
    val book2 = mockk<BookOnDisk>(relaxed = true) {
      every { isSelected } returns true
    }
    renderScreen(
      createTestState(
        bookOnDiskListItems = listOf(book1, book2),
        selectionMode = SelectionMode.MULTI
      )
    )
    composeTestRule
      .onNodeWithText("2")
      .assertIsDisplayed()
  }

  @Test
  fun localLibraryScreen_bookItemSelection_isReflected() {
    val book = mockk<BookOnDisk>(relaxed = true) {
      every { isSelected } returns true
      every { id } returns "123"
    }
    renderScreen(
      createTestState(
        bookOnDiskListItems = listOf(book),
        selectionMode = SelectionMode.MULTI
      )
    )
    // Verify the checkbox or selection state.
    // In BookItem, it uses BOOK_ITEM_CHECKBOX_TESTING_TAG + index
    composeTestRule
      .onNodeWithTag("bookItemCheckboxTestingTag0")
      .assertIsDisplayed()
  }

  @Test
  fun localLibraryScreen_bookItemCallbacks_areTriggered() {
    var clicked = false
    var longClicked = false
    val book = mockk<BookOnDisk>(relaxed = true) {
      every { id } returns "123"
    }
    renderScreen(
      createTestState(bookOnDiskListItems = listOf(book)),
      onClick = { clicked = true },
      onLongClick = { longClicked = true }
    )

    composeTestRule
      .onNodeWithTag(BOOK_ITEM_TESTING_TAG)
      .performClick()
    assert(clicked)

    // Long click is usually simulated via performTouchInput
    composeTestRule
      .onNodeWithTag(BOOK_ITEM_TESTING_TAG)
      .performTouchInput { longClick() }
    assert(longClicked)
  }

  @Test
  fun localLibraryScreen_backToTopButton_visibility() {
    // Mock 10 books to allow scrolling
    val books = List(10) { i ->
      mockk<BookOnDisk>(relaxed = true) {
        every { id } returns "$i"
        every { book.title } returns "Book $i"
      }
    }

    renderScreen(createTestState(bookOnDiskListItems = books))

    // FAB should be hidden initially
    composeTestRule
      .onNodeWithContentDescription(context.getString(R.string.pref_back_to_top))
      .assertDoesNotExist()

    // Scroll to index 6 (threshold is 5)
    composeTestRule
      .onNodeWithTag(BOOK_LIST_TESTING_TAG)
      .performScrollToIndex(6)

    // FAB should now be displayed
    composeTestRule
      .onNodeWithContentDescription(context.getString(R.string.pref_back_to_top))
      .assertIsDisplayed()
  }
}
