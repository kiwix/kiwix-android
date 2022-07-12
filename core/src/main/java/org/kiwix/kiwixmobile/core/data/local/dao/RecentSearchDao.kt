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

import android.database.sqlite.SQLiteException
import com.yahoo.squidb.sql.Query
import org.kiwix.kiwixmobile.core.data.local.KiwixDatabase
import org.kiwix.kiwixmobile.core.data.local.entity.RecentSearch
import javax.inject.Inject

/**
 * Dao class for recent searches.
 */
class RecentSearchDao @Inject constructor(private val mDb: KiwixDatabase) {

  fun getRecentSearches(): MutableList<RecentSearch> {
    val result: MutableList<RecentSearch> = ArrayList()
    try {
      val searchCursor = mDb.query(
        RecentSearch::class.java, Query.select()
      )
      while (searchCursor.moveToNext()) {
        result.add(RecentSearch(searchCursor))
      }
    } catch (exception: SQLiteException) {
      exception.printStackTrace()
    }

    return result
  }
}
