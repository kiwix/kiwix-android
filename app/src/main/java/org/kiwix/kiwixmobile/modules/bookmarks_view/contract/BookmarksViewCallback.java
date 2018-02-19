package org.kiwix.kiwixmobile.modules.bookmarks_view.contract;

import org.kiwix.kiwixmobile.common.base.contract.ViewCallback;

import java.util.ArrayList;

/**
 * Created by EladKeyshawn on 05/04/2017.
 */

public interface BookmarksViewCallback extends ViewCallback {
  void showBookmarks(ArrayList<String> bookmarks, ArrayList<String> bookmarkUrls);

  void popDeleteBookmarksSnackbar();
}
