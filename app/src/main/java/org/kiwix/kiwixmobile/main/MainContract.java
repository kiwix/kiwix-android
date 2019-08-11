package org.kiwix.kiwixmobile.main;

import java.util.List;
import org.kiwix.kiwixmobile.base.BaseContract;
import org.kiwix.kiwixmobile.bookmark.BookmarkItem;
import org.kiwix.kiwixmobile.history.HistoryListItem;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem;

/**
 * The contract between {@link MainActivity} and {@link MainPresenter}.
 */

public class MainContract {

  public interface View extends BaseContract.View<Presenter> {
    void addBooks(List<BooksOnDiskListItem> books);

    void refreshBookmarksUrl(List<String> urls);
  }

  public interface Presenter extends BaseContract.Presenter<View> {
    void loadBooks();

    void saveBooks(List<BooksOnDiskListItem.BookOnDisk> books);

    void saveHistory(HistoryListItem.HistoryItem history);

    void loadCurrentZimBookmarksUrl();

    void saveBookmark(BookmarkItem bookmark);

    void deleteBookmark(BookmarkItem bookmark);
  }
}
