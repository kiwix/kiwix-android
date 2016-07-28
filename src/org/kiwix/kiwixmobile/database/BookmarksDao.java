package org.kiwix.kiwixmobile.database;


import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import org.kiwix.kiwixmobile.database.entity.Bookmarks;
import org.kiwix.kiwixmobile.database.entity.RecentSearch;

import java.util.ArrayList;
import java.util.List;

/**
 * Dao class for bookmarks.
 */

public class BookmarksDao {
  private KiwixDatabase mDb;


  public BookmarksDao(KiwixDatabase kiwikDatabase) {
    this.mDb = kiwikDatabase;
  }


  public ArrayList<String> getBookmarks() {
    SquidCursor<Bookmarks> bookmarkCursor = mDb.query(
        Bookmarks.class,
        Query.selectDistinct(Bookmarks.BOOKMARK_URL)
            .orderBy(Bookmarks.ID.desc()));
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

  public ArrayList<String> getBookmarkTitles() {
    SquidCursor<Bookmarks> bookmarkCursor = mDb.query(
        Bookmarks.class,
        Query.selectDistinct(Bookmarks.BOOKMARK_TITLE)
            .orderBy(Bookmarks.ID.desc()));
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
   * Save {@code searchString} as the most recent search.
   */
  public void saveBookmark(String articleUrl, String articleTitle) {
    mDb.persist(new Bookmarks().setBookmarkUrl(articleUrl).setBookmarkTitle(articleTitle));
  }

  /**
   * Delete all entries that exactly matches {@code searchString}
   */
  public void deleteBookmark(String favArticle) {
    mDb.deleteWhere(Bookmarks.class, Bookmarks.BOOKMARK_URL.eq(favArticle));
  }

  public void saveAll(ArrayList<String> articles){
    for (String article : articles) {
      mDb.persist(new Bookmarks().setBookmarkUrl(article));
    }
  }

  public void deleteAll(){
    mDb.clear();
  }

  public void resetBookmarksToPrevious(ArrayList<String> articles){
    deleteAll();
    saveAll(articles);
  }



}
