package org.kiwix.kiwixmobile.data

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.Single
import org.kiwix.kiwixmobile.bookmark.BookmarkItem
import org.kiwix.kiwixmobile.database.newdb.dao.HistoryDao
import org.kiwix.kiwixmobile.database.newdb.dao.NewBookDao
import org.kiwix.kiwixmobile.database.newdb.dao.NewBookmarksDao
import org.kiwix.kiwixmobile.database.newdb.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.database.newdb.dao.NewRecentSearchDao
import org.kiwix.kiwixmobile.di.qualifiers.IO
import org.kiwix.kiwixmobile.di.qualifiers.MainThread
import org.kiwix.kiwixmobile.history.HistoryListItem
import org.kiwix.kiwixmobile.history.HistoryListItem.DateItem
import org.kiwix.kiwixmobile.history.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.zim_manager.Language
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.LanguageItem
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
  private val recentSearchDao: NewRecentSearchDao
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
    Single.just(historyDao.getHistoryList(showHistoryCurrentBook))
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
    historyDao.deleteHistory(historyDao.getHistoryList(false))
    recentSearchDao.deleteSearchHistory()
  }

  override fun getBookmarks(fromCurrentBook: Boolean): Single<List<BookmarkItem>> =
    Single.just(bookmarksDao.getBookmarks(fromCurrentBook))
      .subscribeOn(io)
      .observeOn(mainThread)

  override fun getCurrentZimBookmarksUrl() =
    Single.just(bookmarksDao.getCurrentZimBookmarksUrl())
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
