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
package org.kiwix.kiwixmobile.database;


import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import org.kiwix.kiwixmobile.database.entity.Bookmarks;

import java.util.ArrayList;

/**
 * Dao class for bookmarks.
 */

public class BookmarksDao {
  private KiwixDatabase mDb;


  public BookmarksDao(KiwixDatabase kiwikDatabase) {
    this.mDb = kiwikDatabase;
  }

  public ArrayList<String> getBookmarks(String ZimId, String ZimName) {
    SquidCursor<Bookmarks> bookmarkCursor = mDb.query(
        Bookmarks.class,
        Query.selectDistinct(Bookmarks.BOOKMARK_URL).where(Bookmarks.ZIM_ID.eq(ZimId).or(Bookmarks.ZIM_NAME.eq(ZimName)))
            .orderBy(Bookmarks.BOOKMARK_TITLE.asc()));
    ArrayList<String> result = new ArrayList<>();
    try {
      while (bookmarkCursor.moveToNext()) {
        result.add(bookmarkCursor.get(Bookmarks.BOOKMARK_URL));
      }
    } finally {
      bookmarkCursor.close();
    }
    return result;
  }

  public ArrayList<String> getBookmarkTitles(String ZimId, String ZimName) {
    SquidCursor<Bookmarks> bookmarkCursor = mDb.query(
        Bookmarks.class,
        Query.selectDistinct(Bookmarks.BOOKMARK_TITLE).where(Bookmarks.ZIM_ID.eq(ZimId).or(Bookmarks.ZIM_NAME.eq(ZimName)))
            .orderBy(Bookmarks.BOOKMARK_TITLE.asc()));
    ArrayList<String> result = new ArrayList<>();
    try {
      while (bookmarkCursor.moveToNext()) {
        result.add(bookmarkCursor.get(Bookmarks.BOOKMARK_TITLE));
      }
    } finally {
      bookmarkCursor.close();
    }


    return result;
  }

  /**
   * Save bookmark by:
   * @param articleUrl
   * @param articleTitle
   * @param ZimId
     */
  public void saveBookmark(String articleUrl, String articleTitle, String ZimId, String ZimName) {
    if (articleUrl != null) {
      mDb.persist(new Bookmarks().setBookmarkUrl(articleUrl).setBookmarkTitle(articleTitle).setZimId(ZimId).setZimName(ZimName));
    } else {
      mDb.persist(new Bookmarks().setBookmarkUrl("null").setBookmarkTitle(articleTitle).setZimId(ZimId).setZimName(ZimName));
    }
  }

  /**
   * Delete bookmark satisfying:
   * @param favArticle - the article url
   * @param ZimId - zim containing article
     */
  public void deleteBookmark(String favArticle, String ZimId, String ZimName) {
    mDb.deleteWhere(Bookmarks.class, Bookmarks.BOOKMARK_URL.eq(favArticle).and(Bookmarks.ZIM_ID.eq(ZimId).or(Bookmarks.ZIM_NAME.eq(ZimName))) );
  }

}
