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

import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem

sealed class State {

  data class SearchResults(
    val searchString: String,
    val historyItems: List<HistoryListItem>,
    val viewAllHistoryToggle: Boolean
  ) : State()

  data class NoSearchResults(val searchString: String) : State()
  data class CurrentHistory(val historyDao: HistoryDao) : State()
  data class AllHistory(val historyItems: List<HistoryListItem>) : State()
  data class SelectedHistoryItems(val historyItems: List<HistoryListItem>) : State()
}
