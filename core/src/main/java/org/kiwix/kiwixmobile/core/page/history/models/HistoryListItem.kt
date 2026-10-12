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
package org.kiwix.kiwixmobile.core.page.history.models

import org.kiwix.kiwixmobile.core.dao.entities.HistoryRoomEntity
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.adapter.PageRelated
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.threeten.bp.LocalDate
import java.util.Calendar

sealed class HistoryListItem : PageRelated {
  data class HistoryItem constructor(
    val databaseId: Long = 0L,
    override val zimId: String,
    val zimName: String,
    override val zimReaderSource: ZimReaderSource?,
    override val favicon: String?,
    val historyUrl: String,
    override val title: String,
    val dateString: String,
    val timeStamp: Long,
    override var isSelected: Boolean = false,
    override val id: Long = databaseId,
    override val url: String = historyUrl
  ) : HistoryListItem(), Page {
    // The day of the visit. It is computed from the timestamp because dateString is
    // formatted in the app language at the time of the visit.
    val date: LocalDate
      get() = Calendar.getInstance().apply { timeInMillis = timeStamp }.let {
        LocalDate.of(it[Calendar.YEAR], it[Calendar.MONTH] + 1, it[Calendar.DAY_OF_MONTH])
      }

    constructor(
      url: String,
      title: String,
      dateString: String,
      timeStamp: Long,
      zimFileReader: ZimFileReader
    ) : this(
      zimId = zimFileReader.id,
      zimName = zimFileReader.name,
      zimReaderSource = zimFileReader.zimReaderSource,
      favicon = zimFileReader.favicon,
      historyUrl = url,
      title = title,
      dateString = dateString,
      timeStamp = timeStamp
    )

    constructor(historyRoomEntity: HistoryRoomEntity) : this(
      historyRoomEntity.id,
      historyRoomEntity.zimId,
      historyRoomEntity.zimName,
      historyRoomEntity.zimReaderSource,
      historyRoomEntity.favicon,
      historyRoomEntity.historyUrl,
      historyRoomEntity.historyTitle,
      historyRoomEntity.dateString,
      historyRoomEntity.timeStamp,
      false
    )
  }

  data class DateItem(
    val date: LocalDate,
    override val id: Long = date.toEpochDay()
  ) : HistoryListItem()
}
