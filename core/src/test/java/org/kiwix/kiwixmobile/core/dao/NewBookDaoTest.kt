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

import android.annotation.SuppressLint
import io.mockk.CapturingSlot
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import io.objectbox.Box
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import io.objectbox.rx.RxQuery
import io.reactivex.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.entities.BookOnDiskEntity
import org.kiwix.kiwixmobile.core.dao.entities.BookOnDiskEntity_
import org.kiwix.kiwixmobile.core.data.local.entity.Bookmark
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.sharedFunctions.book
import org.kiwix.sharedFunctions.bookOnDisk
import org.kiwix.sharedFunctions.bookOnDiskEntity
import java.io.File
import java.util.concurrent.Callable

internal class NewBookDaoTest {

  private val box: Box<BookOnDiskEntity> = mockk(relaxed = true)
  private val newBookDao = NewBookDao(box)

  @BeforeEach
  internal fun setUp() {
    clearAllMocks()
  }

  @Nested
  inner class BooksTests {
    @Test
    fun `books emits entities whose file exists`() {
      val (expectedEntity, _) = expectEmissionOfExistingAndNotExistingBook()
      newBookDao.books().test().assertValues(listOf(BookOnDisk(expectedEntity)))
    }

    @SuppressLint("CheckResult")
    @Test
    fun `books deletes entities whose file does not exist`() {
      val (_, deletedEntity) = expectEmissionOfExistingAndNotExistingBook()
      newBookDao.books().test()
      verify { box.remove(listOf(deletedEntity)) }
    }

    private fun expectEmissionOfExistingAndNotExistingBook():
      Pair<BookOnDiskEntity, BookOnDiskEntity> {
      val query: Query<BookOnDiskEntity> = mockk()
      every { box.query().build() } returns query
      val fileThatExists = mockk<File>()
      val fileThatDoesNotExist = mockk<File>()
      every { fileThatExists.exists() } returns true
      every { fileThatDoesNotExist.exists() } returns false
      val entityThatExists = bookOnDiskEntity(file = fileThatExists)
      val entityThatDoesNotExist = bookOnDiskEntity(file = fileThatDoesNotExist)
      mockkStatic(RxQuery::class)
      every { RxQuery.observable(query) } returns Observable.just(
        listOf(entityThatExists, entityThatDoesNotExist)
      )
      return Pair(entityThatExists, entityThatDoesNotExist)
    }
  }

  @Test
  fun getBooks() {
    val entity = bookOnDiskEntity()
    every { box.all } returns mutableListOf(entity)
    assertThat(newBookDao.getBooks()).isEqualTo(listOf(BookOnDisk(entity)))
  }

  @Nested
  inner class Insertion {
    @Test
    fun `insert transaction adds books to the box that have distinct ids`() {
      val slot: CapturingSlot<Callable<Unit>> = slot()
      every { box.store.callInTx(capture(slot)) } returns Unit
      val distinctBook: BookOnDisk = bookOnDisk(databaseId = 0, book = book(id = "same"))
      newBookDao.insert(
        listOf(distinctBook, bookOnDisk(databaseId = 1, book = book(id = "same")))
      )
      val queryBuilder: QueryBuilder<BookOnDiskEntity> = mockk(relaxed = true)
      every { box.query() } returns queryBuilder
      every {
        queryBuilder.`in`(
          BookOnDiskEntity_.file,
          arrayOf(distinctBook.file.path),
          QueryBuilder.StringOrder.CASE_INSENSITIVE
        )
      } returns queryBuilder
      val query: Query<BookOnDiskEntity> = mockk(relaxed = true)
      every { queryBuilder.build() } returns query
      every { query.find() } returns listOf(bookOnDiskEntity(file = File("matches_nothing")))
      slot.captured.call()
      verify { box.put(listOf(BookOnDiskEntity(distinctBook))) }
    }

    @Test
    fun `insert transaction does not add books if a book with the same path exists in the box`() {
      val slot: CapturingSlot<Callable<Unit>> = slot()
      every { box.store.callInTx(capture(slot)) } returns Unit
      val distinctBook: BookOnDisk = bookOnDisk()
      newBookDao.insert(listOf(distinctBook))
      val queryBuilder: QueryBuilder<BookOnDiskEntity> = mockk(relaxed = true)
      every { box.query() } returns queryBuilder
      every {
        queryBuilder.`in`(
          BookOnDiskEntity_.file,
          arrayOf(distinctBook.file.path),
          QueryBuilder.StringOrder.CASE_INSENSITIVE
        )
      } returns queryBuilder
      val query: Query<BookOnDiskEntity> = mockk(relaxed = true)
      every { queryBuilder.build() } returns query
      every { query.find() } returns listOf(bookOnDiskEntity(file = distinctBook.file))
      slot.captured.call()
      verify { box.put(listOf()) }
    }

    @Test
    fun `insert transaction removes books with duplicate ids`() {
      val slot: CapturingSlot<Callable<Unit>> = slot()
      every { box.store.callInTx(capture(slot)) } returns Unit
      val distinctBook: BookOnDisk = bookOnDisk()
      newBookDao.insert(listOf(distinctBook))
      val queryBuilder: QueryBuilder<BookOnDiskEntity> = mockk()
      every { box.query() } returns queryBuilder
      every {
        queryBuilder.`in`(
          BookOnDiskEntity_.file,
          arrayOf(distinctBook.file.path),
          QueryBuilder.StringOrder.CASE_INSENSITIVE
        )
      } returns queryBuilder
      val query: Query<BookOnDiskEntity> = mockk()
      every { queryBuilder.build() } returns query
      every { query.find() } returns listOf()
      every {
        queryBuilder.`in`(
          BookOnDiskEntity_.bookId,
          arrayOf(distinctBook.book.id),
          QueryBuilder.StringOrder.CASE_INSENSITIVE
        )
      } returns queryBuilder
      every { query.remove() } returns 0L
      slot.captured.call()
      verify { box.put(listOf(BookOnDiskEntity(distinctBook))) }
    }
  }

