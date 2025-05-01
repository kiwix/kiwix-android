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
import androidx.compose.runtime.Composable
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem

data class OnlineLibraryScreenState(
  /**
   * Manages the online library list state.
   */
  val onlineLibraryList: List<LibraryListItem>?,
  /**
   * Stores the height of the bottom navigation bar in pixels.
   */
  val bottomNavigationHeight: Int,
  /**
   * Controls the visibility and behavior of the "Pull to refresh" animation.
   *
   * A [Pair] containing:
   *  - [Boolean]: The first boolean triggers/hides the "pull to refresh" animation.
   *  - [Boolean]: The second boolean enables/disables the "pull to refresh" gesture.
   */
  val swipeRefreshItem: Pair<Boolean, Boolean>,
  /**
   * Handles snack bar messages and displays.
   */
  val snackBarHostState: SnackbarHostState,
  /**
   * Represents a list of action menu items available in the screen's top app bar.
   */
  val actionMenuItems: List<ActionMenuItem>,
  /**
   * Manages the navigation Icon shown on the OnlineLibraryScreen.
   */
  val navigationIcon: @Composable () -> Unit,
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
)
