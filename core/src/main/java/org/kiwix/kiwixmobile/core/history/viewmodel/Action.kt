/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.history.viewmodel

import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem

sealed class Action {
  object ExitHistory : Action()
  object ExitActionModeMenu : Action()
  object UserClickedConfirmDelete : Action()
  object UserClickedDeleteButton : Action()
  object UserClickedDeleteSelectedHistoryItems : Action()

  data class OnItemClick(val historyItem: HistoryItem) : Action()
  data class OnItemLongClick(val historyItem: HistoryItem) : Action()
  data class ToggleShowHistoryFromAllBooks(val isChecked: Boolean) : Action()
  data class AllHistoryPreferenceChanged(val showAll: Boolean) : Action()
  data class Filter(val searchTerm: String) : Action()
  data class UpdateHistory(val history: List<HistoryItem>) : Action()
}
