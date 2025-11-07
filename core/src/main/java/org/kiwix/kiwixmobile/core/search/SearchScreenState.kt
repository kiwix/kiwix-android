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

package org.kiwix.kiwixmobile.core.search

import androidx.compose.runtime.Composable

data class SearchScreenState(
  /**
   * Manages the search screen list state.
   */
  val searchList: List<SearchListItem>,
  /**
   * Manages the showing of loading progress at the initial.
   */
  val isLoading: Boolean,
  /**
   * Manages the showing of load more progress at the end of search list.
   */
  val shouldShowLoadingMoreProgressBar: Boolean,
  /**
   * Handles the calling for more items.
   */
  val onLoadMore: () -> Unit,
  /**
   * Stores the searchView text, and displayed it inside the searchView.
   */
  val searchText: String,
  /**
   * Handles the click on searchView's close button.
   */
  val onSearchViewClearClick: () -> Unit,
  /**
   * Handles the changing of searchView values.
   */
  val onSearchViewValueChange: (String) -> Unit,
  /**
   * Handles the item click on searchItem
   */
  val onItemClick: (SearchListItem) -> Unit,
  /**
   * Handles the long click on searchItem.
   */
  val onItemLongClick: (SearchListItem) -> Unit,
  /**
   * Handles the newTabIcon click.
   */
  val onNewTabIconClick: (SearchListItem) -> Unit,
  /**
   * Handles the Keyboard submit button click.
   */
  val onKeyboardSubmitButtonClick: (String) -> Unit,
  /**
   * Manages the navigationIcon shown in the toolbar.
   */
  val navigationIcon: @Composable() () -> Unit,
  /**
   * Manages the showing of suggested word provided by the libkiwix if no search
   * result found for typed value.
   */
  val suggestedWordsList: List<String>,
  /**
   * Manages the click of suggested item by the libkiwix.
   */
  val onSuggestedItemClick: (String) -> Unit
)
