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
package org.kiwix.kiwixmobile.core.data.local.dao

import com.yahoo.squidb.sql.Query
import org.kiwix.kiwixmobile.core.data.local.KiwixDatabase
import org.kiwix.kiwixmobile.core.data.local.entity.RecentSearch
import java.util.ArrayList
import javax.inject.Inject

/**
 * Dao class for recent searches.
 */
@Deprecated("")
class RecentSearchDao @Inject constructor(private val mDb: KiwixDatabase) {
  /**
   * Returns a distinct enumeration of the `NUM_RECENT_RESULTS` most recent searches.
   */
  fun getRecentSearches(): MutableList<RecentSearch> {
    val result: MutableList<RecentSearch> = ArrayList()
    mDb.query(
      RecentSearch::class.java, Query.select()
    ).use { searchCursor ->
      while (searchCursor.moveToNext()) {
        result.add(RecentSearch(searchCursor))
      }
    }
    return result
  }
}
