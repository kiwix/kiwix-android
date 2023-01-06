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

package org.kiwix.kiwixmobile.core.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverter
import io.objectbox.Box
import io.reactivex.Flowable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.dao.entities.BookOnDiskEntity
import org.kiwix.kiwixmobile.core.dao.entities.BookOnDiskRoomEntity
import org.kiwix.kiwixmobile.core.data.local.entity.Bookmark
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import java.io.File

@Dao
abstract class NewBookRoomDao {
  @Query("SELECT * FROM BookOnDiskRoomEntity")
  abstract fun getBookAsEntity(): Flowable<List<BookOnDiskRoomEntity>>

  @Query("SELECT * FROM BookOnDiskRoomEntity")
  abstract fun getBookAsList(): List<BookOnDiskRoomEntity>

  fun getBooks(): List<BooksOnDiskListItem.BookOnDisk> =
    getBookAsList().map(BooksOnDiskListItem::BookOnDisk)

  fun books() = getBookAsEntity()
    .doOnNext(::removeBooksThatDoNotExist)
    .map { books ->
      books.filter {
        it.file.exists()
      }
    }
    .map { it.map(BooksOnDiskListItem::BookOnDisk) }

  @Delete
  abstract fun deleteBooks(books: BookOnDiskRoomEntity)

  @Transaction
  open fun removeBooksThatDoNotExist(books: List<BookOnDiskRoomEntity>) {
    books.filterNot { it.file.exists() }.forEach(::deleteBooks)
  }

  @Insert
  abstract fun insertAsEntity(bookOnDiskRoomEntity: BookOnDiskRoomEntity)

  @Transaction
  open fun insert(booksOnDisks: List<BooksOnDiskListItem.BookOnDisk>) {
    val uniqueBooks = uniqueBooksByFile(booksOnDisks)
    removeEntriesWithMatchingIds(uniqueBooks)
    uniqueBooks.distinctBy { it.book.id }.map {
      val bookOnDiskRoomEntity = BookOnDiskRoomEntity(it)
      insertAsEntity(bookOnDiskRoomEntity)
    }
  }

  private fun uniqueBooksByFile(booksOnDisk: List<BooksOnDiskListItem.BookOnDisk>):
    List<BooksOnDiskListItem.BookOnDisk> {
    val booksWithSameFilePath = booksWithSameFilePath(booksOnDisk)
    return booksOnDisk.filter { bookOnDisk: BooksOnDiskListItem.BookOnDisk ->
      booksWithSameFilePath?.find { it?.file?.path == bookOnDisk.file.path } == null
    }
  }

  @Query("SELECT * FROM BookOnDiskRoomEntity where file LIKE :filePath")
  abstract fun booksWithSameFilePathAsEntity(filePath: String): BookOnDiskRoomEntity?

  @Transaction
  open fun booksWithSameFilePath(booksOnDisks: List<BooksOnDiskListItem.BookOnDisk>):
    List<BooksOnDiskListItem.BookOnDisk?>? {
    return booksOnDisks.map { booksOnDisk ->
      val path = booksOnDisk.file.path
      val entity = booksWithSameFilePathAsEntity(path)
      if (entity != null) {
        BooksOnDiskListItem.BookOnDisk(entity)
      } else {
        null
      }
    }
  }

  @Query("DELETE FROM BookOnDiskRoomEntity WHERE bookId LIKE :id")
  abstract fun removeEntriesWithMatchingId(id: String)

  @Transaction
  open fun removeEntriesWithMatchingIds(uniqueBooks: List<BooksOnDiskListItem.BookOnDisk>) {
    uniqueBooks.forEachIndexed { _, bookOnDisk ->
      removeEntriesWithMatchingId(bookOnDisk.id.toString())
    }
  }

  @Query("DELETE FROM BookOnDiskRoomEntity WHERE bookId LIKE :databaseId")
  abstract fun delete(databaseId: Long)

  fun getFavIconAndZimFile(it: Bookmark): Pair<String?, String?> {
    return getBookOnDiskById(it.zimId).let {
      return@let it.favIcon to it.file.path
    }
  }

  @Query("SELECT * FROM BookOnDiskRoomEntity WHERE bookId LIKE :zimId")
  abstract fun getBookOnDiskById(zimId: String): BookOnDiskRoomEntity

  @Query("SELECT * FROM BookOnDiskRoomEntity  WHERE file LIKE :downloadTitle")
  abstract fun bookMatching(downloadTitle: String): BookOnDiskRoomEntity

  fun migrationInsert(box: Box<BookOnDiskEntity>) {
    val bookOnDiskEntities = box.all
    bookOnDiskEntities.forEachIndexed { _, bookOnDiskEntity ->
      CoroutineScope(Dispatchers.IO).launch {
        val bookOnDisk = BooksOnDiskListItem.BookOnDisk(bookOnDiskEntity)
        insertAsEntity(BookOnDiskRoomEntity(bookOnDisk))
      }
    }
  }
}

class StringToFileConverterDao {
  @TypeConverter
  fun convertToDatabaseValue(entityProperty: File?) = entityProperty?.path ?: ""

  @TypeConverter
  fun convertToEntityProperty(databaseValue: String?) = File(databaseValue ?: "")
}
