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
package org.kiwix.kiwixmobile.database.newdb.dao

import io.objectbox.Box
import io.objectbox.kotlin.query
import org.kiwix.kiwixmobile.data.ZimContentProvider
import org.kiwix.kiwixmobile.database.newdb.entities.RecentSearchEntity
import org.kiwix.kiwixmobile.database.newdb.entities.RecentSearchEntity_
import javax.inject.Inject

class NewRecentSearchDao @Inject constructor(val box: Box<RecentSearchEntity>) {
  fun getRecentSearches() = box
    .query {
      equal(RecentSearchEntity_.zimId, ZimContentProvider.getId() ?: "")
      orderDesc(RecentSearchEntity_.id)
    }
    .find()
    .distinctBy(RecentSearchEntity::searchTerm)
    .take(NUM_RECENT_RESULTS)
    .map(RecentSearchEntity::searchTerm)

  fun saveSearch(title: String) {
    box.put(RecentSearchEntity(title))
  }

  fun deleteSearchString(searchTerm: String) {
    box
      .query {
        equal(RecentSearchEntity_.searchTerm, searchTerm)
      }
      .remove()
  }

  fun deleteSearchHistory() {
    box.removeAll()
  }

  companion object {
    const val NUM_RECENT_RESULTS = 5
  }
}
