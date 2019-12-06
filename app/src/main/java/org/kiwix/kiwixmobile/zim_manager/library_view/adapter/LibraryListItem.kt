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

package org.kiwix.kiwixmobile.zim_manager.library_view.adapter

import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState.NotEnoughSpaceFor4GbFile
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState.Unknown

sealed class LibraryListItem {
  abstract val id: Long

  data class DividerItem constructor(
    override val id: Long,
    val text: String
  ) : LibraryListItem()

  data class BookItem constructor(
    val book: Book,
    val canBeDownloaded: Boolean,
    override val id: Long = book.id.hashCode().toLong()
  ) : LibraryListItem() {
    constructor(book: Book, fileSystemState: FileSystemState) : this(
      book,
      when (fileSystemState) {
        Unknown, CannotWrite4GbFile -> book.isLessThan4GB()
        NotEnoughSpaceFor4GbFile, CanWrite4GbFile -> true
      }
    )

    companion object {
      private fun Book.isLessThan4GB() =
        size.toLongOrNull() ?: 0L < Fat32Checker.FOUR_GIGABYTES_IN_KILOBYTES
    }
  }
}
