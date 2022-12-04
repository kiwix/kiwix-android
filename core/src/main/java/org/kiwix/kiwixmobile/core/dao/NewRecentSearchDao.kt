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
package org.kiwix.kiwixmobile.core.dao

import io.objectbox.Box
import io.objectbox.kotlin.query
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.flow.map
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchEntity
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchEntity_
import org.kiwix.kiwixmobile.core.data.local.entity.RecentSearch
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.RecentSearchListItem
import javax.inject.Inject

@Deprecated(message = "Replaced with Room")
class NewRecentSearchDao @Inject constructor(
  private val box: Box<RecentSearchEntity>,
  private val flowBuilder: FlowBuilder
) {
  fun recentSearches(zimId: String?) = flowBuilder.buildCallbackFlow(
    box.query {
      equal(
        RecentSearchEntity_.zimId,
        zimId ?: "",
        QueryBuilder.StringOrder.CASE_INSENSITIVE
      )
      orderDesc(RecentSearchEntity_.id)
    }
  ).map { searchEntities ->
    searchEntities.distinctBy(RecentSearchEntity::searchTerm)
      .take(NUM_RECENT_RESULTS)
      .map { searchEntity -> RecentSearchListItem(searchEntity.searchTerm) }
  }

  fun saveSearch(title: String, id: String) {
    box.put(RecentSearchEntity(searchTerm = title, zimId = id))
  }

  fun deleteSearchString(searchTerm: String) {
    box
      .query {
        equal(
          RecentSearchEntity_.searchTerm,
          searchTerm,
          QueryBuilder.StringOrder.CASE_INSENSITIVE
        )
      }
      .remove()
  }

  fun deleteSearchHistory() {
    box.removeAll()
  }

  fun migrationInsert(recentSearches: MutableList<RecentSearch>) {
    box.put(recentSearches.map(::RecentSearchEntity))
  }

  companion object {
    private const val NUM_RECENT_RESULTS = 100
  }
}
