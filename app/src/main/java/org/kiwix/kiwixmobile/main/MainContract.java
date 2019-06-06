package org.kiwix.kiwixmobile.main;

import java.util.List;
import org.kiwix.kiwixmobile.base.BaseContract;
import org.kiwix.kiwixmobile.data.local.entity.Bookmark;
import org.kiwix.kiwixmobile.data.local.entity.History;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem;

/**
 * The contract between {@link MainActivity} and {@link MainPresenter}.
 */

class MainContract {

  interface View extends BaseContract.View<Presenter> {
    void addBooks(List<BooksOnDiskListItem> books);

    void refreshBookmarksUrl(List<String> urls);
  }

  interface Presenter extends BaseContract.Presenter<View> {
    void showHome();

    void saveBooks(List<LibraryNetworkEntity.Book> books);

    void saveHistory(History history);

    void loadCurrentZimBookmarksUrl();

    void saveBookmark(Bookmark bookmark);

    void deleteBookmark(Bookmark bookmark);
  }
}
