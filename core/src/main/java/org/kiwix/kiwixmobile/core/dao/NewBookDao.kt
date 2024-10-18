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

import io.objectbox.Box
import io.objectbox.kotlin.inValues
import io.objectbox.kotlin.query
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.rx3.rxSingle
import org.kiwix.kiwixmobile.core.dao.entities.BookOnDiskEntity
import org.kiwix.kiwixmobile.core.dao.entities.BookOnDiskEntity_
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import javax.inject.Inject

class NewBookDao @Inject constructor(private val box: Box<BookOnDiskEntity>) {

  fun books() = box.asFlowable()
    .flatMap { books ->
      io.reactivex.rxjava3.core.Flowable.fromIterable(books)
        .flatMapSingle { bookOnDiskEntity ->
          val file = bookOnDiskEntity.file
          val zimReaderSource = ZimReaderSource(file)

          rxSingle { zimReaderSource.canOpenInLibkiwix() }
            .map { canOpen ->
              if (canOpen) {
                bookOnDiskEntity.zimReaderSource = zimReaderSource
              }
              bookOnDiskEntity
            }
            .onErrorReturn { bookOnDiskEntity }
        }
        .toList()
        .toFlowable()
    }
    .doOnNext { removeBooksThatDoNotExist(it.toMutableList()) }
    .map { books -> books.filter { it.zimReaderSource.exists() } }
    .map { it.map(::BookOnDisk) }

  suspend fun getBooks() = box.all.map { bookOnDiskEntity ->
    bookOnDiskEntity.file.let { file ->
      // set zimReaderSource for previously saved books
      val zimReaderSource = ZimReaderSource(file)
      if (zimReaderSource.canOpenInLibkiwix()) {
        bookOnDiskEntity.zimReaderSource = zimReaderSource
      }
    }
    BookOnDisk(bookOnDiskEntity)
  }

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
      booksWithSameFilePath.find { it.zimReaderSource == bookOnDisk.zimReaderSource } == null
    }
  }

  private fun booksWithSameFilePath(booksOnDisk: List<BookOnDisk>) =
    box.query {
      inValues(
        BookOnDiskEntity_.zimReaderSource,
        booksOnDisk.map { it.zimReaderSource.toDatabase() }.toTypedArray(),
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
    insert(books.map { BookOnDisk(book = it, zimReaderSource = ZimReaderSource(it.file!!)) })
  }

  private fun removeBooksThatDoNotExist(books: MutableList<BookOnDiskEntity>) {
    delete(books.filterNot { it.zimReaderSource.exists() })
  }

  private fun delete(books: List<BookOnDiskEntity>) {
    box.remove(books)
  }

  fun bookMatching(downloadTitle: String) = box.query {
    endsWith(
      BookOnDiskEntity_.zimReaderSource, downloadTitle,
      QueryBuilder.StringOrder.CASE_INSENSITIVE
    )
  }.findFirst()
}
