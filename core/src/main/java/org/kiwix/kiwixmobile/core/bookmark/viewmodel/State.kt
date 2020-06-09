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

package org.kiwix.kiwixmobile.core.bookmark.viewmodel

import org.kiwix.kiwixmobile.core.bookmark.adapter.BookmarkItem

sealed class State(
  open val bookmarks: List<BookmarkItem>,
  open val showAll: Boolean,
  open val currentZimId: String?,
  open val searchTerm: String = ""
) {

  fun getFilteredBookmarks(): List<BookmarkItem> =
    bookmarks
      .filter {
        it.bookmarkTitle.contains(
          searchTerm,
          true
        ) && (it.zimId == currentZimId || showAll)
      }

  fun toggleSelectionOfItem(bookmark: BookmarkItem): State {
    val newList = bookmarks.map {
      if (it.databaseId == bookmark.databaseId) it.apply {
        isSelected = !isSelected
      } else it
    }
    if (newList.none(BookmarkItem::isSelected)) {
      return Results(newList, showAll, currentZimId, searchTerm)
    }
    return SelectionResults(newList, showAll, currentZimId, searchTerm)
  }

  data class Results(
    override val bookmarks: List<BookmarkItem>,
    override val showAll: Boolean,
    override val currentZimId: String?,
    override val searchTerm: String = ""
  ) : State(bookmarks, showAll, currentZimId, searchTerm)

  data class SelectionResults(
    override val bookmarks: List<BookmarkItem>,
    override val showAll: Boolean,
    override val currentZimId: String?,
    override val searchTerm: String
  ) : State(bookmarks, showAll, currentZimId, searchTerm) {
    val selectedItems: List<BookmarkItem> =
      bookmarks.filter(BookmarkItem::isSelected)
  }
}
