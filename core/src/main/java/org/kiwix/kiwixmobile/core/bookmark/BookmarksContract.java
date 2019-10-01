package org.kiwix.kiwixmobile.core.bookmark;

import java.util.List;
import org.kiwix.kiwixmobile.core.base.BaseContract;

interface BookmarksContract {
  interface View extends BaseContract.View<Presenter> {
    void updateBookmarksList(List<BookmarkItem> bookmarks);

    void notifyBookmarksListFiltered(List<BookmarkItem> bookmarks);
  }

  interface Presenter extends BaseContract.Presenter<View> {
    void loadBookmarks(boolean showBookmarksCurrentBook);

    void filterBookmarks(List<BookmarkItem> bookmarksList, String newText);

    void deleteBookmarks(List<BookmarkItem> deleteList);
  }
}
