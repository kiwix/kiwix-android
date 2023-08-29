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

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.Single
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.dao.NewBookmarksDao
import org.kiwix.kiwixmobile.core.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.core.dao.NewNoteDao
import org.kiwix.kiwixmobile.core.dao.NewRecentSearchDao
import org.kiwix.kiwixmobile.core.di.qualifiers.IO
import org.kiwix.kiwixmobile.core.di.qualifiers.MainThread
import org.kiwix.kiwixmobile.core.extensions.HeaderizableList
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.BookmarkItem
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.notes.adapter.NoteListItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.LanguageItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A central repository of data which should provide the presenters with the required data.
 */

@Suppress("LongParameterList")
@Singleton
class Repository @Inject internal constructor(
  @param:IO private val io: Scheduler,
  @param:MainThread private val mainThread: Scheduler,
  private val bookDao: NewBookDao,
  private val libkiwixBookmarks: LibkiwixBookmarks,
  private val historyDao: HistoryDao,
  private val notesDao: NewNoteDao,
  private val languageDao: NewLanguagesDao,
  private val recentSearchDao: NewRecentSearchDao,
  private val zimReaderContainer: ZimReaderContainer
) : DataSource {

  override fun getLanguageCategorizedBooks() =
    booksOnDiskAsListItems()
      .first(emptyList())
      .subscribeOn(io)
      .observeOn(mainThread)

  override fun booksOnDiskAsListItems(): Flowable<List<BooksOnDiskListItem>> = bookDao.books()
    .map { it.sortedBy { bookOnDisk -> bookOnDisk.book.language + bookOnDisk.book.title } }
    .map {
      HeaderizableList<BooksOnDiskListItem, BookOnDisk, LanguageItem>(it).foldOverAddingHeaders(
        { bookOnDisk -> LanguageItem(bookOnDisk.locale) },
        { current, next -> current.locale.displayName != next.locale.displayName }
      )
    }
    .map(MutableList<BooksOnDiskListItem>::toList)

  override fun saveBooks(books: List<BookOnDisk>) =
    Completable.fromAction { bookDao.insert(books) }
      .subscribeOn(io)

  override fun saveBook(book: BookOnDisk) =
    Completable.fromAction { bookDao.insert(listOf(book)) }
      .subscribeOn(io)

  override fun saveLanguages(languages: List<Language>) =
    Completable.fromAction { languageDao.insert(languages) }
      .subscribeOn(io)

  override fun saveHistory(history: HistoryItem) =
    Completable.fromAction { historyDao.saveHistory(history) }
      .subscribeOn(io)

  override fun deleteHistory(historyList: List<HistoryListItem>) =
    Completable.fromAction {
      historyDao.deleteHistory(historyList.filterIsInstance(HistoryItem::class.java))
    }
      .subscribeOn(io)

  override fun clearHistory() = Completable.fromAction {
    historyDao.deleteAllHistory()
    recentSearchDao.deleteSearchHistory()
  }

  override fun getBookmarks() = bookmarksDao.bookmarks() as Flowable<List<BookmarkItem>>

  override fun getCurrentZimBookmarksUrl() =
    Single.just(bookmarksDao.getCurrentZimBookmarksUrl(zimReaderContainer.zimFileReader))
      .subscribeOn(io)
      .observeOn(mainThread)

  override fun saveBookmark(bookmark: BookmarkItem) =
    Completable.fromAction { bookmarksDao.saveBookmark(bookmark) }
      .subscribeOn(io)

  override fun deleteBookmarks(bookmarks: List<BookmarkItem>) =
    Completable.fromAction { bookmarksDao.deleteBookmarks(bookmarks) }
      .subscribeOn(io)

  override fun deleteBookmark(bookmarkUrl: String): Completable? =
    Completable.fromAction { bookmarksDao.deleteBookmark(bookmarkUrl) }
      .subscribeOn(io)

  override fun saveNote(noteListItem: NoteListItem): Completable =
    Completable.fromAction { notesDao.saveNote(noteListItem) }
      .subscribeOn(io)

  override fun deleteNotes(noteList: List<NoteListItem>) =
    Completable.fromAction { notesDao.deleteNotes(noteList) }
      .subscribeOn(io)

  override fun deleteNote(noteUniqueKey: String): Completable =
    Completable.fromAction { notesDao.deleteNote(noteUniqueKey) }
      .subscribeOn(io)
}
