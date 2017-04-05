package org.kiwix.kiwixmobile.BookmarksView;

import android.widget.ArrayAdapter;

import java.util.ArrayList;

/**
 * Created by EladKeyshawn on 05/04/2017.
 */

public interface BookmarksViewCallback extends ViewCallback{
    void showBookmarks(ArrayList<String> bookmarks, ArrayList<String> bookmarkUrls);
    void updateAdapter();

    void popDeleteBookmarksSnackbar();
}
