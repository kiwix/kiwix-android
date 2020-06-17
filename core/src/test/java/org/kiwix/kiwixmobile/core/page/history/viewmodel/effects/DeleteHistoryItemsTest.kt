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

package org.kiwix.kiwixmobile.core.page.history.viewmodel.effects

import androidx.appcompat.app.AppCompatActivity
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.HistoryDao

internal class DeleteHistoryItemsTest {
  private val historyDao: HistoryDao = mockk()
  val activity: AppCompatActivity = mockk()
  private val item1 = historyItem()
  private val item2 = historyItem()

  @Test
  fun `delete with selected items only deletes the selected items`() {
    item1.isSelected = true
    DeleteHistoryItems(historyState(listOf(item1, item2)), historyDao).invokeWith(activity)
    verify { historyDao.deleteHistory(listOf(item1)) }
  }

  @Test
  fun `delete with no selected items deletes all items`() {
    item1.isSelected = false
    DeleteHistoryItems(historyState(listOf(item1, item2)), historyDao).invokeWith(activity)
    verify { historyDao.deleteHistory(listOf(item1, item2)) }
  }
}
