package org.kiwix.kiwixmobile.bookmark;

import java.util.List;
import org.kiwix.kiwixmobile.base.BaseContract;
import org.kiwix.kiwixmobile.data.local.entity.Bookmark;

interface BookmarksContract {
  interface View extends BaseContract.View<Presenter> {
    void updateBookmarksList(List<Bookmark> bookmarks);

    void notifyBookmarksListFiltered(List<Bookmark> bookmarks);
  }

  interface Presenter extends BaseContract.Presenter<View> {
    void loadBookmarks(boolean showBookmarksCurrentBook);

    void filterBookmarks(List<Bookmark> bookmarksList, String newText);

    void deleteBookmarks(List<Bookmark> deleteList);
  }
}
