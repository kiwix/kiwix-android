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

import androidx.lifecycle.Transformations.map
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.ProvidedTypeConverter
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.Update
import io.objectbox.Box
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.dao.entities.HistoryEntity
import org.kiwix.kiwixmobile.core.dao.entities.HistoryRoomEntity
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem

@Dao
abstract class HistoryRoomDao : BasePageDao {
  @Query("SELECT * FROM HistoryRoomEntity ORDER BY HistoryRoomEntity.timeStamp DESC")
  abstract fun historyRoomEntity(): Flow<List<HistoryRoomEntity>>

  fun history(): Flow<List<Page>> = historyRoomEntity().map {
    it.map(HistoryListItem::HistoryItem)
  }

  // // TODO: After refactoring all the database we should implement [PageDao]
  override fun pages() = history()
  override fun deletePages(pagesToDelete: List<Page>) =
    deleteHistory(pagesToDelete as List<HistoryListItem.HistoryItem>)

  @Query("SELECT * FROM HistoryRoomEntity WHERE historyUrl LIKE :url AND dateString LIKE :date")
  abstract fun getHistoryItem(url: String, date: String): HistoryListItem.HistoryItem

  fun getHistoryItem(historyItem: HistoryListItem.HistoryItem): HistoryListItem.HistoryItem =
    getHistoryItem(historyItem.historyUrl, historyItem.dateString)

  @Update
  abstract fun updateHistoryItem(historyItem: HistoryListItem.HistoryItem)

  fun saveHistory(historyItem: HistoryListItem.HistoryItem) {
    val item = getHistoryItem(historyItem)
    updateHistoryItem(item)
  }

  @Delete
  abstract fun deleteHistory(historyList: List<HistoryListItem.HistoryItem>)

  @Query("DELETE FROM HistoryRoomEntity")
  abstract fun deleteAllHistory()

  fun migrationToRoomHistory(
    box: Box<HistoryEntity>
  ) {
    val historyEntityList = box.all
    historyEntityList.forEachIndexed { _, item ->
      CoroutineScope(Dispatchers.IO).launch {
        // saveHistory(HistoryListItem.HistoryItem(item))
        // Todo Should we remove object store data now?
      }
    }
  }
}

class HistoryRoomDaoCoverts {
  @TypeConverter
  fun fromHistoryRoomEntity(historyRoomEntity: HistoryRoomEntity): HistoryListItem =
    HistoryListItem.HistoryItem(historyRoomEntity)

  @TypeConverter
  fun historyItemToHistoryListItem(historyItem: HistoryListItem.HistoryItem): HistoryRoomEntity =
    HistoryRoomEntity(historyItem)
}
