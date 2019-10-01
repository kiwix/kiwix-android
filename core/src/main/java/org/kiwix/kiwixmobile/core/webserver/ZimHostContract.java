package org.kiwix.kiwixmobile.core.webserver;

import java.util.List;
import org.kiwix.kiwixmobile.core.base.BaseContract;
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem;

class ZimHostContract {

  interface View
    extends BaseContract.View<ZimHostContract.Presenter> {
    void addBooks(List<BooksOnDiskListItem> books);
  }

  interface Presenter
    extends BaseContract.Presenter<ZimHostContract.View> {
    void loadBooks();
  }
}
