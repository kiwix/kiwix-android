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

package org.kiwix.kiwixmobile.core.page.history.viewmodel

import org.kiwix.kiwixmobile.core.extensions.HeaderizableList
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.DateItem
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.viewmodel.PageState

data class HistoryState(
  override val pageItems: List<HistoryItem>,
  override val showAll: Boolean,
  override val currentZimId: String?,
  override val searchTerm: String = ""
) : PageState<HistoryItem>() {
  override val visiblePageItems: List<HistoryListItem> =
    HeaderizableList<HistoryListItem, HistoryItem, DateItem>(filteredPageItems)
      .foldOverAddingHeaders(
        { historyItem -> DateItem(historyItem.dateString) },
        { current, next -> current.dateString != next.dateString }
      )

  override fun copyWithNewItems(newItems: List<HistoryItem>): PageState<HistoryItem> =
    copy(pageItems = newItems)
}
