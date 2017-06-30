package org.kiwix.kiwixmobile;

import android.support.annotation.NonNull;

public enum PageBottomTab {
  BOOKMARKS() {
    @Override
    public void select(@NonNull Callback cb) {
      cb.onBookmarksTabSelected();
    }
  },
  FIND_IN_PAGE() {
    @Override
    public void select(@NonNull Callback cb) {
      cb.onFindInPageTabSelected();
    }
  };

  @NonNull
  public static PageBottomTab of(int code) {
    switch (code) {
      case 0:
        return BOOKMARKS;
      case 1:
        return FIND_IN_PAGE;
      default:
        throw new IllegalArgumentException("Tab position is: " + code);
    }
  }

  public abstract void select(@NonNull Callback cb);

  public interface Callback {
    void onBookmarksTabSelected();
    void onFindInPageTabSelected();
  }
}
