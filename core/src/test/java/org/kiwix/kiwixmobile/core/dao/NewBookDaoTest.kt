/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.kiwix.kiwixmobile.core.dao.entities.MyObjectBox
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import java.io.File

@TestInstance(Lifecycle.PER_CLASS)
internal class NewBookDaoTest {
  private val testDirectory = File("src/test/java/org/kiwix/kiwixmobile/core/dao/test-db")
  private lateinit var boxStore: BoxStore
  private lateinit var newBookDao: NewBookDao

  @BeforeEach
  fun setUp() {
    BoxStore.deleteAllFiles(testDirectory)
    boxStore = MyObjectBox.builder().directory(testDirectory).build()
    newBookDao = NewBookDao(boxStore.boxFor())
  }

  @AfterEach
  fun tearDown() {
    boxStore.close()
    BoxStore.deleteAllFiles(testDirectory)
  }

  private fun mockBook(bookName: String): Book {
    val book = Book()

    book.id = "id $bookName"
    book.title = "title $bookName"
    book.description = "description $bookName"
    book.language = "language $bookName"
    book.creator = "creator $bookName"
    book.publisher = "publisher $bookName"
    book.date = "date $bookName"
    book.url = "url $bookName"
    book.articleCount = "articleCount $bookName"
    book.mediaCount = "mediaCount $bookName"
    book.size = "size $bookName"
    book.bookName = bookName
    book.favicon = "favicon $bookName"
    book.tags = "tags $bookName"

    return book
  }

  fun mockBookOnDisk(bookName: String): BookOnDisk =
    BookOnDisk(databaseId = 1, book = mockBook(bookName), file = File(bookName))

  @Nested
  inner class InsertTest {
    @Test
    fun `insert should insert a single BookOnDiskEntity`() {
      val bookList = listOf(mockBookOnDisk("first book"))
      newBookDao.insert(bookList)

      val databaseBookList: List<BookOnDisk> = newBookDao.getBooks()

      assertEquals(bookList[0], databaseBookList[0])
    }
  }
}
