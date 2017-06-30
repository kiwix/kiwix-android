package org.kiwix.kiwixmobile.zim_manager.fileselect_view;

import org.kiwix.kiwixmobile.base.ViewCallback;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;

import java.util.ArrayList;

/**
 * Created by EladKeyshawn on 06/04/2017.
 */
public interface ZimFileSelectViewCallback extends ViewCallback {
  void showFiles(ArrayList<LibraryNetworkEntity.Book> books);
}
