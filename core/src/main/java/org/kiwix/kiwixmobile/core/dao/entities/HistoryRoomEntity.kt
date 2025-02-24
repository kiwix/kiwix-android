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

package org.kiwix.kiwixmobile.core.dao.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import java.io.File

@Entity
data class HistoryRoomEntity(
  @PrimaryKey(autoGenerate = true) var id: Long = 0L,
  val zimId: String,
  val zimName: String,
  val zimFilePath: String?,
  @TypeConverters(ZimSourceRoomConverter::class)
  var zimReaderSource: ZimReaderSource?,
  val favicon: String?,
  var historyUrl: String,
  var historyTitle: String,
  var dateString: String,
  var timeStamp: Long
) {
  constructor(historyItem: HistoryItem) : this(
    historyItem.databaseId,
    historyItem.zimId,
    historyItem.zimName,
    null,
    historyItem.zimReaderSource,
    historyItem.favicon,
    historyItem.historyUrl,
    historyItem.title,
    historyItem.dateString,
    historyItem.timeStamp
  )
}

class ZimSourceRoomConverter {
  @TypeConverter
  fun convertToDatabaseValue(entityProperty: ZimReaderSource?): String =
    entityProperty?.toDatabase().orEmpty()

  @TypeConverter
  fun convertToEntityProperty(databaseValue: String?): ZimReaderSource =
    ZimReaderSource.fromDatabaseValue(databaseValue) ?: ZimReaderSource(File(""))
}
