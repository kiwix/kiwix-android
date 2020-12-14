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
package org.kiwix.kiwixmobile.core.page.history.adapter

import org.kiwix.kiwixmobile.core.dao.entities.HistoryEntity
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.adapter.PageRelated
import org.kiwix.kiwixmobile.core.reader.ZimReader
import org.kiwix.kiwixmobile.core.reader.ZimSource

sealed class HistoryListItem : PageRelated {

  data class HistoryItem constructor(
    val databaseId: Long = 0L,
    override val zimId: String,
    val zimName: String,

    override val zimSource: ZimSource,
    override val favicon: String?,
    val historyUrl: String,
    override val title: String,
    val dateString: String,
    val timeStamp: Long,
    override var isSelected: Boolean = false,
    override val id: Long = databaseId,
    override val url: String = historyUrl
  ) : HistoryListItem(), Page {

    constructor(
      url: String,
      title: String,
      dateString: String,
      timeStamp: Long,
      zimReader: ZimReader
    ) : this(
      zimId = zimReader.id,
      zimName = zimReader.name,
      zimSource = zimReader.zimSource,
      favicon = zimReader.favicon,
      historyUrl = url,
      title = title,
      dateString = dateString,
      timeStamp = timeStamp
    )

    constructor(historyEntity: HistoryEntity) : this(
      historyEntity.id,
      historyEntity.zimId,
      historyEntity.zimName,
      historyEntity.zimSource,
      historyEntity.favicon,
      historyEntity.historyUrl,
      historyEntity.historyTitle,
      historyEntity.dateString,
      historyEntity.timeStamp,
      false
    )
  }

  data class DateItem(
    val dateString: String,
    override val id: Long = dateString.hashCode().toLong()
  ) : HistoryListItem()
}
