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

import org.kiwix.kiwixmobile.core.extensions.HeaderizableList
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.DateItem
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem
import java.text.SimpleDateFormat
import java.util.Locale

sealed class State(
  open val historyItems: List<HistoryItem>,
  open val showAll: Boolean,
  open val currentZimId: String?,
  open val searchTerm: String = ""
) {

  private val dateFormatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

  val historyListItems: List<HistoryListItem> by lazy {
    HeaderizableList<HistoryListItem, HistoryItem, DateItem>(historyItems
      .filter {
        it.historyTitle.contains(
          searchTerm,
          true
        ) && (it.zimId == currentZimId || showAll)
      }
      .sortedByDescending { dateFormatter.parse(it.dateString) })
      .foldOverAddingHeaders(
        { historyItem -> DateItem(historyItem.dateString) },
        { current, next -> current.dateString != next.dateString }
      )
  }

  fun toggleSelectionOfItem(historyListItem: HistoryItem): State {
    val newList = historyItems.map {
      if (it.id == historyListItem.id) it.apply {
        isSelected = !isSelected
      } else it
    }
    if (newList.none(HistoryItem::isSelected)) {
      return Results(newList, showAll, currentZimId)
    }
    return SelectionResults(newList, showAll, currentZimId, searchTerm)
  }

  // fun filterBasedOnSearchTerm(searchTerm: String): State =
  //   copy(historyListItems = historyItems.filter {
  //     it.historyTitle.contains(
  //       searchTerm,
  //       true
  //     )
  //   })

  data class Results(
    override val historyItems: List<HistoryItem>,
    override val showAll: Boolean,
    override val currentZimId: String?,
    override val searchTerm: String = ""
  ) : State(historyItems, showAll, currentZimId, searchTerm)

  data class SelectionResults(
    override val historyItems: List<HistoryItem>,
    override val showAll: Boolean,
    override val currentZimId: String?,
    override val searchTerm: String
  ) : State(historyItems, showAll, currentZimId, searchTerm) {
    val selectedItems: List<HistoryItem> =
      historyListItems.filterIsInstance<HistoryItem>().filter(HistoryItem::isSelected)
  }
}
