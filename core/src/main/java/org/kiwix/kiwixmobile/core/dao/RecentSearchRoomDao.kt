/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.dao

import androidx.room.Dao
import androidx.room.Query
import io.objectbox.Box
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchEntity
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchRoomEntity
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem

@Dao
abstract class RecentSearchRoomDao {

  @Query(
    "SELECT * FROM RecentSearchRoomEntity WHERE zimId LIKE :zimId ORDER BY" +
      " RecentSearchRoomEntity.id DESC"
  )
  abstract fun search(zimId: String?): Flow<List<RecentSearchRoomEntity>>

  @Query(
    "SELECT * FROM RecentSearchRoomEntity"
  )
  abstract fun fullSearch(): Flow<List<RecentSearchRoomEntity>>

  fun recentSearches(zimId: String? = ""): Flow<List<SearchListItem.RecentSearchListItem>> {
    return if (zimId != "") {
      search(zimId).map { searchEntities ->
        searchEntities.distinctBy(RecentSearchRoomEntity::searchTerm).take(NUM_RECENT_RESULTS)
          .map { searchEntity -> SearchListItem.RecentSearchListItem(searchEntity.searchTerm) }
      }
    } else {
      return fullSearch().map { searchEntities ->
        searchEntities.distinctBy(RecentSearchRoomEntity::searchTerm)
          .take(NUM_RECENT_RESULTS)
          .map { searchEntity ->
            org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.RecentSearchListItem(
              searchEntity.searchTerm
            )
          }
      }
    }
  }

  @Query("INSERT INTO RecentSearchRoomEntity(searchTerm, zimId) VALUES (:title , :zimId)")
  abstract fun saveSearch(title: String, zimId: String)

  @Query("DELETE FROM RecentSearchRoomEntity WHERE searchTerm=:searchTerm")
  abstract fun deleteSearchString(searchTerm: String)

  @Query("DELETE FROM RecentSearchRoomEntity")
  abstract fun deleteSearchHistory()

  fun migrationToRoomInsert(
    box: Box<RecentSearchEntity>
  ) {
    val searchRoomEntityList = box.all
    searchRoomEntityList.forEachIndexed { _, recentSearchEntity ->
      CoroutineScope(Dispatchers.IO).launch {
        saveSearch(recentSearchEntity.searchTerm, recentSearchEntity.zimId)
        // Todo Should we remove object store data now?
      }
    }
  }

  companion object {
    private const val NUM_RECENT_RESULTS = 100
  }
}
