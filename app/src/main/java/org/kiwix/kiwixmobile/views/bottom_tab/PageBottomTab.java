/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kiwix.kiwixmobile.views.bottom_tab;

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
