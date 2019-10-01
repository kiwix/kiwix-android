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
package org.kiwix.kiwixmobile.core.history

import org.kiwix.kiwixmobile.core.newdb.entities.HistoryEntity
import org.kiwix.kiwixmobile.core.zim_manager.ZimFileReader

sealed class HistoryListItem {
  abstract val id: Long

  data class HistoryItem constructor(
    val databaseId: Long = 0L,
    val zimId: String,
    val zimName: String,
    val zimFilePath: String,
    val favicon: String,
    val historyUrl: String,
    val historyTitle: String,
    val dateString: String,
    val timeStamp: Long,
    override val id: Long = databaseId
  ) : HistoryListItem() {

    constructor(
      url: String,
      title: String,
      dateString: String,
      timeStamp: Long,
      zimFileReader: ZimFileReader
    ) : this(
      zimId = zimFileReader.id,
      zimName = zimFileReader.name,
      zimFilePath = zimFileReader.zimFile.canonicalPath,
      favicon = zimFileReader.favicon,
      historyUrl = url,
      historyTitle = title,
      dateString = dateString,
      timeStamp = timeStamp
    )

    constructor(historyEntity: HistoryEntity) : this(
      historyEntity.id,
      historyEntity.zimId,
      historyEntity.zimName,
      historyEntity.zimFilePath,
      historyEntity.favicon,
      historyEntity.historyUrl,
      historyEntity.historyTitle,
      historyEntity.dateString,
      historyEntity.timeStamp
    )
  }

  data class DateItem(
    val dateString: String,
    override val id: Long = dateString.hashCode().toLong()
  ) : HistoryListItem()
}
