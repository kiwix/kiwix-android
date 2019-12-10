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
package org.kiwix.kiwixmobile.core.data.local.dao;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.core.data.local.KiwixDatabase;
import org.kiwix.kiwixmobile.core.data.local.entity.RecentSearch;

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
  public List<RecentSearch> getRecentSearches() {
    List<RecentSearch> result = new ArrayList<>();
    try (SquidCursor<RecentSearch> searchCursor = mDb.query(
      RecentSearch.class, Query.select())) {
      while (searchCursor.moveToNext()) {
        result.add(new RecentSearch(searchCursor));
      }
    }
    return result;
  }
}
