/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.core.data

import kotlinx.coroutines.flow.Flow
import org.kiwix.kiwixmobile.core.dao.entities.WebViewHistoryEntity
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.notes.adapter.NoteListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.libkiwix.Book

/**
 * Defines the set of methods which are required to provide the presenter with the requisite data.
 */
interface DataSource {
  fun getLanguageCategorizedBooks(): Flow<List<BooksOnDiskListItem>>
  suspend fun saveBook(book: Book)
  suspend fun saveBooks(book: List<Book>)
  suspend fun saveHistory(history: HistoryItem)
  suspend fun deleteHistory(historyList: List<HistoryListItem>)
  suspend fun clearHistory()
  fun getBookmarks(): Flow<List<LibkiwixBookmarkItem>>
  suspend fun getCurrentZimBookmarksUrl(): List<String>
  suspend fun saveBookmark(libkiwixBookmarkItem: LibkiwixBookmarkItem)
  suspend fun deleteBookmarks(bookmarks: List<LibkiwixBookmarkItem>)
  suspend fun deleteBookmark(bookId: String, bookmarkUrl: String)
  fun booksOnDiskAsListItems(): Flow<List<BooksOnDiskListItem>>
  suspend fun saveNote(noteListItem: NoteListItem)
  suspend fun deleteNote(noteTitle: String)
  suspend fun deleteNotes(noteList: List<NoteListItem>)
  suspend fun insertWebViewPageHistoryItems(webViewHistoryEntityList: List<WebViewHistoryEntity>)
  fun getAllWebViewPagesHistory(): Flow<List<WebViewHistoryEntity>>
  suspend fun clearWebViewPagesHistory()
}
