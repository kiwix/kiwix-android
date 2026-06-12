/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.zimManager.fileselectView.effects

import javax.inject.Inject
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem

data class DeleteFilesUseCase @Inject constructor(
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk,
  private val zimReaderContainer: ZimReaderContainer
) {
  suspend operator fun invoke(
    books: List<BooksOnDiskListItem.BookOnDisk>
  ): Boolean {
    return books.fold(true) { acc, book ->
      acc &&
        deleteBook(book).also {
          if (it && book.zimReaderSource == zimReaderContainer.zimReaderSource) {
            zimReaderContainer.setZimReaderSource(null)
          }
        }
    }
  }

  @Suppress("ReturnCount")
  private suspend fun deleteBook(
    book: BooksOnDiskListItem.BookOnDisk
  ): Boolean {
    val file = book.zimReaderSource.file ?: return false

    FileUtils.deleteZimFile(file.path)

    if (file.isFileExist()) {
      return false
    }

    libkiwixBookOnDisk.delete(book.book.id)
    return true
  }
}
