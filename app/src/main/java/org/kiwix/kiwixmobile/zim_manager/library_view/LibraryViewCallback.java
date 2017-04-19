package org.kiwix.kiwixmobile.zim_manager.library_view;

import java.util.ArrayList;
import org.kiwix.kiwixmobile.base.ViewCallback;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;

import java.util.LinkedList;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;

/**
 * Created by EladKeyshawn on 06/04/2017.
 */

public interface LibraryViewCallback extends ViewCallback {

  void showBooks(LinkedList<Book> books);

  void displayNoNetworkConnection();

  void displayScanningContent();

  void stopScanningContent();

  void downloadFile(LibraryNetworkEntity.Book book);
}
