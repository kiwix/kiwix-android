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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import org.kiwix.kiwixmobile.core.dao.entities.BookOnDiskEntity
import org.kiwix.kiwixmobile.core.dao.entities.BookOnDiskEntity_
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import javax.inject.Inject

class NewBookDao @Inject constructor(private val box: Box<BookOnDiskEntity>) {
  @OptIn(ExperimentalCoroutinesApi::class)
  @Suppress("Deprecation")
  fun books(dispatcher: CoroutineDispatcher = Dispatchers.IO) =
    box.asFlow()
      .mapLatest { booksList ->
        val updatedBooks = booksList.onEach { bookOnDiskEntity ->
          val file = bookOnDiskEntity.file
          val zimReaderSource = ZimReaderSource(file)
          try {
            if (zimReaderSource.canOpenInLibkiwix()) {
              bookOnDiskEntity.zimReaderSource = zimReaderSource
            }
          } catch (_: Exception) {
            // Do nothing simply return the bookOnDiskEntity.
          }
          bookOnDiskEntity
        }
        removeBooksThatAreInTrashFolder(updatedBooks)
        removeBooksThatDoNotExist(updatedBooks.toMutableList())

        updatedBooks.mapNotNull { book ->
          try {
            if (book.zimReaderSource.exists() &&
              !isInTrashFolder(book.zimReaderSource.toDatabase())
            ) {
              book
            } else {
              null
            }
          } catch (_: Exception) {
            null
          }
        }
      }
      .map { it.map(::BookOnDisk) }
      .flowOn(dispatcher)

  @Suppress("Deprecation")
  suspend fun getBooks() =
    box.all.map { bookOnDiskEntity ->
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
      booksWithSameFilePath.none { it.zimReaderSource == bookOnDisk.zimReaderSource }
    }
  }

  @Suppress("Deprecation")
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
  fun migrationInsert(books: List<LibkiwixBook>) {
    insert(books.map { BookOnDisk(book = it, zimReaderSource = ZimReaderSource(it.file!!)) })
  }

  private suspend fun removeBooksThatDoNotExist(books: MutableList<BookOnDiskEntity>) {
    delete(books.filterNot { it.zimReaderSource.exists() })
  }

  // Remove the existing books from database which are showing on the library screen.
  private fun removeBooksThatAreInTrashFolder(books: List<BookOnDiskEntity>) {
    delete(books.filter { isInTrashFolder(it.zimReaderSource.toDatabase()) })
  }

  // Check if any existing ZIM file showing on the library screen which is inside the trash folder.
  private fun isInTrashFolder(filePath: String) =
    Regex("/\\.Trash/").containsMatchIn(filePath)

  private fun delete(books: List<BookOnDiskEntity>) {
    box.remove(books)
  }

  fun bookMatching(downloadTitle: String) =
    box.query {
      endsWith(
        BookOnDiskEntity_.zimReaderSource,
        downloadTitle,
        QueryBuilder.StringOrder.CASE_INSENSITIVE
      )
    }.findFirst()
}
