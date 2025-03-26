/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.zim_manager.fileselect_view

import org.kiwix.kiwixmobile.core.dao.entities.BookOnDiskEntity
import org.kiwix.kiwixmobile.core.dao.entities.DownloadRoomEntity
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag
import java.io.File
import java.util.Locale

sealed class BooksOnDiskListItem {
  var isSelected: Boolean = false
  abstract val id: Long

  data class LanguageItem constructor(
    override val id: Long,
    val text: String
  ) : BooksOnDiskListItem() {
    constructor(locale: Locale) : this(
      locale.language.hashCode().toLong(),
      locale.getDisplayLanguage(locale)
    )
  }

  data class BookOnDisk constructor(
    val databaseId: Long = 0L,
    val book: LibraryNetworkEntity.Book,
    val file: File = File(""),
    val zimReaderSource: ZimReaderSource,
    val tags: List<KiwixTag> = KiwixTag.Companion.from(book.tags),
    override val id: Long = databaseId
  ) : BooksOnDiskListItem() {
    val locale: Locale by lazy {
      Locale(book.language)
    }

    constructor(bookOnDiskEntity: BookOnDiskEntity) : this(
      databaseId = bookOnDiskEntity.id,
      file = bookOnDiskEntity.file,
      book = bookOnDiskEntity.toBook(),
      zimReaderSource = bookOnDiskEntity.zimReaderSource
    )

    constructor(downloadRoomEntity: DownloadRoomEntity) : this(
      book = downloadRoomEntity.toBook(),
      zimReaderSource = ZimReaderSource(File(downloadRoomEntity.file))
    )

    constructor(zimFileReader: ZimFileReader) : this(
      book = zimFileReader.toBook(),
      zimReaderSource = zimFileReader.zimReaderSource
    )
  }
}