  @Test
  fun delete() {
    newBookDao.delete(0L)
    verify { box.remove(0L) }
  }

  @Test
  fun migrationInsert() {
    val book: LibraryNetworkEntity.Book = book(file = mockk<File>(relaxed = true))
    val slot: CapturingSlot<Callable<Unit>> = slot()
    every { box.store.callInTx(capture(slot)) } returns Unit
    newBookDao.migrationInsert(listOf(book))
    slot.captured.call()
    verify { box.put(listOf(BookOnDiskEntity(BookOnDisk(book = book, file = book.file!!)))) }
  }

  @Test
  fun `getFavIconAndZimFile with no result returns pair with null values`() {
    val bookmark: Bookmark = mockk()
    expectGetFavIconAndZimFileWith(bookmark, listOf())
    assertThat(newBookDao.getFavIconAndZimFile(bookmark)).isEqualTo(Pair(null, null))
  }

  @Test
  fun `getFavIconAndZimFile with result returns valid pair`() {
    val bookmark: Bookmark = mockk()
    val bookOnDiskEntity: BookOnDiskEntity = bookOnDiskEntity()
    expectGetFavIconAndZimFileWith(bookmark, listOf(bookOnDiskEntity))
    assertThat(newBookDao.getFavIconAndZimFile(bookmark))
      .isEqualTo(Pair(bookOnDiskEntity.favIcon, bookOnDiskEntity.file.path))
  }

  private fun expectGetFavIconAndZimFileWith(
    bookmark: Bookmark,
    queryResult: List<BookOnDiskEntity>
  ) {
    val expectedZimId = "zimId"
    every { bookmark.zimId } returns expectedZimId
    val queryBuilder: QueryBuilder<BookOnDiskEntity> = mockk()
    every { box.query() } returns queryBuilder
    every {
      queryBuilder.equal(
        BookOnDiskEntity_.bookId,
        expectedZimId,
        QueryBuilder.StringOrder.CASE_INSENSITIVE
      )
    } returns queryBuilder
    val query: Query<BookOnDiskEntity> = mockk()
    every { queryBuilder.build() } returns query
    every { query.find() } returns queryResult
  }

  @Test
  fun `bookMatching queries file by title`() {
    val downloadTitle = "title"
    val queryBuilder: QueryBuilder<BookOnDiskEntity> = mockk()
    every { box.query() } returns queryBuilder
    every {
      queryBuilder.endsWith(
        BookOnDiskEntity_.file,
        downloadTitle,
        QueryBuilder.StringOrder.CASE_INSENSITIVE
      )
    } returns queryBuilder
    val query: Query<BookOnDiskEntity> = mockk()
    every { queryBuilder.build() } returns query
    val bookOnDiskEntity: BookOnDiskEntity = bookOnDiskEntity()
    every { query.findFirst() } returns bookOnDiskEntity
    assertThat(newBookDao.bookMatching(downloadTitle)).isEqualTo(bookOnDiskEntity)
  }
}
