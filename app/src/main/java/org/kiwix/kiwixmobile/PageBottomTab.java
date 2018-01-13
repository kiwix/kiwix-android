package org.kiwix.kiwixmobile;

import android.support.annotation.NonNull;

public enum PageBottomTab {
  HOME() {
    @Override
    public void select(@NonNull Callback cb) {
      cb.onHomeTabSelected();
    }
  },
  FIND_IN_PAGE() {
    @Override
    public void select(@NonNull Callback cb) {
      cb.onFindInPageTabSelected();
    }
  },
  FULL_SCREEN() {
    @Override
    public void select(@NonNull Callback cb) {
      cb.onFullscreenTabSelected();
    }
  },
  RANDOM_ARTICLE() {
    @Override
    public void select(@NonNull Callback cb) {
      cb.onRandomArticleTabSelected();
    }
  },

  BOOKMARK() {
    @Override
    public void select(@NonNull Callback cb) {
      cb.onBookmarkTabSelected();
    }

    @Override
    public void longClick(@NonNull Callback cb) {
      cb.onBookmarkTabLongClicked();
    }
  };


  @NonNull
  public static PageBottomTab of(int code) {
    switch (code) {
      case 0:
        return HOME;
      case 1:
        return FIND_IN_PAGE;
      case 2:
        return FULL_SCREEN;
      case 3:
        return RANDOM_ARTICLE;
      case 4:
        return BOOKMARK;
      default:
        throw new IllegalArgumentException("Tab position is: " + code);
    }
  }

  public abstract void select(@NonNull Callback cb);

  public void longClick(@NonNull Callback cb) {
    // Override me
  }

  public interface Callback {
    void onHomeTabSelected();
    void onFindInPageTabSelected();
    void onFullscreenTabSelected();
    void onRandomArticleTabSelected();
    void onBookmarkTabSelected();
    void onBookmarkTabLongClicked();
  }
}
