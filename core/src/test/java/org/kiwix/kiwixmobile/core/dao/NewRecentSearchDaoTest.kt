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
import io.mockk.verify
import io.objectbox.Box
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchEntity
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchEntity_
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.RecentSearchListItem
import org.kiwix.kiwixmobile.core.search.viewmodel.test
import org.kiwix.sharedFunctions.recentSearchEntity

internal class NewRecentSearchDaoTest {

  private val box: Box<RecentSearchEntity> = mockk(relaxed = true)
  private val flowBuilder: FlowBuilder = mockk()
  private val newRecentSearchDao = NewRecentSearchDao(box, flowBuilder)

  @Nested
  inner class RecentSearchTests {
    @Test
    fun `recentSearches searches by Id passed`() = runBlockingTest {
      val zimId = "id"
      val queryResult = listOf<RecentSearchEntity>(recentSearchEntity())
      expectFromRecentSearches(queryResult, zimId)
      newRecentSearchDao.recentSearches(zimId)
        .test(this)
        .assertValues(
          queryResult.map { RecentSearchListItem(it.searchTerm, it.url) }
        )
        .finish()
    }

    @Test
    fun `recentSearches searches with blank Id if null passed`() = runBlockingTest {
      val queryResult = listOf<RecentSearchEntity>(recentSearchEntity())
      expectFromRecentSearches(queryResult, "")
      newRecentSearchDao.recentSearches(null)
        .test(this)
        .assertValues(
          queryResult.map { RecentSearchListItem(it.searchTerm, it.url) }
        )
        .finish()
    }

    @Test
    fun `recentSearches searches returns distinct entities by searchTerm`() = runBlockingTest {
      val queryResult = listOf<RecentSearchEntity>(recentSearchEntity(), recentSearchEntity())
      expectFromRecentSearches(queryResult, "")
      newRecentSearchDao.recentSearches("")
        .test(this)
        .assertValues(
          queryResult.take(1).map { RecentSearchListItem(it.searchTerm, it.url) }
        )
        .finish()
    }

    @Test
    fun `recentSearches searches returns a limitedNumber of entities`() = runBlockingTest {
      val searchResults: List<RecentSearchEntity> =
        (0..200).map { recentSearchEntity(searchTerm = "$it") }
      expectFromRecentSearches(searchResults, "")
      newRecentSearchDao.recentSearches("")
        .test(this)
        .assertValue { it.size == 100 }
        .finish()
    }

    private fun expectFromRecentSearches(queryResult: List<RecentSearchEntity>, zimId: String) {
      val queryBuilder = mockk<QueryBuilder<RecentSearchEntity>>()
      every { box.query() } returns queryBuilder
      every {
        queryBuilder.equal(
          RecentSearchEntity_.zimId,
          zimId,
          QueryBuilder.StringOrder.CASE_INSENSITIVE
        )
      } returns queryBuilder
      every { queryBuilder.orderDesc(RecentSearchEntity_.id) } returns queryBuilder
      val query = mockk<Query<RecentSearchEntity>>()
      every { queryBuilder.build() } returns query
      every { flowBuilder.buildCallbackFlow(query) } returns flowOf(queryResult)
    }
  }

  @Test
  fun `saveSearch puts RecentSearchEntity into box`() {
    newRecentSearchDao.saveSearch("title", "id", "https://kiwix.app/mainPage")
    verify {
      box.put(
        recentSearchEntity(
          searchTerm = "title",
          zimId = "id",
          url = "https://kiwix.app/mainPage"
        )
      )
    }
  }

  @Test
  fun `deleteSearchString removes query results for the term`() {
    val searchTerm = "searchTerm"
    val queryBuilder: QueryBuilder<RecentSearchEntity> = mockk()
    every { box.query() } returns queryBuilder
    every {
      queryBuilder.equal(
        RecentSearchEntity_.searchTerm,
        searchTerm,
        QueryBuilder.StringOrder.CASE_INSENSITIVE
      )
    } returns queryBuilder
    val query: Query<RecentSearchEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    newRecentSearchDao.deleteSearchString(searchTerm)
    verify { query.remove() }
  }

  @Test
  fun `deleteSearchHistory deletes everything`() {
    newRecentSearchDao.deleteSearchHistory()
    verify { box.removeAll() }
  }
}
