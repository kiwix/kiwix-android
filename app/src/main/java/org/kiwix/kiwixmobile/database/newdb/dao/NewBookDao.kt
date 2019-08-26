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
package org.kiwix.kiwixmobile.database.newdb.dao

import io.objectbox.Box
import io.objectbox.kotlin.inValues
import io.objectbox.kotlin.query
import org.kiwix.kiwixmobile.database.newdb.entities.BookOnDiskEntity
import org.kiwix.kiwixmobile.database.newdb.entities.BookOnDiskEntity_
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import java.util.ArrayList
import javax.inject.Inject

class NewBookDao @Inject constructor(private val box: Box<BookOnDiskEntity>) {

  fun books() = box.asFlowable()
    .doOnNext(::removeBooksThatDoNotExist)
    .map { books -> books.filter { it.file.exists() } }
    .map { it.map(::BookOnDisk) }

  fun getBooks() = box.all.map(::BookOnDisk)

  fun insert(booksOnDisk: List<BookOnDisk>) {
    box.store.callInTx {
      box
        .query {
          inValues(BookOnDiskEntity_.bookId, booksOnDisk.map { it.book.id }.toTypedArray())
        }
        .remove()
      box.put(booksOnDisk.distinctBy { it.book.id }.map(::BookOnDiskEntity))
    }
  }

  fun delete(databaseId: Long) {
    box.remove(databaseId)
  }

  fun migrationInsert(books: ArrayList<Book>) {
    insert(books.map { BookOnDisk(book = it, file = it.file) })
  }

  private fun removeBooksThatDoNotExist(books: MutableList<BookOnDiskEntity>) {
    delete(books.filterNot { it.file.exists() })
  }

  private fun delete(books: List<BookOnDiskEntity>) {
    box.remove(books)
  }
}
