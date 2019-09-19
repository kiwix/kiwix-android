package org.kiwix.kiwixmobile.data;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.util.List;
import org.kiwix.kiwixmobile.bookmark.BookmarkItem;
import org.kiwix.kiwixmobile.history.HistoryListItem;
import org.kiwix.kiwixmobile.zim_manager.Language;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem;

/**
 * Defines the set of methods which are required to provide the presenter with the requisite data.
 */

public interface DataSource {
  Single<List<BooksOnDiskListItem>> getLanguageCategorizedBooks();

  Completable saveBook(BooksOnDiskListItem.BookOnDisk book);

  Completable saveBooks(List<BooksOnDiskListItem.BookOnDisk> book);

  Completable saveLanguages(List<Language> languages);

  Single<List<HistoryListItem>> getDateCategorizedHistory(boolean showHistoryCurrentBook);

  Completable saveHistory(HistoryListItem.HistoryItem history);

  Completable deleteHistory(List<HistoryListItem> historyList);

  Completable clearHistory();

  Single<List<BookmarkItem>> getBookmarks(boolean showFromCurrentBook);

  Single<List<String>> getCurrentZimBookmarksUrl();

  Completable saveBookmark(BookmarkItem bookmark);

  Completable deleteBookmarks(List<BookmarkItem> bookmarks);

  Completable deleteBookmark(String bookmarkUrl);

  Flowable<List<BooksOnDiskListItem>> booksOnDiskAsListItems();
}
