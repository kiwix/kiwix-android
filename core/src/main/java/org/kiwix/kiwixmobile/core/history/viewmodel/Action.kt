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

import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.utils.DialogShower

sealed class Action {
  object ExitHistory : Action()
  object ExitActionModeMenu : Action()
  object DeleteAllHistoryItems : Action()
  object DeleteSelectedHistoryItems : Action()

  data class RequestDeleteAllHistoryItems(val dialogShower: DialogShower) : Action()
  data class RequestDeleteSelectedHistoryItems(val dialogShower: DialogShower) : Action()
  data class OnItemClick(val historyListItem: HistoryListItem) : Action()
  data class OnItemLongClick(val historyItem: HistoryItem) : Action()
  data class ToggleShowHistoryFromAllBooks(val isChecked: Boolean) : Action()
  data class Filter(val searchTerm: String) : Action()
  data class DeleteHistoryItems(val itemsToDelete: List<HistoryItem>) : Action()
  data class CreatedWithIntent(val searchTerm: String) : Action()
}
