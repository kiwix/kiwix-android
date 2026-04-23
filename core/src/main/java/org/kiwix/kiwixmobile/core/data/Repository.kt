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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.dao.HistoryRoomDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.dao.NotesRoomDao
import org.kiwix.kiwixmobile.core.dao.RecentSearchRoomDao
import org.kiwix.kiwixmobile.core.dao.WebViewHistoryRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.WebViewHistoryEntity
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.extensions.HeaderizableList
import org.kiwix.kiwixmobile.core.page.bookmark.models.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.page.history.models.HistoryListItem
import org.kiwix.kiwixmobile.core.page.history.models.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.notes.models.NoteListItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.LanguageItem
import org.kiwix.libkiwix.Book
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A central repository of data which should provide the presenters with the required data.
 */

@Singleton
class Repository @Inject internal constructor(
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk,
  private val libkiwixBookmarks: LibkiwixBookmarks,
  private val historyRoomDao: HistoryRoomDao,
  private val webViewHistoryRoomDao: WebViewHistoryRoomDao,
  private val notesRoomDao: NotesRoomDao,
  private val recentSearchRoomDao: RecentSearchRoomDao,
  private val zimReaderContainer: ZimReaderContainer,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DataSource {
  override fun getLanguageCategorizedBooks() =
    booksOnDiskAsListItems()
      .map { it.ifEmpty { emptyList() } }

  override fun booksOnDiskAsListItems(): Flow<List<BooksOnDiskListItem>> =
    libkiwixBookOnDisk.books()
      .map { books ->
        books.flatMap { bookOnDisk ->
          // Split languages if there are multiple, otherwise return the single book. Bug fix #3892
          if (bookOnDisk.book.language.contains(',')) {
            bookOnDisk.book.language.split(',').map { lang ->
              bookOnDisk.copy(book = bookOnDisk.book.copy(_language = lang.trim()))
            }
          } else {
            listOf(bookOnDisk)
          }
        }.distinctBy { it.book.id }
          .sortedBy { it.book.language + it.book.title }
      }
      .map { items ->
        HeaderizableList<BooksOnDiskListItem, BookOnDisk, LanguageItem>(items).foldOverAddingHeaders(
          { bookOnDisk -> LanguageItem(bookOnDisk.locale) },
          { current, next -> current.locale.displayName != next.locale.displayName }
        )
      }
      .map(MutableList<BooksOnDiskListItem>::toList)
      .flowOn(ioDispatcher)

  override suspend fun saveBooks(books: List<Book>) = withContext(ioDispatcher) {
    libkiwixBookOnDisk.insert(books)
  }

  override suspend fun saveBook(book: Book) {
    saveBooks(listOf(book))
  }

  override suspend fun saveHistory(history: HistoryItem) = withContext(ioDispatcher) {
    historyRoomDao.saveHistory(history)
  }

  override suspend fun deleteHistory(historyList: List<HistoryListItem>) =
    withContext(ioDispatcher) {
      historyRoomDao.deleteHistory(historyList.filterIsInstance<HistoryItem>())
    }

  override suspend fun clearHistory() = withContext(ioDispatcher) {
    historyRoomDao.deleteAllHistory()
    recentSearchRoomDao.deleteSearchHistory()
  }

  override fun getBookmarks() =
    libkiwixBookmarks.bookmarks() as Flow<List<LibkiwixBookmarkItem>>

  override suspend fun getCurrentZimBookmarksUrl() =
    libkiwixBookmarks.getCurrentZimBookmarksUrl(zimReaderContainer.zimFileReader)

  override suspend fun saveBookmark(libkiwixBookmarkItem: LibkiwixBookmarkItem) =
    libkiwixBookmarks.saveBookmark(libkiwixBookmarkItem)

  override suspend fun deleteBookmarks(bookmarks: List<LibkiwixBookmarkItem>) =
    libkiwixBookmarks.deleteBookmarks(bookmarks)

  override suspend fun deleteBookmark(bookId: String, bookmarkUrl: String) =
    libkiwixBookmarks.deleteBookmark(bookId, bookmarkUrl)

  override suspend fun saveNote(noteListItem: NoteListItem) =
    withContext(ioDispatcher) {
      notesRoomDao.saveNote(noteListItem)
    }

  override suspend fun clearNotes() =
    withContext(ioDispatcher) {
      val notesList = notesRoomDao.notes().first().map { it as NoteListItem }
      notesRoomDao.deleteNotes(notesList)
    }

  override suspend fun insertWebViewPageHistoryItems(
    webViewHistoryEntityList: List<WebViewHistoryEntity>
  ) {
    webViewHistoryRoomDao.insertWebViewPageHistoryItems(webViewHistoryEntityList)
  }

  override fun getAllWebViewPagesHistory() =
    webViewHistoryRoomDao.getAllWebViewPagesHistory()

  override suspend fun clearWebViewPagesHistory() {
    webViewHistoryRoomDao.clearWebViewPagesHistory()
  }

  override suspend fun deleteNote(noteTitle: String) = withContext(ioDispatcher) {
    notesRoomDao.deleteNote(noteTitle)
  }
}
