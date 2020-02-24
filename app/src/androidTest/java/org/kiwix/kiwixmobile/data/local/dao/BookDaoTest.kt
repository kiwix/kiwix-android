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
package org.kiwix.kiwixmobile.data.local.dao

import android.content.Context
import androidx.test.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.fail
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.data.local.KiwixDatabase
import org.kiwix.kiwixmobile.core.data.local.dao.BookDao
import org.kiwix.kiwixmobile.core.data.local.entity.BookDatabaseEntity
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import java.io.File
import java.io.IOException
import java.util.ArrayList

@RunWith(AndroidJUnit4::class)
class BookDaoTest {
  private var testDir: File? = null

  @Before
  fun executeBefore() {
    testDir = InstrumentationRegistry.getTargetContext().getDir("testDir", Context.MODE_PRIVATE)
  }

  // TODO : test books are saved after downloading the list of available zim files
  @Test @Throws(IOException::class)
  fun testGetBooks() { // Save the fake data to test
    val kiwixDatabase = mockk<KiwixDatabase>()
    val bookDao = BookDao(kiwixDatabase)
    val testId = "6qq5301d-2cr0-ebg5-474h-6db70j52864p"
    val fileName = testDir?.path + "/" + testId + "testFile"
    val booksToAdd = getFakeData(fileName)
    every { kiwixDatabase.deleteWhere(any(), any()) } returns 0
    val booksRetrieved =
      bookDao.filterBookResults(booksToAdd)
    if (!booksRetrieved.contains(booksToAdd[0])) fail()
    verify(exactly = 0) {
      kiwixDatabase.deleteWhere(
        BookDatabaseEntity::class.java,
        BookDatabaseEntity.URL.eq(booksToAdd[0].file.path)
      )
    }
    if (booksRetrieved.contains(booksToAdd[1])) fail()
    verify(exactly = 0) {
      kiwixDatabase.deleteWhere(
        BookDatabaseEntity::class.java,
        BookDatabaseEntity.URL.eq(booksToAdd[1].file.path)
      )
    }
    if (booksRetrieved.contains(booksToAdd[2])) fail()
    verify(exactly = 0) {
      kiwixDatabase.deleteWhere(
        BookDatabaseEntity::class.java,
        BookDatabaseEntity.URL.eq(booksToAdd[2].file.path)
      )
    }
    if (booksRetrieved.contains(booksToAdd[3])) fail()
    verify {
      kiwixDatabase.deleteWhere(
        BookDatabaseEntity::class.java,
        BookDatabaseEntity.URL.eq(booksToAdd[3].file.path)
      )
    }
    if (!booksRetrieved.contains(booksToAdd[4])) fail()
    verify(exactly = 0) {
      kiwixDatabase.deleteWhere(
        BookDatabaseEntity::class.java,
        BookDatabaseEntity.URL.eq(booksToAdd[4].file.path)
      )
    }
    if (!booksRetrieved.contains(booksToAdd[5])) fail()
    verify(exactly = 0) {
      kiwixDatabase.deleteWhere(
        BookDatabaseEntity::class.java,
        BookDatabaseEntity.URL.eq(booksToAdd[5].file.path)
      )
    }
    if (booksRetrieved.contains(booksToAdd[6])) fail()
    verify {
      kiwixDatabase.deleteWhere(
        BookDatabaseEntity::class.java,
        BookDatabaseEntity.URL.eq(booksToAdd[6].file.path)
      )
    }
    if (!booksRetrieved.contains(booksToAdd[7])) fail()
    verify(exactly = 0) {
      kiwixDatabase.deleteWhere(
        BookDatabaseEntity::class.java,
        BookDatabaseEntity.URL.eq(booksToAdd[7].file.path)
      )
    }
    if (booksRetrieved.contains(booksToAdd[8])) fail()
    verify {
      kiwixDatabase.deleteWhere(
        BookDatabaseEntity::class.java,
        BookDatabaseEntity.URL.eq(booksToAdd[8].file.path)
      )
    }
  }

  private fun fail() {
    fail { "shouldn't happen" }
  }

  @Throws(IOException::class)
  private fun getFakeData(baseFileName: String): ArrayList<Book> {
    val books =
      ArrayList<Book>()
    for (i in 0..8) {
      val book = Book()
      book.bookName = "Test Copy $i"
      book.id = "Test ID $i"
      val fileName = baseFileName + i
      when (i) {
        0 -> {
          book.file = File("$fileName.zim")
          book.file.createNewFile()
        }
        1 -> {
          book.file = File("$fileName.part")
          book.file.createNewFile()
        }
        2 -> {
          book.file = File("$fileName.zim")
          val t2 = File("$fileName.zim.part")
          t2.createNewFile()
        }
        3 -> book.file = File("$fileName.zim")
        4 -> {
          book.file = File("$fileName.zim")
          book.file.createNewFile()
          val t4 = File("$fileName.zim.part")
          t4.createNewFile()
        }
        5 -> {
          book.file = File("$fileName.zimdg")
          setupCase1(fileName)
        }
        6 -> {
          book.file = File("$fileName.zimyr")
          setupCase2(fileName)
        }
        7 -> {
          book.file = File("$fileName.zimdg")
          setupCase1(fileName)
        }
        8 -> {
          book.file = File("$fileName.zimyr")
          setupCase2(fileName)
        }
      }
      books.add(book)
    }
    return books
  }

  @Throws(IOException::class)
  private fun setupCase1(fileName: String) {
    var char1 = 'a'
    while (char1 <= 'z') {
      var char2 = 'a'
      while (char2 <= 'z') {
        val file = File("$fileName.zim$char1$char2")
        file.createNewFile()
        if (char1 == 'd' && char2 == 'r') {
          break
        }
        char2++
      }
      if (char1 == 'd') {
        break
      }
      char1++
    }
  }

  @Throws(IOException::class)
  private fun setupCase2(fileName: String) {
    var char1 = 'a'
    while (char1 <= 'z') {
      var char2 = 'a'
      while (char2 <= 'z') {
        val file = File("$fileName.zim$char1$char2")
        file.createNewFile()
        if (char1 == 'd' && char2 == 'r') {
          break
        }
        char2++
      }
      if (char1 == 'd') {
        break
      }
      char1++
    }
    val t = File("$fileName.zimcp.part")
  }

  @After fun removeTestDirectory() {
    for (child in testDir?.listFiles() ?: emptyArray()) {
      child.delete()
    }
    testDir?.delete()
  }
}
