/*
 * Kiwix Android
 * Copyright (c) 2021 Kiwix <android.kiwix.org>
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

import io.mockk.CapturingSlot
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.objectbox.Box
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.entities.HistoryEntity
import org.kiwix.kiwixmobile.core.dao.entities.HistoryEntity_
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem
import java.util.concurrent.Callable

internal class HistoryDaoTest {

  private val box: Box<HistoryEntity> = mockk(relaxed = true)
  private val historyDao = HistoryDao(box)

  @AfterEach
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun deletePages() {
    val historyItem: HistoryListItem.HistoryItem = mockk(relaxed = true)
    val historyItemList: List<HistoryListItem.HistoryItem> = listOf(historyItem)
    val pagesToDelete: List<Page> = historyItemList
    historyDao.deletePages(pagesToDelete)
    verify { historyDao.deleteHistory(historyItemList) }
  }

  @Test
  fun saveHistory() {
    val historyItem: HistoryListItem.HistoryItem = mockk(relaxed = true)
    val slot: CapturingSlot<Callable<Unit>> = slot()
    every { box.store.callInTx(capture(slot)) } returns Unit
    val queryBuilder: QueryBuilder<HistoryEntity> = mockk()
    every { box.query() } returns queryBuilder
    every {
      queryBuilder.equal(HistoryEntity_.historyUrl, "")
    } returns queryBuilder
    every { queryBuilder.and() } returns queryBuilder
    every {
      queryBuilder.equal(
        HistoryEntity_.dateString,
        ""
      )
    } returns queryBuilder
    val query: Query<HistoryEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    every { historyItem.historyUrl } returns ""
    every { historyItem.dateString } returns ""
    historyDao.saveHistory(historyItem)
    slot.captured.call()
    verify { query.remove() }
    verify { box.put(HistoryEntity(historyItem)) }
  }

  @Test
  fun deleteAllHistory() {
    historyDao.deleteAllHistory()
    verify { box.removeAll() }
  }
}
