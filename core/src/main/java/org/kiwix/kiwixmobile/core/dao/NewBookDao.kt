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
package org.kiwix.kiwixmobile.core.dao

import android.net.Uri
import io.objectbox.Box
import io.objectbox.kotlin.inValues
import io.objectbox.kotlin.query
import io.objectbox.query.QueryBuilder
import org.kiwix.kiwixmobile.core.dao.entities.BookOnDiskEntity
import org.kiwix.kiwixmobile.core.dao.entities.BookOnDiskEntity_
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
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
    val booksWithSameFilePath = booksWithSameFilePath(booksOnDisk)
    return booksOnDisk.filter { bookOnDisk: BookOnDisk ->
      booksWithSameFilePath.find { it.file.path == bookOnDisk.file.path } == null
    }
  }

  private fun booksWithSameFilePath(booksOnDisk: List<BookOnDisk>) =
    box.query {
      inValues(
        BookOnDiskEntity_.file, booksOnDisk.map { it.file.path }.toTypedArray(),
        QueryBuilder.StringOrder.CASE_INSENSITIVE
      )
    }.find()
      .map(::BookOnDisk)

  private fun removeEntriesWithMatchingIds(uniqueBooks: List<BookOnDisk>) {
    box.query {
      inValues(
        BookOnDiskEntity_.bookId,
        uniqueBooks.map { it.book.id }.toTypedArray(),
        QueryBuilder.StringOrder.CASE_INSENSITIVE
      )
    }
      .remove()
  }

  fun delete(databaseId: Long) {
    box.remove(databaseId)
  }

  @Suppress("UnsafeCallOnNullableType")
  fun migrationInsert(books: List<Book>) {
    insert(books.map { BookOnDisk(book = it, file = it.file!!) })
  }

  private fun removeBooksThatDoNotExist(books: MutableList<BookOnDiskEntity>) {
    delete(books.filterNot { it.file.exists() })
  }

  private fun delete(books: List<BookOnDiskEntity>) {
    box.remove(books)
  }

  fun bookMatching(downloadTitle: String) = box.query {
    endsWith(
      BookOnDiskEntity_.file, downloadTitle,
      QueryBuilder.StringOrder.CASE_INSENSITIVE
    )
  }.findFirst()

  fun bookMatchingUrl(url: Uri): BookOnDiskEntity? {
    url.host?.let {
      it.split(".").let { splittedHost ->
        val list = box.query {
          contains(
            BookOnDiskEntity_.name, if (splittedHost.size > 2) splittedHost[1] else splittedHost[0],
            QueryBuilder.StringOrder.CASE_INSENSITIVE
          )
        }.find()
        list.sortWith(Comparator { book1: BookOnDiskEntity, book2: BookOnDiskEntity ->
          if (book1.language == splittedHost[0] || book1.language == splittedHost[splittedHost.size - 1]
          ) 1 else 0
        })
        return@bookMatchingUrl list.first()
      }
    }
    return null
  }
}
