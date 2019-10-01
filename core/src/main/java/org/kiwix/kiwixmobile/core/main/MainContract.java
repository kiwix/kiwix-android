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
