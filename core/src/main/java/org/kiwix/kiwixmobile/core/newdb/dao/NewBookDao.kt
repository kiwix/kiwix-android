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
package org.kiwix.kiwixmobile.core.newdb.dao

import io.objectbox.Box
import io.objectbox.kotlin.inValues
import io.objectbox.kotlin.query
import org.kiwix.kiwixmobile.core.data.local.entity.Bookmark
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.newdb.entities.BookOnDiskEntity
import org.kiwix.kiwixmobile.core.newdb.entities.BookOnDiskEntity_
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
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
      val uniqueBooks = uniqueBooksByFile(booksOnDisk)
      removeEntriesWithMatchingIds(uniqueBooks)
      box.put(uniqueBooks.distinctBy { it.book.id }.map(::BookOnDiskEntity))
    }
  }

  private fun uniqueBooksByFile(booksOnDisk: List<BookOnDisk>): List<BookOnDisk> {
    val booksThatPointToSameLocation = box
      .query {
        inValues(BookOnDiskEntity_.file, booksOnDisk.map { it.file.path }.toTypedArray())
      }.find().map(::BookOnDisk)
    return booksOnDisk.filter { bookOnDisk: BookOnDisk ->
      booksThatPointToSameLocation.find { it.file.path == bookOnDisk.file.path } == null
    }
  }

  private fun removeEntriesWithMatchingIds(newBooks: List<BookOnDisk>) {
    box
      .query {
        inValues(BookOnDiskEntity_.bookId, newBooks.map { it.book.id }.toTypedArray())
      }
      .remove()
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

  fun getFavIconAndZimFile(it: Bookmark): Pair<String?, String?> {
    val bookOnDiskEntity = box.query {
      equal(BookOnDiskEntity_.bookId, it.zimId)
    }.find().getOrNull(0)
    return bookOnDiskEntity?.let { Pair(it.favIcon, it.file.path) } ?: Pair(null, null)
  }
}
