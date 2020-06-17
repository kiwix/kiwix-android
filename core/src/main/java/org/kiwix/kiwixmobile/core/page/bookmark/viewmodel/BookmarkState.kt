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

package org.kiwix.kiwixmobile.core.page.bookmark.viewmodel

import org.kiwix.kiwixmobile.core.page.bookmark.adapter.BookmarkItem

data class BookmarkState(
  val bookmarks: List<BookmarkItem>,
  val showAll: Boolean,
  val currentZimId: String?,
  val searchTerm: String = ""
) {

  val isInSelectionState = bookmarks.any(BookmarkItem::isSelected)

  val filteredBookmarks: List<BookmarkItem> = bookmarks
    .filter { it.zimId == currentZimId || showAll }
    .filter { it.bookmarkTitle.contains(searchTerm, true) }

  fun toggleSelectionOfItem(bookmark: BookmarkItem): BookmarkState {
    val newList = bookmarks.map {
      if (it.databaseId == bookmark.databaseId) it.apply { isSelected = !isSelected } else it
    }
    return BookmarkState(newList, showAll, currentZimId, searchTerm)
  }
}
