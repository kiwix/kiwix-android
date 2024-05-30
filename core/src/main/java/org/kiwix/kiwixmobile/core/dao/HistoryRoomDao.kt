/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.kiwix.kiwixmobile.core.dao.entities.HistoryRoomEntity
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem

@Dao
abstract class HistoryRoomDao : PageRoomDao {
  @Query("SELECT * FROM HistoryRoomEntity ORDER BY HistoryRoomEntity.timeStamp DESC")
  abstract fun historyRoomEntity(): Flow<List<HistoryRoomEntity>>

  fun history(): Flow<List<Page>> = historyRoomEntity().map {
    it.map(HistoryListItem::HistoryItem)
  }

  override fun pages() = history()
  override fun deletePages(pagesToDelete: List<Page>) =
    deleteHistory(pagesToDelete as List<HistoryListItem.HistoryItem>)

  @Query("SELECT * FROM HistoryRoomEntity WHERE historyUrl LIKE :url AND dateString LIKE :date")
  abstract fun getHistoryRoomEntity(url: String, date: String): HistoryRoomEntity?

  @Update
  abstract fun updateHistoryItem(historyRoomEntity: HistoryRoomEntity)

  @Insert
  abstract fun insertHistoryItem(historyRoomEntity: HistoryRoomEntity)

  fun saveHistory(historyItem: HistoryListItem.HistoryItem) {
    getHistoryRoomEntity(
      historyItem.historyUrl,
      historyItem.dateString
    )?.let(::updateHistoryItem) ?: run {
      insertHistoryItem(HistoryRoomEntity(historyItem))
    }
  }

  fun deleteHistory(historyList: List<HistoryListItem.HistoryItem>) {
    deleteHistoryList(historyList.map(::HistoryRoomEntity))
  }

  @Delete
  abstract fun deleteHistoryList(historyList: List<HistoryRoomEntity>)

  @Query("DELETE FROM HistoryRoomEntity")
  abstract fun deleteAllHistory()
}

class HistoryRoomDaoCoverts {
  @TypeConverter
  fun fromHistoryRoomEntity(historyRoomEntity: HistoryRoomEntity): HistoryListItem =
    HistoryListItem.HistoryItem(historyRoomEntity)

  @TypeConverter
  fun historyItemToHistoryListItem(historyItem: HistoryListItem.HistoryItem): HistoryRoomEntity =
    HistoryRoomEntity(historyItem)
}
