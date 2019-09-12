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
