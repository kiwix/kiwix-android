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
import org.kiwix.kiwixmobile.core.page.bookmark.models.BookmarkItem
import org.kiwix.kiwixmobile.core.page.bookmark.models.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.BookmarkState
import org.kiwix.kiwixmobile.core.page.history.models.HistoryListItem
import org.kiwix.kiwixmobile.core.page.history.viewmodel.HistoryState
import org.kiwix.kiwixmobile.core.page.notes.models.NoteListItem
import org.kiwix.kiwixmobile.core.page.viewmodel.TestablePageState
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource

data class PageImpl(
  override val zimReaderSource: ZimReaderSource?,
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
  zimId: String = "zimId",
  historyUrl: String = "historyUrl",
  zimReaderSource: ZimReaderSource
): HistoryListItem.HistoryItem {
  return HistoryListItem.HistoryItem(
    2,
    zimId,
    "zimName",
    zimReaderSource,
    "favicon",
    historyUrl,
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
  zimReaderSource: ZimReaderSource,
  bookmarkUrl: String = "bookmarkUrl",
  favicon: String = "favicon"
): BookmarkItem {
  return BookmarkItem(
    id = id,
    zimId = zimId,
    zimName = zimName,
    zimReaderSource = zimReaderSource,
    bookmarkUrl = bookmarkUrl,
    title = bookmarkTitle,
    isSelected = isSelected,
    favicon = favicon
  )
}

fun libkiwixBookmarkItem(
  databaseId: Long = 0L,
  bookmarkTitle: String = "bookmarkTitle",
  isSelected: Boolean = false,
  id: Long = 2,
  zimId: String = "zimId",
  zimName: String = "zimName",
  zimReaderSource: ZimReaderSource,
  bookmarkUrl: String = "bookmarkUrl",
  favicon: String = "favicon"
): LibkiwixBookmarkItem {
  return LibkiwixBookmarkItem(
    databaseId = databaseId,
    id = id,
    zimId = zimId,
    zimName = zimName,
    zimFilePath = zimReaderSource.toDatabase(),
    zimReaderSource = null,
    bookmarkUrl = bookmarkUrl,
    title = bookmarkTitle,
    isSelected = isSelected,
    favicon = favicon,
    libKiwixBook = null
  )
}

fun note(
  zimId: String = "id",
  title: String = "noteTitle",
  zimUrl: String = "",
  url: String = "",
  noteFilePath: String = "",
  zimReaderSource: ZimReaderSource,
  favicon: String = ""
): NoteListItem {
  return NoteListItem(
    zimId = zimId,
    zimUrl = zimUrl,
    title = title,
    url = url,
    noteFilePath = noteFilePath,
    zimReaderSource = zimReaderSource,
    favicon = favicon
  )
}

fun bookmarkState(
  bookmarks: List<LibkiwixBookmarkItem> = emptyList(),
  showAll: Boolean = true,
  zimId: String = "id",
  searchTerm: String = ""
): BookmarkState = BookmarkState(bookmarks, showAll, zimId, searchTerm)

fun pageState(): TestablePageState = TestablePageState()
