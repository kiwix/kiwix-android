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

package org.kiwix.kiwixmobile.core.page

import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.BookmarkState
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem
import org.kiwix.kiwixmobile.core.page.history.viewmodel.HistoryState
import org.kiwix.kiwixmobile.core.page.viewmodel.TestablePageState

data class PageImpl(
  override val zimFilePath: String? = "zimFilePath",
  override val url: String = "url",
  override var isSelected: Boolean = false,
  override val id: Long = 0L,
  override val zimId: String = "zimId",
  override val title: String = "title",
  override val favicon: String? = "favicon"
) : Page

fun historyItem(
  historyTitle: String = "historyTitle",
  dateString: String = "5 Jul 2020",
  isSelected: Boolean = false,
  id: Long = 2,
  zimId: String = "zimId"
): HistoryListItem.HistoryItem {
  return HistoryListItem.HistoryItem(
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
  historyItems: List<HistoryListItem.HistoryItem> = listOf(),
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

fun bookmark(
  bookmarkTitle: String = "bookmarkTitle",
  isSelected: Boolean = false,
  id: Long = 2,
  zimId: String = "zimId",
  zimName: String = "zimName",
  zimFilePath: String = "zimFilePath",
  bookmarkUrl: String = "bookmarkUrl",
  favicon: String = "favicon"
): LibkiwixBookmarkItem {
  return LibkiwixBookmarkItem(
    id = id,
    zimId = zimId,
    zimName = zimName,
    zimFilePath = zimFilePath,
    bookmarkUrl = bookmarkUrl,
    title = bookmarkTitle,
    isSelected = isSelected,
    favicon = favicon,
    libKiwixBook = null
  )
}

fun bookmarkState(
  bookmarks: List<LibkiwixBookmarkItem> = emptyList(),
  showAll: Boolean = true,
  zimId: String = "id",
  searchTerm: String = ""
): BookmarkState = BookmarkState(bookmarks, showAll, zimId, searchTerm)

fun pageState(): TestablePageState = TestablePageState()
