/*
 * Copyright (c) 2019 Kiwix
 * All rights reserved.
 */

package org.kiwix.kiwixmobile.webserver;

import java.util.List;
import org.kiwix.kiwixmobile.base.BaseContract;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem;

class ZimHostContract {

  interface View
    extends BaseContract.View<org.kiwix.kiwixmobile.webserver.ZimHostContract.Presenter> {
    void addBooks(List<BooksOnDiskListItem> books);
  }

  interface Presenter
    extends BaseContract.Presenter<org.kiwix.kiwixmobile.webserver.ZimHostContract.View> {
    void loadBooks();
  }
}
