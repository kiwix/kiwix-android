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

import android.net.Uri
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.converter.PropertyConverter
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import java.io.File

@Entity
data class BookOnDiskEntity(
  @Id var id: Long = 0,
  @Convert(converter = StringToFileConverter::class, dbType = String::class)
  val file: String = "",
  val bookId: String,
  val title: String,
  val description: String?,
  val language: String,
  val creator: String,
  val publisher: String,
  val date: String,
  val url: String?,
  val articleCount: String?,
  val mediaCount: String?,
  val size: String,
  val name: String?,
  val favIcon: String,
  val tags: String? = null
) {
  constructor(bookOnDisk: BookOnDisk) : this(
    0,
    bookOnDisk.file,
    bookOnDisk.book.id,
    bookOnDisk.book.title,
    bookOnDisk.book.description,
    bookOnDisk.book.language,
    bookOnDisk.book.creator,
    bookOnDisk.book.publisher,
    bookOnDisk.book.date,
    bookOnDisk.book.url,
    bookOnDisk.book.articleCount,
    bookOnDisk.book.mediaCount,
    bookOnDisk.book.size,
    bookOnDisk.book.bookName,
    bookOnDisk.book.favicon,
    bookOnDisk.book.tags
  )

  fun toBook() = Book().apply {
    id = bookId
    title = this@BookOnDiskEntity.title
    description = this@BookOnDiskEntity.description
    language = this@BookOnDiskEntity.language
    creator = this@BookOnDiskEntity.creator
    publisher = this@BookOnDiskEntity.publisher
    date = this@BookOnDiskEntity.date
    url = this@BookOnDiskEntity.url
    articleCount = this@BookOnDiskEntity.articleCount
    mediaCount = this@BookOnDiskEntity.mediaCount
    size = this@BookOnDiskEntity.size
    bookName = name
    favicon = favIcon
    tags = this@BookOnDiskEntity.tags
  }
}

class StringToFileConverter : PropertyConverter<String, String> {
  override fun convertToDatabaseValue(entityProperty: String?) = entityProperty

  override fun convertToEntityProperty(databaseValue: String?) = databaseValue
}
