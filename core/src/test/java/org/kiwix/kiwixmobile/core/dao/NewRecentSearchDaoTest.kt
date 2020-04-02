/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.objectbox.Box
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import io.objectbox.rx.RxQuery
import io.reactivex.Observable
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchEntity
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchEntity_
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.RecentSearchListItem
import org.kiwix.sharedFunctions.recentSearchEntity

internal class NewRecentSearchDaoTest {

  private val box: Box<RecentSearchEntity> = mockk()
  private val newRecentSearchDao = NewRecentSearchDao(box)

  @Nested
  inner class RecentSearchTests {
    @Test
    fun `recentSearches searches by Id passed`() {
      val zimId = "id"
      val queryResult = listOf<RecentSearchEntity>(recentSearchEntity())
      expectFromRecentSearches(queryResult, zimId)
      newRecentSearchDao.recentSearches(zimId).test()
        .assertValues(queryResult.map { RecentSearchListItem(it.searchTerm) })
    }

    @Test
    fun `recentSearches searches with blank Id if null passed`() {
      val queryResult = listOf<RecentSearchEntity>(recentSearchEntity())
      expectFromRecentSearches(queryResult, "")
      newRecentSearchDao.recentSearches(null).test()
        .assertValues(queryResult.map { RecentSearchListItem(it.searchTerm) })
    }

    @Test
    fun `recentSearches searches returns distinct entities by searchTerm`() {
      val queryResult = listOf<RecentSearchEntity>(recentSearchEntity(), recentSearchEntity())
      expectFromRecentSearches(queryResult, "")
      newRecentSearchDao.recentSearches(null).test()
        .assertValues(queryResult.take(1).map { RecentSearchListItem(it.searchTerm) })
    }

    @Test
    fun `recentSearches searches returns a limitedNumber of entities`() {
      val searchResults: List<RecentSearchEntity> =
        (0..101).map { recentSearchEntity(searchTerm = "$it") }
      expectFromRecentSearches(searchResults, "")
      newRecentSearchDao.recentSearches(null).test()
        .assertValues(searchResults.take(100).map { RecentSearchListItem(it.searchTerm) })
    }

    private fun expectFromRecentSearches(queryResult: List<RecentSearchEntity>, zimId: String) {
      val queryBuilder = mockk<QueryBuilder<RecentSearchEntity>>()
      every { box.query() } returns queryBuilder
      every { queryBuilder.equal(RecentSearchEntity_.zimId, zimId) } returns queryBuilder
      every { queryBuilder.orderDesc(RecentSearchEntity_.id) } returns queryBuilder
      val query = mockk<Query<RecentSearchEntity>>()
      every { queryBuilder.build() } returns query
      mockkStatic(RxQuery::class)
      every { RxQuery.observable(query) } returns Observable.just(queryResult)
    }
  }

  @Test
  fun saveSearch() {
  }

  @Test
  fun deleteSearchString() {
  }

  @Test
  fun deleteSearchHistory() {
  }

  @Test
  fun migrationInsert() {
  }
}
