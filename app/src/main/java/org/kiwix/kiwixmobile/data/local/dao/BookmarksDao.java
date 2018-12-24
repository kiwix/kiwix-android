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
package org.kiwix.kiwixmobile.data.local.dao;


import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.Update;

import org.kiwix.kiwixmobile.data.ZimContentProvider;
import org.kiwix.kiwixmobile.data.local.KiwixDatabase;
import org.kiwix.kiwixmobile.data.local.entity.Bookmark;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Dao class for bookmarks.
 */

public class BookmarksDao {
  private final KiwixDatabase kiwixDatabase;

  @Inject
  public BookmarksDao(KiwixDatabase kiwixDatabase) {
    this.kiwixDatabase = kiwixDatabase;
  }

  /**
   * @return Url of the bookmarks from the current Zim file.
   */
  public List<String> getCurrentZimBookmarksUrl() {
    ArrayList<String> bookmarksUrl = new ArrayList<>();
    try (SquidCursor<Bookmark> bookmarkCursor = kiwixDatabase.query(Bookmark.class,
        Query.selectDistinct(Bookmark.BOOKMARK_URL)
            .where(Bookmark.ZIM_ID.eq(ZimContentProvider.getId())
                .or(Bookmark.ZIM_NAME.eq(ZimContentProvider.getName())))
            .orderBy(Bookmark.BOOKMARK_TITLE.asc()))) {
      while (bookmarkCursor.moveToNext()) {
        bookmarksUrl.add(bookmarkCursor.get(Bookmark.BOOKMARK_URL));
      }
    }
    return bookmarksUrl;
  }

  public void saveBookmark(Bookmark bookmark) {
    kiwixDatabase.deleteWhere(Bookmark.class, Bookmark.BOOKMARK_URL.eq(bookmark.getBookmarkUrl())
        .and(Bookmark.ZIM_ID.eq(bookmark.getZimId())));

    kiwixDatabase.persist(new Bookmark()
        .setZimId(bookmark.getZimId())
        .setZimName(bookmark.getZimName())
        .setZimFilePath(bookmark.getZimFilePath())
        .setFavicon(bookmark.getFavicon())
        .setBookmarkUrl(bookmark.getBookmarkUrl())
        .setBookmarkTitle(bookmark.getBookmarkTitle()));
  }

  public List<Bookmark> getBookmarks(boolean fromCurrentBook) {
    ArrayList<Bookmark> bookmarks = new ArrayList<>();
    Query query = Query.select();
    if (fromCurrentBook) {
      query = query.where(Bookmark.ZIM_ID.eq(ZimContentProvider.getId()));
    }
    try (SquidCursor<Bookmark> squidCursor = kiwixDatabase
        .query(Bookmark.class, query.orderBy(Bookmark.BOOKMARK_TITLE.asc()))) {
      while (squidCursor.moveToNext()) {
        Bookmark bookmark = new Bookmark();

        bookmark.setZimId(squidCursor.get(Bookmark.ZIM_ID));
        bookmark.setZimName(squidCursor.get(Bookmark.ZIM_NAME));
        bookmark.setZimFilePath(squidCursor.get(Bookmark.ZIM_FILE_PATH));
        bookmark.setFavicon(squidCursor.get(Bookmark.FAVICON));
        bookmark.setBookmarkTitle(squidCursor.get(Bookmark.BOOKMARK_TITLE));
        bookmark.setBookmarkUrl(squidCursor.get(Bookmark.BOOKMARK_URL));

        bookmarks.add(bookmark);
      }
    }
    return bookmarks;
  }

  public void deleteBookmarks(List<Bookmark> bookmarks) {
    for (Bookmark bookmark : bookmarks) {
      kiwixDatabase.deleteWhere(Bookmark.class, Bookmark.BOOKMARK_URL.eq(bookmark.getBookmarkUrl())
          .and(Bookmark.ZIM_ID.eq(bookmark.getZimId())));
    }
  }

  public void deleteBookmark(Bookmark bookmark) {
    kiwixDatabase.deleteWhere(Bookmark.class, Bookmark.BOOKMARK_URL.eq(bookmark.getBookmarkUrl())
        .and(Bookmark.ZIM_ID.eq(bookmark.getZimId())));
  }

  public void processBookmark(StringOperation operation) {
    try (SquidCursor<Bookmark> bookmarkCursor = kiwixDatabase.query(Bookmark.class,
        Query.select(Bookmark.ID, Bookmark.BOOKMARK_URL))) {
      while (bookmarkCursor.moveToNext()) {
        String url = bookmarkCursor.get(Bookmark.BOOKMARK_URL);
        url = operation.apply(url);
        if (url != null) {
          kiwixDatabase.update(Update.table(Bookmark.TABLE)
              .where(Bookmark.ID.eq(bookmarkCursor.get(Bookmark.ID)))
              .set(Bookmark.BOOKMARK_URL, url));
        }
      }
    }
  }

  public interface StringOperation {
    String apply(String string);
  }
}
