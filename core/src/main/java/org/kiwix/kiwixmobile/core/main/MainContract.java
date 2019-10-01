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

package org.kiwix.kiwixmobile.core.main;

import java.util.List;
import org.kiwix.kiwixmobile.core.base.BaseContract;
import org.kiwix.kiwixmobile.core.bookmark.BookmarkItem;
import org.kiwix.kiwixmobile.core.history.HistoryListItem;
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem;

/**
 * The contract between {@link MainActivity} and {@link MainPresenter}.
 */

class MainContract {

  interface View extends BaseContract.View<Presenter> {
    void addBooks(List<BooksOnDiskListItem> books);

    void refreshBookmarksUrl(List<String> urls);
  }

  interface Presenter extends BaseContract.Presenter<View> {
    void loadBooks();

    void saveBooks(List<BooksOnDiskListItem.BookOnDisk> books);

    void saveHistory(HistoryListItem.HistoryItem history);

    void loadCurrentZimBookmarksUrl();

    void saveBookmark(BookmarkItem bookmark);

    void deleteBookmark(String bookmarkUrl);
  }
}
