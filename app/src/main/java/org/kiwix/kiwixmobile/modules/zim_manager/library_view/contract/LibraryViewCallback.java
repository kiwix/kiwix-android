package org.kiwix.kiwixmobile.modules.zim_manager.library_view.contract;

import org.kiwix.kiwixmobile.common.base.contract.ViewCallback;
import org.kiwix.kiwixmobile.modules.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.modules.library.entity.LibraryNetworkEntity.Book;

import java.util.LinkedList;

/**
 * Created by EladKeyshawn on 06/04/2017.
 */

public interface LibraryViewCallback extends ViewCallback {

  void showBooks(LinkedList<Book> books);

  void displayNoNetworkConnection();

  void displayNoItemsFound();

  void displayNoItemsAvailable();

  void displayScanningContent();

  void stopScanningContent();

  void downloadFile(LibraryNetworkEntity.Book book);
}
