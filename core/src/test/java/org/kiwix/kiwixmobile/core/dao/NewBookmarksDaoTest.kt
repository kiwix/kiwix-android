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

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import io.objectbox.Box
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.bookmark.BookmarkItem
import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity
import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity_
import org.kiwix.kiwixmobile.core.data.local.entity.Bookmark
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.sharedFunctions.bookmarkItem

internal class NewBookmarksDaoTest {

  private val box: Box<BookmarkEntity> = mockk(relaxed = true)
  private val newBookmarksDao = NewBookmarksDao(box)

  @BeforeEach
  fun init() {
    clearAllMocks()
    mockkStatic(CoreApp::class)
    every { CoreApp.getInstance().packageName } returns "pkg"
  }

  @Test
  fun `get bookmarks without current book`() {
    val zimFileReader: ZimFileReader = mockk()
    val queryBuilder: QueryBuilder<BookmarkEntity> = mockk()
    every { box.query() } returns queryBuilder
    every { queryBuilder.order(BookmarkEntity_.bookmarkTitle) } returns queryBuilder
    val query: Query<BookmarkEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    val bookmarkEntities: List<BookmarkEntity> = mockk(relaxed = true)
    every { query.find() } returns bookmarkEntities
    newBookmarksDao.getBookmarks(false, zimFileReader)
    verify { queryBuilder.build() }
  }

  @Test
  fun `get bookmarks from current book`() {
    val zimFileReader: ZimFileReader = mockk()
    val queryBuilder: QueryBuilder<BookmarkEntity> = mockk()
    every { box.query() } returns queryBuilder
    every { zimFileReader.name } returns ""
    every {
      queryBuilder.equal(
        BookmarkEntity_.zimName,
        zimFileReader.name
      )
    } returns queryBuilder
    every { queryBuilder.order(BookmarkEntity_.bookmarkTitle) } returns queryBuilder
    val query: Query<BookmarkEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    newBookmarksDao.getBookmarks(true, zimFileReader)
    verify { queryBuilder.equal(BookmarkEntity_.zimName, zimFileReader.name) }
  }

  @Test
  fun `get bookmarks from current book when zimFileReader is null`() {
    val zimFileReader: ZimFileReader? = null
    val queryBuilder: QueryBuilder<BookmarkEntity> = mockk()
    every { box.query() } returns queryBuilder
    every {
      queryBuilder.equal(
        BookmarkEntity_.zimName,
        zimFileReader?.name ?: ""
      )
    } returns queryBuilder
    every { queryBuilder.order(BookmarkEntity_.bookmarkTitle) } returns queryBuilder
    val query: Query<BookmarkEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    newBookmarksDao.getBookmarks(true, zimFileReader)
    verify { queryBuilder.equal(BookmarkEntity_.zimName, "") }
  }

  @Test
  fun `get current zim bookmarks url`() {
    val zimFileReader: ZimFileReader = mockk()
    val queryBuilder: QueryBuilder<BookmarkEntity> = mockk()
    every { box.query() } returns queryBuilder
    every { zimFileReader.name } returns ""
    every {
      queryBuilder.equal(
        BookmarkEntity_.zimName,
        zimFileReader.name
      )
    } returns queryBuilder
    every { queryBuilder.or() } returns queryBuilder
    every { zimFileReader.id } returns ""
    every {
      queryBuilder.equal(
        BookmarkEntity_.zimId,
        zimFileReader.id
      )
    } returns queryBuilder
    every { queryBuilder.order(BookmarkEntity_.bookmarkTitle) } returns queryBuilder
    val query: Query<BookmarkEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    newBookmarksDao.getCurrentZimBookmarksUrl(zimFileReader)
  }

  @Test
  fun `bookmark urls for current workbook`() {
    val zimFileReader: ZimFileReader = mockk()
    val queryBuilder: QueryBuilder<BookmarkEntity> = mockk()
    every { box.query() } returns queryBuilder
    every { zimFileReader.name } returns ""
    every {
      queryBuilder.equal(
        BookmarkEntity_.zimName,
        zimFileReader.name
      )
    } returns queryBuilder
    every { queryBuilder.or() } returns queryBuilder
    every { zimFileReader.id } returns ""
    every {
      queryBuilder.equal(
        BookmarkEntity_.zimId,
        zimFileReader.id
      )
    } returns queryBuilder
    every { queryBuilder.order(BookmarkEntity_.bookmarkTitle) } returns queryBuilder
    val query: Query<BookmarkEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    newBookmarksDao.bookmarkUrlsForCurrentBook(zimFileReader).test()
      .assertEmpty()
  }

  @Test
  fun `saveBookmark saves the specific bookmark Item`() {
    val bookmarkItem =
      bookmarkItem(0L, "zimId", "zimName", "zimFilePath", "bookmarkUrl", "bookmarkTitle", "favicon")
    newBookmarksDao.saveBookmark(bookmarkItem)
    verify { box.put(BookmarkEntity(bookmarkItem)) }
  }

  @Test
  fun `deleteBookmark removes query results for the url`() {
    val bookmarkUrl = "bookmarkUrl"
    val queryBuilder: QueryBuilder<BookmarkEntity> = mockk()
    every { box.query() } returns queryBuilder
    every { queryBuilder.equal(BookmarkEntity_.bookmarkUrl, bookmarkUrl) } returns queryBuilder
    val query: Query<BookmarkEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    newBookmarksDao.deleteBookmark(bookmarkUrl)
    verify { query.remove() }
  }

  @Test
  fun `deleteBookmarks removes specified bookmark item list`() {
    val bookmarksList: List<BookmarkItem> = mockk(relaxed = true)
    newBookmarksDao.deleteBookmarks(bookmarksList)
    verify { box.remove(bookmarksList.map(::BookmarkEntity)) }
  }

  @Test
  fun `migrate insert`() {
    val bookmarks: MutableList<Bookmark> = mockk(relaxed = true)
    val bookDao: NewBookDao = mockk()
    newBookmarksDao.migrationInsert(bookmarks, bookDao)
    verify {
      box.put(
        bookmarks.zip(bookmarks.map(bookDao::getFavIconAndZimFile)).map(::BookmarkEntity)
      )
    }
  }
}

