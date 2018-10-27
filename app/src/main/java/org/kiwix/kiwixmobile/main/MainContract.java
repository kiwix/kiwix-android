package org.kiwix.kiwixmobile.main;

import org.kiwix.kiwixmobile.base.BaseContract;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;

import java.util.List;

/**
 * The contract between {@link MainActivity} and {@link MainPresenter}.
 */

class MainContract {

  interface View extends BaseContract.View<Presenter> {
    void addBooks(List<LibraryNetworkEntity.Book> books);

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