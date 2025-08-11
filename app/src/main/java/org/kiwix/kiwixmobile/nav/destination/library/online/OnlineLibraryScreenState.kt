/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

import androidx.compose.material3.SnackbarHostState
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem.BookItem
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem.LibraryDownloadItem

data class OnlineLibraryScreenState(
  /**
   * Manages the online library list state.
   */
  val onlineLibraryList: List<LibraryListItem>?,
  /**
   * Controls the visibility and behavior of the "Pull to refresh" animation.
   *
   *  - [Boolean]: The first boolean triggers/hides the "pull to refresh" animation.
   */
  val isRefreshing: Boolean,
  /**
   * Handles snack bar messages and displays.
   */
  val snackBarHostState: SnackbarHostState,
  /**
   * Manages the refresh action. When user perform the swipe down action this callback will trigger.
   */
  val onRefresh: () -> Unit,
  /**
   * Represents the scanning progress state.
   *
   * A [Pair] containing:
   *  - [Boolean]: Whether scanning is in progress.
   *  - [String]: The message which will show on the progress layout.
   */
  val scanningProgressItem: Pair<Boolean, String>,
  /**
   * Controls the visibility of the "No Content", and other error messages.
   *
   * A [Pair] containing:
   *  - [String]: The text displayed when no content are available.
   *  - [Boolean]: Whether to show or hide this view.
   */
  val noContentViewItem: Pair<String, Boolean>,
  /**
   * To get the book language.
   */
  val bookUtils: BookUtils,
  /**
   * To calculate the available space for book.
   */
  val availableSpaceCalculator: AvailableSpaceCalculator,
  /**
   * Handles the click on book item.
   */
  val onBookItemClick: (BookItem) -> Unit,
  /**
   * Handles when pause/resume button click in downloading.
   */
  val onPauseResumeButtonClick: (LibraryDownloadItem) -> Unit,
  /**
   * Handles when stop button click in downloading.
   */
  val onStopButtonClick: (LibraryDownloadItem) -> Unit,
  /**
   * Handles the showing of searchBar in toolbar.
   */
  val isSearchActive: Boolean,
  /**
   * Stores the searchView text, and displayed it inside the searchView.
   */
  val searchText: String,
  /**
   * Triggers when search query changed.
   */
  val searchValueChangedListener: (String) -> Unit,
  /**
   * Triggers when clear button clicked.
   */
  val clearSearchButtonClickListener: () -> Unit,
  /**
   * Triggers when user at the end of the online content.
   */
  val onLoadMore: (Int) -> Unit,
  /**
   * Manages the showing of progressBar at the end of book list when more items is loading.
   */
  val isLoadingMoreItem: Boolean
)
