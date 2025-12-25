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

import android.widget.FrameLayout
import androidx.compose.material3.SnackbarHostState
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.ui.models.IconItem.Drawable

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
   * Manages the showing of "Update" dialog.
   */
  val shouldShowUpdatePopup: Boolean,
  /**
   * Manages the showing of "Full screen view" of webView's video.
   *
   * A [Pair] containing:
   *  - [Boolean]: Whether to show/hide full screen mode.
   *  - [FrameLayout]: full screen view.
   */
  val fullScreenItem: Pair<Boolean, FrameLayout?>,
  /**
   * Manages the showing of "BackToTop" fab button.
   */
  val showBackToTopButton: Boolean,
  /**
   * Handles the click of "BackToTop" fab button.
   */
  val backToTopButtonClick: () -> Unit,
  val showTtsControls: Boolean = false,
  val onPauseTtsClick: () -> Unit = {},
  /**
   * Manages the showing of TTS button text(Pause/Resume).
   */
  val pauseTtsButtonText: String,
  val onStopTtsClick: () -> Unit = {},
  /**
   * Holds the current selected webView position.
   */
  val currentWebViewPosition: Int,
  /**
   * To show in the tabs view.
   */
  val kiwixWebViewList: List<KiwixWebView>,
  /**
   * To show/hide tab switcher.
   */
  val showTabSwitcher: Boolean,
  /**
   * Manages the showing of current selected webView.
   */
  val selectedWebView: KiwixWebView?,
  /**
   * Handles the (UI, and clicks) for bookmark button in reader bottom toolbar.
   *
   * A [Triple] containing:
   *  - [Unit]: Handles the normal click of button.
   *  - [Unit]: Handles the long click of button.
   *  - [Drawable]: Handles the Icon of button.
   */
  val bookmarkButtonItem: Triple<() -> Unit, () -> Unit, Drawable>,
  /**
   * Handles the clicks of previous page button in reader bottom toolbar.
   *
   * A [Triple] containing:
   *  - [Unit]: Handles the normal click of button(For going to previous page).
   *  - [Unit]: Handles the long click of button(For showing the previous pages history).
   *  - [Boolean]: Handles the button should enable or not.
   */
  val previousPageButtonItem: Triple<() -> Unit, () -> Unit, Boolean>,
  /**
   * Handles the click to open home page of ZIM file button click in reader bottom toolbar.
   */
  val onHomeButtonClick: () -> Unit,
  /**
   * Handles the clicks of next page button in reader bottom toolbar.
   *
   * A [Triple] containing:
   *  - [Unit]: Handles the normal click of button(For going to next page).
   *  - [Unit]: Handles the long click of button(For showing the next pages history).
   *  - [Boolean]: Handles the button should enable or not.
   */
  val nextPageButtonItem: Triple<() -> Unit, () -> Unit, Boolean>,
  /**
   * Handles the click to open right sidebar button click in reader bottom toolbar.
   *
   * A [Pair] containing:
   *  - [Boolean]: Handles the button should enable or not(Specially for custom apps).
   *  - [Unit]: Handles the click of button.
   */
  val tocButtonItem: Pair<Boolean, () -> Unit>,
  val onCloseAllTabs: () -> Unit,
  /**
   * Manages the showing of Reader's [BottomAppBarOfReaderScreen].
   */
  val shouldShowBottomAppBar: Boolean,
  val readerScreenTitle: String,
  /**
   * Manages the click event on tabs.
   */
  val onTabClickListener: TabClickListener,
  /**
   * Manages the showing/hiding of search placeholder in toolbar for custom apps.
   */
  val searchPlaceHolderItemForCustomApps: Pair<Boolean, () -> Unit>,
  /**
   * Manages the showing of application name in donation layout.
   */
  val appName: String,
  /**
   * Handles the click when user clicks on "Make a donation" button.
   */
  val donateButtonClick: () -> Unit,
  /**
   * Handles the click when user clicks on "Later" button in donation layout.
   */
  val laterButtonClick: () -> Unit,
  /**
   * Manages the showing of header title of "table of content".
   */
  val tableOfContentTitle: String
)
