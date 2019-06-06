package org.kiwix.kiwixmobile.data

import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.Single
import org.kiwix.kiwixmobile.data.local.dao.BookmarksDao
import org.kiwix.kiwixmobile.data.local.dao.HistoryDao
import org.kiwix.kiwixmobile.data.local.dao.RecentSearchDao
import org.kiwix.kiwixmobile.data.local.entity.Bookmark
import org.kiwix.kiwixmobile.data.local.entity.History
import org.kiwix.kiwixmobile.database.newdb.dao.NewBookDao
import org.kiwix.kiwixmobile.database.newdb.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.di.qualifiers.IO
import org.kiwix.kiwixmobile.di.qualifiers.MainThread
import org.kiwix.kiwixmobile.downloader.model.BookOnDisk
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.zim_manager.Language
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDiskItem
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.LanguageItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A central repository of data which should provide the presenters with the required data.
 */

@Singleton
class Repository @Inject internal constructor(
  @param:IO private val io: Scheduler, @param:MainThread private val mainThread: Scheduler,
  private val bookDao: NewBookDao,
  private val bookmarksDao: BookmarksDao,
  private val historyDao: HistoryDao,
  private val languageDao: NewLanguagesDao,
  private val recentSearchDao: RecentSearchDao
) : DataSource {

  override fun getLanguageCategorizedBooks() =
    booksOnDiskAsListItems()
        .first(emptyList())
        .subscribeOn(io)
        .observeOn(mainThread)

  override fun booksOnDiskAsListItems() = bookDao.books()
      .map { it.sortedBy { bookOnDisk -> bookOnDisk.book.language + bookOnDisk.book.title } }
      .map(this::foldOverAddingLanguageItems)
      .map { it.toList() }

  private fun foldOverAddingLanguageItems(booksOnDisk: List<BookOnDisk>): MutableList<BooksOnDiskListItem> =
    booksOnDisk.foldIndexed(mutableListOf(),
        { index, acc, currentBook ->
          if (index == 0) {
            acc.add(LanguageItem(currentBook.locale))
          }
          acc.add(BookOnDiskItem(currentBook))
          if (index < booksOnDisk.size - 1) {
            val nextBook = booksOnDisk[index + 1]
            if (currentBook.locale.displayName != nextBook.locale.displayName) {
              acc.add(LanguageItem(nextBook.locale))
            }
          }
          acc
        }
    )

  override fun saveBooks(books: List<LibraryNetworkEntity.Book>): Completable {
    return Completable.fromAction { }
        .subscribeOn(io)
  }

  override fun saveBook(book: LibraryNetworkEntity.Book): Completable {
    return Completable.fromAction { }
        .subscribeOn(io)
  }

  override fun deleteBook(book: LibraryNetworkEntity.Book) = Completable.fromAction { }
      .subscribeOn(io)

  override fun saveLanguages(languages: List<Language>) = Completable.fromAction { }
      .subscribeOn(io)

  override fun getDateCategorizedHistory(showHistoryCurrentBook: Boolean): Single<List<History>> =
    Single.just(historyDao.getHistoryList(showHistoryCurrentBook))
        .map { histories ->
          var history: History? = null
          if (histories.size >= 1) {
            history = histories[0]
            histories.add(0, null)
          }
          for (position in 2 until histories.size) {
            if (history != null && histories[position] != null &&
                history.date != histories[position].date
            ) {
              histories.add(position, null)
            }
            history = histories[position]
          }
          histories
        }
        .subscribeOn(io)
        .observeOn(mainThread)

  override fun saveHistory(history: History) =
    Completable.fromAction { historyDao.saveHistory(history) }
        .subscribeOn(io)

  override fun deleteHistory(historyList: List<History>) =
    Completable.fromAction { historyDao.deleteHistory(historyList) }
        .subscribeOn(io)

  override fun clearHistory() = Completable.fromAction {
    historyDao.deleteHistory(historyDao.getHistoryList(false))
    recentSearchDao.deleteSearchHistory()
  }
      .subscribeOn(io)

  override fun getBookmarks(fromCurrentBook: Boolean): Single<List<Bookmark>> =
    Single.just(bookmarksDao.getBookmarks(fromCurrentBook))
        .subscribeOn(io)
        .observeOn(mainThread)

  override fun getCurrentZimBookmarksUrl() =
    Single.just(bookmarksDao.currentZimBookmarksUrl)
        .subscribeOn(io)
        .observeOn(mainThread)

  override fun saveBookmark(bookmark: Bookmark) =
    Completable.fromAction { bookmarksDao.saveBookmark(bookmark) }
        .subscribeOn(io)

  override fun deleteBookmarks(bookmarks: List<Bookmark>) =
    Completable.fromAction { bookmarksDao.deleteBookmarks(bookmarks) }
        .subscribeOn(io)

  override fun deleteBookmark(bookmark: Bookmark) =
    Completable.fromAction { bookmarksDao.deleteBookmark(bookmark) }
        .subscribeOn(io)
}
