/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
 *
 */
package org.kiwix.kiwixmobile.data.local.dao;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.Update;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.data.local.KiwixDatabase;
import org.kiwix.kiwixmobile.data.local.entity.Bookmark;

/**
 * Dao class for bookmarks.
 */

@Deprecated
public class BookmarksDao {
  private final KiwixDatabase kiwixDatabase;

  @Inject
  public BookmarksDao(KiwixDatabase kiwixDatabase) {
    this.kiwixDatabase = kiwixDatabase;
  }

  public List<Bookmark> getBookmarks() {
    ArrayList<Bookmark> bookmarks = new ArrayList<>();
    Query query = Query.select();
    try (SquidCursor<Bookmark> squidCursor = kiwixDatabase
      .query(Bookmark.class, query.orderBy(Bookmark.BOOKMARK_TITLE.asc()))) {
      while (squidCursor.moveToNext()) {
        Bookmark bookmark = new Bookmark();
        bookmark.setZimId(squidCursor.get(Bookmark.ZIM_ID));
        bookmark.setZimName(squidCursor.get(Bookmark.ZIM_NAME));
        bookmark.setBookmarkTitle(squidCursor.get(Bookmark.BOOKMARK_TITLE));
        bookmark.setBookmarkUrl(squidCursor.get(Bookmark.BOOKMARK_URL));
        bookmarks.add(bookmark);
      }
    }
    return bookmarks;
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
