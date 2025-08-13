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

package org.kiwix.kiwixmobile.nav.destination.library.local

import androidx.compose.material3.SnackbarHostState
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState

/**
 * Represents the UI state for the Local Library Screen.
 *
 * This data class encapsulates all UI-related states in a single object,
 * reducing complexity in the Fragment.
 */
data class LocalLibraryScreenState(
  /**
   * Manages the file selection list state.
   */
  val fileSelectListState: FileSelectListState,
  /**
   * Handles snack bar messages and displays.
   */
  val snackBarHostState: SnackbarHostState,
  /**
   * Controls the visibility and behavior of the "Pull to refresh" animation.
   *
   * A [Pair] containing:
   *  - [Boolean]: The first boolean triggers/hides the "pull to refresh" animation.
   *  - [Boolean]: The second boolean enables/disables the "pull to refresh" gesture.
   */
  val swipeRefreshItem: Pair<Boolean, Boolean>,
  /**
   * Represents the scanning progress state.
   *
   * A [Pair] containing:
   *  - [Boolean]: Whether scanning is in progress.
   *  - [Int]: The progress percentage of the scan.
   */
  val scanningProgressItem: Pair<Boolean, Int>,
  /**
   * Controls the visibility of the "No files here" message and the "Download books" button.
   *
   * A [Triple] containing:
   *  - [String]: The title text displayed when no files are available.
   *  - [String]: The label for the download button.
   *  - [Boolean]: Whether to show or hide this view.
   */
  val noFilesViewItem: Triple<String, String, Boolean>,
  /**
   * Represents a list of action menu items available in the screen's top app bar.
   */
  val actionMenuItems: List<ActionMenuItem>
)
