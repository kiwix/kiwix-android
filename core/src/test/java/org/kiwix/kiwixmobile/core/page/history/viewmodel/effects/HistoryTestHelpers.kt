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

package org.kiwix.kiwixmobile.core.page.history.viewmodel.effects

import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.history.viewmodel.HistoryState

fun historyItem(
  historyTitle: String = "historyTitle",
  dateString: String = "5 Jul 2020",
  isSelected: Boolean = false,
  id: Long = 2,
  zimId: String = "zimId"
): HistoryItem {
  return HistoryItem(
    2,
    zimId,
    "zimName",
    "zimFilePath",
    "favicon",
    "historyUrl",
    historyTitle,
    dateString,
    100,
    isSelected,
    id
  )
}

fun historyState(
  historyItems: List<HistoryItem> = listOf(),
  showAll: Boolean = true,
  zimId: String = "id",
  searchTerm: String = ""
): HistoryState =
  HistoryState(
    historyItems,
    showAll,
    zimId,
    searchTerm
  )
