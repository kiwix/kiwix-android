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

package org.kiwix.kiwixmobile.core.main.reader

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.platform.ComposeView

/**
 * Represents the UI state for the Reader Screen.
 *
 * This data class encapsulates all UI-related states in a single object,
 * reducing complexity in the Fragment.
 */
data class ReaderScreenState(
  /**
   * Handles snack bar messages and displays.
   */
  val snackBarHostState: SnackbarHostState,
  /**
   * Manages the showing of "No open book" message and button.
   */
  val isNoBookOpenInReader: Boolean,
  /**
   * Handles when open library button clicks.
   */
  val onOpenLibraryButtonClicked: () -> Unit,
  /**
   * Manages the showing of "ProgressBar" when ZIM file page is loading.
   *
   * A [Pair] containing:
   *  - [Boolean]: Whether page is loading.
   *  - [Int]: progress of page loading.
   */
  val pageLoadingItem: Pair<Boolean, Int>,
  /**
   * Manages the showing of "Donation" layout.
   */
  val shouldShowDonationPopup: Boolean,
  /**
   * Manages the showing of "Full screen view".
   *
   * A [Pair] containing:
   *  - [Boolean]: Whether to show/hide full screen mode.
   *  - [ComposeView]: full screen view.
   */
  val fullScreenItem: Pair<Boolean, ComposeView>,
  /**
   * Manages the showing of "BackToTop" fab button.
   */
  val showBackToTopButton: Boolean,
  /**
   * Handles the click of "BackToTop" fab button.
   */
  val backToTopButtonClick: () -> Unit,
  val showFullscreenButton: Boolean = false,
  val onExitFullscreenClick: () -> Unit = {},
  val showTtsControls: Boolean = false,
  val onPauseTtsClick: () -> Unit = {},
  val onStopTtsClick: () -> Unit = {},
)
