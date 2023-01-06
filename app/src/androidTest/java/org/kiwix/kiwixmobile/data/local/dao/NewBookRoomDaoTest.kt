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

package org.kiwix.kiwixmobile.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.dao.NewBookRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.BookOnDiskRoomEntity
import org.kiwix.kiwixmobile.core.data.local.KiwixRoomDatabase
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class NewBookRoomDaoTest {

  private lateinit var newBookRoomDao: NewBookRoomDao
  private lateinit var db: KiwixRoomDatabase

  @Test
  @Throws(IOException::class)
  fun testBooks() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()
    newBookRoomDao = db.newBookRoomDao()
    val books: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    val books2: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    val books3: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    val books4: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    val books5: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    newBookRoomDao.insert(listOf(books, books2, books3, books4, books5))
    newBookRoomDao.books().subscribe {
      Assertions.assertEquals(5, it.size)
    }
  }

  @Test
  @Throws(IOException::class)
  fun deleteBook() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()
    newBookRoomDao = db.newBookRoomDao()
    val books: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    val books2: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    val books3: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    val books4: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    val books5: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    newBookRoomDao.insert(listOf(books, books2, books3, books4, books5))
    newBookRoomDao.deleteBooks(BookOnDiskRoomEntity(books5))
    newBookRoomDao.books().subscribe {
      Assertions.assertEquals(4, it.size)
    }
  }

  @Test
  @Throws(IOException::class)
  fun deleteBookByDatabaseId() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()
    newBookRoomDao = db.newBookRoomDao()
    val books: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    val books2: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    val books3: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    val books4: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    val books5: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    newBookRoomDao.insert(listOf(books, books2, books3, books4, books5))
    newBookRoomDao.delete(databaseId = books5.databaseId)
    newBookRoomDao.books().subscribe {
      Assertions.assertEquals(4, it.size)
    }
  }

  @Test
  @Throws(IOException::class)
  fun getBooksOnDiskById() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()
    newBookRoomDao = db.newBookRoomDao()
    val book: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    val books2: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    val books3: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    val books4: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    val books5: BooksOnDiskListItem.BookOnDisk = mockk(relaxed = true)
    newBookRoomDao.insert(listOf(book, books2, books3, books4, books5))
    val returnedBooks = newBookRoomDao.getBookOnDiskById(books5.book.id)
    Assertions.assertEquals(books5, returnedBooks)
  }
}
