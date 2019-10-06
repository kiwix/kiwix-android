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

package org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter

import org.kiwix.kiwixmobile.database.newdb.entities.BookOnDiskEntity
import org.kiwix.kiwixmobile.database.newdb.entities.FetchDownloadEntity
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.zim_manager.ZimFileReader
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

  data class BookOnDisk(
    val databaseId: Long = 0L,
    val book: Book,
    val file: File,
    override val id: Long = databaseId
  ) : BooksOnDiskListItem() {

    val locale: Locale by lazy {
      Locale(book.language)
    }

    constructor(bookOnDiskEntity: BookOnDiskEntity) : this(
      bookOnDiskEntity.id,
      bookOnDiskEntity.toBook(),
      bookOnDiskEntity.file
    )

    constructor(fetchDownloadEntity: FetchDownloadEntity) : this(
      book = fetchDownloadEntity.toBook(),
      file = File(fetchDownloadEntity.file)
    )

    constructor(file: File, zimFileReader: ZimFileReader) : this(
      book = zimFileReader.toBook(),
      file = file
    )
  }
}
