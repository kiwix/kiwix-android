package org.kiwix.kiwixmobile.data;

import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.models.Language;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * Defines the set of methods which are required to provide the presenter with the requisite data.
 */

public interface DataSource {
  Single<List<LibraryNetworkEntity.Book>> getLanguageCategorizedBooks();

  Completable saveBook(LibraryNetworkEntity.Book book);

  Completable saveBooks(List<LibraryNetworkEntity.Book> book);

  Completable deleteBook(LibraryNetworkEntity.Book book);

  Completable saveLanguages(List<Language> languages);

  Single<List<History>> getDateCategorizedHistory(boolean showHistoryCurrentBook);

  Completable saveHistory(History history);

  Completable deleteHistory(List<History> historyList);

  Completable clearHistory();

  Single<List<Bookmark>> getBookmarks(boolean showFromCurrentBook);

  Single<List<String>> getCurrentZimBookmarksUrl();

  Completable saveBookmark(Bookmark bookmark);

  Completable deleteBookmarks(List<Bookmark> bookmarks);

  Completable deleteBookmark(Bookmark bookmark);
}
