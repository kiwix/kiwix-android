/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import java.io.File

@Entity
data class BookOnDiskRoomEntity(
  @PrimaryKey(autoGenerate = true)
  var id: Long = 0,
  val file: File = File(""),
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
  constructor(bookOnDisk: BooksOnDiskListItem.BookOnDisk) : this(
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

  fun toBook() = LibraryNetworkEntity.Book().apply {
    id = bookId
    title = this@BookOnDiskRoomEntity.title
    description = this@BookOnDiskRoomEntity.description
    language = this@BookOnDiskRoomEntity.language
    creator = this@BookOnDiskRoomEntity.creator
    publisher = this@BookOnDiskRoomEntity.publisher
    date = this@BookOnDiskRoomEntity.date
    url = this@BookOnDiskRoomEntity.url
    articleCount = this@BookOnDiskRoomEntity.articleCount
    mediaCount = this@BookOnDiskRoomEntity.mediaCount
    size = this@BookOnDiskRoomEntity.size
    bookName = name
    favicon = favIcon
    tags = this@BookOnDiskRoomEntity.tags
  }
}
