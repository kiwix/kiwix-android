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

package org.kiwix.kiwixmobile.core.page

import androidx.annotation.StringRes
import org.kiwix.kiwixmobile.core.page.viewmodel.PageState

/**
 * Represents the UI state for the PageFragment Screen.
 * A Base screen for Bookmarks, History, and Notes screens.
 *
 * This data class encapsulates all UI-related states in a single object,
 * reducing complexity in the Fragment.
 */
data class PageFragmentScreenState(
  val pageState: PageState<*>,
  val isSearchActive: Boolean,
  val searchQueryHint: String,
  val searchText: String,
  val searchValueChangedListener: (String) -> Unit,
  val clearSearchButtonClickListener: () -> Unit,
  @StringRes val screenTitle: Int,
  val noItemsString: String,
  val switchString: String,
  val switchIsChecked: Boolean,
  val switchIsEnabled: Boolean = true,
  val onSwitchCheckedChanged: (Boolean) -> Unit,
  @StringRes val deleteIconTitle: Int
)
