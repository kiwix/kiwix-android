package org.kiwix.kiwixmobile.zim_manager.library_view;

import org.kiwix.kiwixmobile.base.ViewCallback;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;

import java.util.LinkedList;

/**
 * Created by EladKeyshawn on 06/04/2017.
 */

public interface LibraryViewCallback extends ViewCallback {

  void showBooks(LinkedList<LibraryNetworkEntity.Book> books);

  void displayNoNetworkConnection();

  void displayScanningContent();

  void stopScanningContent();

  void downloadFile(LibraryNetworkEntity.Book book);
}
