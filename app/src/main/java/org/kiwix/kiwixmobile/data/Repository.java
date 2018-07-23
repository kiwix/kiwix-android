package org.kiwix.kiwixmobile.data;

import org.kiwix.kiwixmobile.data.local.dao.BookDao;
import org.kiwix.kiwixmobile.data.local.dao.BookmarksDao;
import org.kiwix.kiwixmobile.data.local.dao.HistoryDao;
import org.kiwix.kiwixmobile.data.local.dao.NetworkLanguageDao;
import org.kiwix.kiwixmobile.data.local.dao.RecentSearchDao;
import org.kiwix.kiwixmobile.data.local.entity.Bookmark;
import org.kiwix.kiwixmobile.data.local.entity.History;
import org.kiwix.kiwixmobile.di.qualifiers.IO;
import org.kiwix.kiwixmobile.di.qualifiers.MainThread;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.models.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;

/**
 * A central repository of data which should provide the presenters with the required data.
 */

@Singleton
public class Repository implements DataSource {

  private final BookDao bookDao;
  private final BookmarksDao bookmarksDao;
  private final HistoryDao historyDao;
  private final NetworkLanguageDao languageDao;
  private final RecentSearchDao recentSearchDao;
  private final Scheduler io;
  private final Scheduler mainThread;

  @Inject
  Repository(@IO Scheduler io, @MainThread Scheduler mainThread,
             BookDao bookDao,
             BookmarksDao bookmarksDao,
             HistoryDao historyDao,
             NetworkLanguageDao languageDao,
             RecentSearchDao recentSearchDao) {
    this.io = io;
    this.mainThread = mainThread;
    this.bookDao = bookDao;
    this.languageDao = languageDao;
    this.bookmarksDao = bookmarksDao;
    this.historyDao = historyDao;
    this.recentSearchDao = recentSearchDao;
  }

  @Override
  public Single<List<LibraryNetworkEntity.Book>> getLanguageCategorizedBooks() {
    return Observable.fromIterable(bookDao.getBooks())
        .toSortedList((book1, book2) -> book1.getLanguage().compareToIgnoreCase(book2.getLanguage()) == 0 ?
            book1.getTitle().compareToIgnoreCase(book2.getTitle()) :
            book1.getLanguage().compareToIgnoreCase(book2.getLanguage()))
        .map(books -> {
          LibraryNetworkEntity.Book book = null;
          if (books.size() >= 1) {
            book = books.get(0);
            books.add(0, null);
          }
          for (int position = 2; position < books.size(); position++) {

            if (book != null && books.get(position) != null &&
                !new Locale(books.get(position).getLanguage()).getDisplayName()
                    .equalsIgnoreCase(new Locale(book.getLanguage()).getDisplayName())) {
              books.add(position, null);
            }
            book = books.get(position);
          }
          return books;
        })
        .subscribeOn(io)
        .observeOn(mainThread);
  }

  @Override
  public Completable saveBooks(List<LibraryNetworkEntity.Book> books) {
    return Completable.fromAction(() -> bookDao.saveBooks((ArrayList<LibraryNetworkEntity.Book>) books))
        .subscribeOn(io);
  }

  @Override
  public Completable saveBook(LibraryNetworkEntity.Book book) {
    return Completable.fromAction(() -> bookDao.saveBook(book))
        .subscribeOn(io);
  }

  @Override
  public Completable deleteBook(LibraryNetworkEntity.Book book) {
    return Completable.fromAction(() -> bookDao.deleteBook(book.getId()))
        .subscribeOn(io);
  }

  @Override
  public Completable saveLanguages(List<Language> languages) {
    return Completable.fromAction(() -> languageDao.saveFilteredLanguages(languages))
        .subscribeOn(io);
  }

  @Override
  public Single<List<History>> getDateCategorizedHistory(boolean showHistoryCurrentBook) {
    return Single.just(historyDao.getHistoryList(showHistoryCurrentBook))
        .map(histories -> {
          History history = null;
          if (histories.size() >= 1) {
            history = histories.get(0);
            histories.add(0, null);
          }
          for (int position = 2; position < histories.size(); position++) {
            if (history != null && histories.get(position) != null &&
                !history.getDate().equals(histories.get(position).getDate())) {
              histories.add(position, null);
            }
            history = histories.get(position);
          }
          return histories;
        })
        .subscribeOn(io)
        .observeOn(mainThread);
  }

  @Override
  public Completable saveHistory(History history) {
    return Completable.fromAction(() -> historyDao.saveHistory(history))
        .subscribeOn(io);
  }

  @Override
  public Completable deleteHistory(List<History> historyList) {
    return Completable.fromAction(() -> historyDao.deleteHistory(historyList))
        .subscribeOn(io);
  }

  @Override
  public Completable clearHistory() {
    return Completable.fromAction(() -> {
      historyDao.deleteHistory(historyDao.getHistoryList(false));
      recentSearchDao.deleteSearchHistory();
    })
        .subscribeOn(io);
  }

  @Override
  public Single<List<Bookmark>> getBookmarks(boolean fromCurrentBook) {
    return Single.just(bookmarksDao.getBookmarks(fromCurrentBook))
        .subscribeOn(io)
        .observeOn(mainThread);
  }

  @Override
  public Single<List<String>> getCurrentZimBookmarksUrl() {
    return Single.just(bookmarksDao.getCurrentZimBookmarksUrl())
        .subscribeOn(io)
        .observeOn(mainThread);
  }

  @Override
  public Completable saveBookmark(Bookmark bookmark) {
    return Completable.fromAction(() -> bookmarksDao.saveBookmark(bookmark))
        .subscribeOn(io);
  }

  @Override
  public Completable deleteBookmarks(List<Bookmark> bookmarks) {
    return Completable.fromAction(() -> bookmarksDao.deleteBookmarks(bookmarks))
        .subscribeOn(io);
  }

  @Override
  public Completable deleteBookmark(Bookmark bookmark) {
    return Completable.fromAction(() -> bookmarksDao.deleteBookmark(bookmark))
        .subscribeOn(io);
  }
}
