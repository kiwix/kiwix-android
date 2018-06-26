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
  }

  interface Presenter extends BaseContract.Presenter<View> {
    void showHome();

    void saveBooks(List<LibraryNetworkEntity.Book> books);

    void saveHistory(String file, String favicon, String url, String title, long time);
  }
}