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

package org.kiwix.kiwixmobile.core.bookmark

import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity
import org.kiwix.kiwixmobile.core.reader.ZimFileReader

data class BookmarkItem(
  val databaseId: Long = 0L,
  val zimId: String,
  val zimName: String,
  val zimFilePath: String?,
  val bookmarkUrl: String,
  val bookmarkTitle: String,
  val favicon: String?
) {
  constructor(entity: BookmarkEntity) : this(
    entity.id,
    entity.zimId,
    entity.zimName,
    entity.zimFilePath,
    entity.bookmarkUrl,
    entity.bookmarkTitle,
    entity.favicon
  )

  constructor(
    title: String,
    url: String,
    zimFileReader: ZimFileReader
  ) : this(
    zimId = zimFileReader.id,
    zimName = zimFileReader.name,
    zimFilePath = zimFileReader.zimFile.canonicalPath,
    bookmarkUrl = url,
    bookmarkTitle = title,
    favicon = zimFileReader.favicon
  )
}
