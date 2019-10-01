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
import org.kiwix.kiwixmobile.core.bookmark.BookmarkItem
import org.kiwix.kiwixmobile.core.di.qualifiers.IO
import org.kiwix.kiwixmobile.core.di.qualifiers.MainThread
import org.kiwix.kiwixmobile.core.history.HistoryListItem
import org.kiwix.kiwixmobile.core.history.HistoryListItem.DateItem
import org.kiwix.kiwixmobile.core.history.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.newdb.dao.HistoryDao
import org.kiwix.kiwixmobile.core.newdb.dao.NewBookDao
import org.kiwix.kiwixmobile.core.newdb.dao.NewBookmarksDao
import org.kiwix.kiwixmobile.core.newdb.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.core.newdb.dao.NewRecentSearchDao
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.core.zim_manager.ZimReaderContainer
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.LanguageItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A central repository of data which should provide the presenters with the required data.
 */

@Singleton
class Repository @Inject internal constructor(
  @param:IO private val io: Scheduler,
  @param:MainThread private val mainThread: Scheduler,
  private val bookDao: NewBookDao,
  private val bookmarksDao: NewBookmarksDao,
  private val historyDao: HistoryDao,
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
      foldOverAddingHeaders(
        it,
        { bookOnDisk -> LanguageItem(bookOnDisk.locale) },
        { current, next -> current.locale.displayName != next.locale.displayName })
    }
    .map { it.toList() }

  override fun saveBooks(books: List<BookOnDisk>) =
    Completable.fromAction { bookDao.insert(books) }
      .subscribeOn(io)

  override fun saveBook(book: BookOnDisk) =
    Completable.fromAction { bookDao.insert(listOf(book)) }
      .subscribeOn(io)

  override fun saveLanguages(languages: List<Language>) =
    Completable.fromAction { languageDao.insert(languages) }
      .subscribeOn(io)

  override fun getDateCategorizedHistory(showHistoryCurrentBook: Boolean) =
    Single.just(
      historyDao.getHistoryList(
        showHistoryCurrentBook,
        zimReaderContainer.zimCanonicalPath
      )
    )
      .map {
        foldOverAddingHeaders(
          it,
          { historyItem -> DateItem(historyItem.dateString) },
          { current, next -> current.dateString != next.dateString })
      }
      .subscribeOn(io)
      .observeOn(mainThread)

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

  override fun getBookmarks(fromCurrentBook: Boolean): Single<List<BookmarkItem>> =
    Single.just(bookmarksDao.getBookmarks(fromCurrentBook, zimReaderContainer.zimFileReader))
      .subscribeOn(io)
      .observeOn(mainThread)

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

  private fun <SUPERTYPE, ITEM : SUPERTYPE, HEADER : SUPERTYPE> foldOverAddingHeaders(
    it: List<ITEM>,
    headerConstructor: (ITEM) -> HEADER,
    criteriaToAddHeader: (ITEM, ITEM) -> Boolean
  ): MutableList<SUPERTYPE> = it.foldIndexed(mutableListOf(),
    { index, acc, currentItem ->
      if (index == 0) {
        acc.add(headerConstructor.invoke(currentItem))
      }
      acc.add(currentItem)
      if (index < it.size - 1) {
        val nextItem = it[index + 1]
        if (criteriaToAddHeader.invoke(currentItem, nextItem)) {
          acc.add(headerConstructor.invoke(nextItem))
        }
      }
      acc
    }
  )
}
