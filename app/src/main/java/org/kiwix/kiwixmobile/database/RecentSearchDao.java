package org.kiwix.kiwixmobile.database;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import org.kiwix.kiwixmobile.ZimContentProvider;
import org.kiwix.kiwixmobile.database.entity.RecentSearch;

import java.util.ArrayList;
import java.util.List;

/**
 * Dao class for recent searches.
 */
public class RecentSearchDao {

  private static final int NUM_RECENT_RESULTS = 5;
  private KiwixDatabase mDb;

  public RecentSearchDao(KiwixDatabase kiwixDatabase) {
    this.mDb = kiwixDatabase;
  }

  /**
   * Returns a distinct enumeration of the {@code NUM_RECENT_RESULTS} most recent searches.
   */
  public List<String> getRecentSearches() {
    SquidCursor<RecentSearch> searchCursor = mDb.query(
        RecentSearch.class,
        Query.selectDistinct(RecentSearch.SEARCH_STRING).where(RecentSearch.ZIM_I_D.eq(ZimContentProvider.getId()))
            .orderBy(RecentSearch.ID.desc())
            .limit(NUM_RECENT_RESULTS));
    List<String> result = new ArrayList<>();
    try {
      while (searchCursor.moveToNext()) {
        result.add(searchCursor.get(RecentSearch.SEARCH_STRING));
      }
    } finally {
      searchCursor.close();
    }
    return result;
  }

  /**
   * Save {@code searchString} as the most recent search.
   */
  public void saveSearch(String searchString) {
    mDb.persist(new RecentSearch().setSearchString(searchString).setZimID(ZimContentProvider.getId()));
  }

  /**
   * Delete all entries that exactly matches {@code searchString}
   */
  public void deleteSearchString(String searchString) {
    mDb.deleteWhere(RecentSearch.class, RecentSearch.SEARCH_STRING.eq(searchString));
  }

  /**
   * Deletes all entries.
   */
  public void deleteSearchHistory() {
    mDb.deleteWhere(RecentSearch.class, RecentSearch.ID.isNotNull());
  }
}
