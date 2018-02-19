package org.kiwix.kiwixmobile.modules.zim_manager.fileselect_view.contract;

import org.kiwix.kiwixmobile.common.base.contract.ViewCallback;
import org.kiwix.kiwixmobile.modules.library.entity.LibraryNetworkEntity;

import java.util.ArrayList;

/**
 * Created by EladKeyshawn on 06/04/2017.
 */
public interface ZimFileSelectViewCallback extends ViewCallback {
  void showFiles(ArrayList<LibraryNetworkEntity.Book> books);
}
