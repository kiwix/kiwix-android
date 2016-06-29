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
        Query.selectDistinct(Bookmarks.BOOKMARK_STR)
            .orderBy(Bookmarks.ID.desc()));
    ArrayList<String> result = new ArrayList<>();
    try {
      while (bookmarkCursor.moveToNext()) {
        result.add(bookmarkCursor.get(Bookmarks.BOOKMARK_STR));
      }
    } finally {
      bookmarkCursor.close();
    }
    return result;
  }

  /**
   * Save {@code searchString} as the most recent search.
   */
  public void saveBookmark(String favArticle) {
    mDb.persist(new Bookmarks().setBookmarkStr(favArticle));
  }

  /**
   * Delete all entries that exactly matches {@code searchString}
   */
  public void deleteBookmark(String favArticle) {
    mDb.deleteWhere(Bookmarks.class, Bookmarks.BOOKMARK_STR.eq(favArticle));
  }

  public void saveAll(ArrayList<String> articles){
    for (String article : articles) {
      mDb.persist(new Bookmarks().setBookmarkStr(article));
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
