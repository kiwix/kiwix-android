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

sealed class State(
  open val historyItems: List<HistoryListItem>
) {

  data class Results(
    val searchString: String,
    override val historyItems: List<HistoryListItem>,
    val currentHistoryToggled: Boolean,
    val currentBookId: String
  ) : State(historyItems)

  data class NoResults(
    val searchString: String,
    override val historyItems: List<HistoryListItem>
  ) : State(historyItems)

  data class SelectionResults(
    val searchString: String,
    override val historyItems: List<HistoryListItem>,
    val selectedHistoryItems: List<HistoryListItem>,
    val currentHistoryToggled: Boolean,
    val currentBookId: String
  ) : State(historyItems)
}
