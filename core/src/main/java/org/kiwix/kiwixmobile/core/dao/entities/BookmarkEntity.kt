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
package org.kiwix.kiwixmobile.core.dao.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.converter.PropertyConverter
import org.kiwix.kiwixmobile.core.data.local.entity.Bookmark
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.BookmarkItem
import org.kiwix.kiwixmobile.core.reader.ZimSource

@Entity
data class BookmarkEntity(
  @Id var id: Long = 0,
  val zimId: String,
  var zimName: String,
  @Convert(converter = ZimSourceConverter::class, dbType = String::class)
  var zimSource: ZimSource?,
  var bookmarkUrl: String,
  var bookmarkTitle: String,
  var favicon: String?
) {
  constructor(item: BookmarkItem) : this(
    item.databaseId,
    item.zimId,
    item.zimName,
    item.zimSource,
    item.bookmarkUrl,
    item.title,
    item.favicon
  )

  private constructor(bookmark: Bookmark, favicon: String?, zimSource: ZimSource?) : this(
    0,
    bookmark.zimId,
    bookmark.zimName,
    zimSource,
    bookmark.bookmarkUrl,
    bookmark.bookmarkTitle,
    favicon
  )

  constructor(bookmarkWithFavIconAndFile: Pair<Bookmark, Pair<String?, ZimSource?>>) : this(
    bookmarkWithFavIconAndFile.first,
    bookmarkWithFavIconAndFile.second.first,
    bookmarkWithFavIconAndFile.second.second
  )
}

class ZimSourceConverter : PropertyConverter<ZimSource, String> {

  override fun convertToDatabaseValue(entityProperty: ZimSource?) = entityProperty?.toDatabase()

  override fun convertToEntityProperty(databaseValue: String) =
    ZimSource.fromDatabaseValue(databaseValue)
}
