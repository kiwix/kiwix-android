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
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.data.ZimContentProvider;
import org.kiwix.kiwixmobile.data.local.KiwixDatabase;
import org.kiwix.kiwixmobile.data.local.entity.RecentSearch;

/**
 * Dao class for recent searches.
 */
@Deprecated
public class RecentSearchDao {

  private static final int NUM_RECENT_RESULTS = 5;
  private KiwixDatabase mDb;

  @Inject
  public RecentSearchDao(KiwixDatabase kiwixDatabase) {
    this.mDb = kiwixDatabase;
  }

  /**
   * Returns a distinct enumeration of the {@code NUM_RECENT_RESULTS} most recent searches.
   */
  public List<String> getRecentSearches() {
    SquidCursor<RecentSearch> searchCursor = mDb.query(
      RecentSearch.class,
      Query.selectDistinct(RecentSearch.SEARCH_STRING)
        .where(RecentSearch.ZIM_I_D.eq(ZimContentProvider.getId()))
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
    mDb.persist(
      new RecentSearch().setSearchString(searchString).setZimID(ZimContentProvider.getId()));
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
