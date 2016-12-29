package org.kiwix.kiwixmobile.database;


import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;
import java.util.ArrayList;
import org.kiwix.kiwixmobile.database.entity.Bookmarks;

/**
 * Dao class for bookmarks.
 */

public class BookmarksDao {
  private KiwixDatabase mDb;


  public BookmarksDao(KiwixDatabase kiwikDatabase) {
    this.mDb = kiwikDatabase;
  }

  public ArrayList<String> getBookmarks(String ZimId) {
    SquidCursor<Bookmarks> bookmarkCursor = mDb.query(
        Bookmarks.class,
        Query.selectDistinct(Bookmarks.BOOKMARK_URL).where(Bookmarks.ZIM_ID.eq(ZimId))
            .orderBy(Bookmarks.ID.asc()));
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

  public ArrayList<String> getBookmarkTitles(String ZimId) {
    SquidCursor<Bookmarks> bookmarkCursor = mDb.query(
        Bookmarks.class,
        Query.selectDistinct(Bookmarks.BOOKMARK_TITLE).where(Bookmarks.ZIM_ID.eq(ZimId))
            .orderBy(Bookmarks.ID.asc()));
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
  public void saveBookmark(String articleUrl, String articleTitle, String ZimId) {
    if (articleUrl != null) {
      mDb.persist(new Bookmarks().setBookmarkUrl(articleUrl).setBookmarkTitle(articleTitle).setZimId(ZimId));
    } else {
      mDb.persist(new Bookmarks().setBookmarkUrl("null").setBookmarkTitle(articleTitle).setZimId(ZimId));
    }
  }

  /**
   * Delete bookmark satisfying:
   * @param favArticle - the article url
   * @param ZimId - zim containing article
     */
  public void deleteBookmark(String favArticle, String ZimId) {
    mDb.deleteWhere(Bookmarks.class, Bookmarks.BOOKMARK_URL.eq(favArticle).and(Bookmarks.ZIM_ID.eq(ZimId)) );
  }


  public void deleteAll(){
    mDb.clear();
  }

}
