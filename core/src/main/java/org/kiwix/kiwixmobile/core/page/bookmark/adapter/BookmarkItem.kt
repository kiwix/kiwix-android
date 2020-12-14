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

package org.kiwix.kiwixmobile.core.page.bookmark.adapter

import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.reader.ZimReader
import org.kiwix.kiwixmobile.core.reader.ZimSource

data class BookmarkItem(
  val databaseId: Long = 0L,
  override val zimId: String,
  val zimName: String,
  override val zimSource: ZimSource?,
  val bookmarkUrl: String,
  override val title: String,
  override val favicon: String?,
  override var isSelected: Boolean = false,
  override val url: String = bookmarkUrl,
  override val id: Long = databaseId
) : Page {
  constructor(entity: BookmarkEntity) : this(
    entity.id,
    entity.zimId,
    entity.zimName,
    entity.zimSource,
    entity.bookmarkUrl,
    entity.bookmarkTitle,
    entity.favicon
  )

  constructor(
    title: String,
    url: String,
    zimReader: ZimReader
  ) : this(
    zimId = zimReader.id,
    zimName = zimReader.name,
    zimSource = zimReader.zimSource,
    bookmarkUrl = url,
    title = title,
    favicon = zimReader.favicon
  )
}
