package org.kiwix.kiwixmobile.bookmarks_view;

import org.kiwix.kiwixmobile.base.ViewCallback;

import java.util.ArrayList;

/**
 * Created by EladKeyshawn on 05/04/2017.
 */

public interface BookmarksViewCallback extends ViewCallback {
  void showBookmarks(ArrayList<String> bookmarks, ArrayList<String> bookmarkUrls);

  void popDeleteBookmarksSnackbar();
}
