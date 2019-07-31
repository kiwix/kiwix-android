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
package org.kiwix.kiwixmobile.database.newdb.entities

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import org.kiwix.kiwixmobile.bookmark.BookmarkItem

@Entity
data class BookmarkEntity(
  @Id var id: Long = 0,
  val zimId: String,
  var zimName: String,
  var zimFilePath: String,
  var bookmarkUrl: String,
  var bookmarkTitle: String,
  var favicon: String
) {
  constructor(item: BookmarkItem) : this(
    item.databaseId,
    item.zimId,
    item.zimName,
    item.zimFilePath,
    item.bookmarkUrl,
    item.bookmarkTitle,
    item.favicon
  )
}
