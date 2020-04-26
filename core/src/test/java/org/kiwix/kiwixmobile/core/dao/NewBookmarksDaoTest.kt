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
import io.objectbox.query.PropertyQuery
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.bookmark.BookmarkItem
import org.kiwix.kiwixmobile.core.dao.entities.BookOnDiskEntity
import org.kiwix.kiwixmobile.core.dao.entities.BookOnDiskEntity_
import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity
import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity_
import org.kiwix.kiwixmobile.core.data.local.entity.Bookmark
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.sharedFunctions.bookmarkItem
import org.kiwix.sharedFunctions.bookmarkEntity
import org.kiwix.sharedFunctions.bookOnDiskEntity

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
    val bookmarkEntities: List<BookmarkEntity> = listOf(bookmarkEntity())
    every { query.find() } returns bookmarkEntities
    val bookmarkItem = BookmarkItem(bookmarkEntity())
    assertThat(newBookmarksDao.getBookmarks(false, zimFileReader))
      .isEqualTo(listOf(bookmarkItem))
  }

  @Test
  fun `get bookmarks from current book when zimFileReader is null`() {
    val queryBuilder: QueryBuilder<BookmarkEntity> = mockk()
    every { box.query() } returns queryBuilder
    every { queryBuilder.equal(BookmarkEntity_.zimName, "") } returns queryBuilder
    every { queryBuilder.order(BookmarkEntity_.bookmarkTitle) } returns queryBuilder
    val query: Query<BookmarkEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    val bookmarkEntities: List<BookmarkEntity> = listOf(bookmarkEntity())
    every { query.find() } returns bookmarkEntities
    val bookmarkItem = BookmarkItem(bookmarkEntity())
    assertThat(newBookmarksDao.getBookmarks(true, null))
      .isEqualTo(listOf(bookmarkItem))
  }

  @Test
  fun `get bookmarks from current book`() {
    val zimFileReader: ZimFileReader = mockk()
    val queryBuilder: QueryBuilder<BookmarkEntity> = mockk()
    every { box.query() } returns queryBuilder
    val zimName = "zimName"
    every { zimFileReader?.name } returns zimName
    every { queryBuilder.equal(BookmarkEntity_.zimName, zimFileReader?.name) } returns queryBuilder
    every { queryBuilder.order(BookmarkEntity_.bookmarkTitle) } returns queryBuilder
    val query: Query<BookmarkEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    val bookmarkEntities: List<BookmarkEntity> = listOf(bookmarkEntity())
    every { query.find() } returns bookmarkEntities
    val bookmarkItem = BookmarkItem(bookmarkEntity())
    assertThat(newBookmarksDao.getBookmarks(false, zimFileReader))
      .isEqualTo(listOf(bookmarkItem))
  }

  @Test
  fun `getCurrentZimBookmarksUrl returns list of bookmarkUrl`() {
    val zimFileReader: ZimFileReader = mockk()
    val queryBuilder: QueryBuilder<BookmarkEntity> = mockk()
    every { box.query() } returns queryBuilder
    val zimId = ""
    every { zimFileReader?.id } returns zimId
    every { queryBuilder.equal(BookmarkEntity_.zimId, zimFileReader?.id) } returns queryBuilder
    every { queryBuilder.or() } returns queryBuilder
    val zimName = ""
    every { zimFileReader?.name } returns zimName
    every { queryBuilder.equal(BookmarkEntity_.zimName, zimFileReader?.name) } returns queryBuilder
    every { queryBuilder.order(BookmarkEntity_.bookmarkTitle) } returns queryBuilder
    val query: Query<BookmarkEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    val bookmarkUrl = "bookmarkUrl"
    val propertyQuery: PropertyQuery = mockk()
    every { query.property(BookmarkEntity_.bookmarkUrl) } returns propertyQuery
    val bookmarkUrlArray = arrayOf<String>(bookmarkUrl)
    every { propertyQuery.findStrings() } returns bookmarkUrlArray
    assertThat(newBookmarksDao.getCurrentZimBookmarksUrl(zimFileReader))
      .isEqualTo(listOf(bookmarkUrl))
  }

  @Test
  fun `getCurrentZimBookmarksUrl returns list of distinct bookmarkUrl`() {
    val zimFileReader: ZimFileReader = mockk()
    val queryBuilder: QueryBuilder<BookmarkEntity> = mockk()
    every { box.query() } returns queryBuilder
    val zimId = ""
    every { zimFileReader?.id } returns zimId
    every { queryBuilder.equal(BookmarkEntity_.zimId, zimFileReader?.id) } returns queryBuilder
    every { queryBuilder.or() } returns queryBuilder
    val zimName = ""
    every { zimFileReader?.name } returns zimName
    every { queryBuilder.equal(BookmarkEntity_.zimName, zimFileReader?.name) } returns queryBuilder
    every { queryBuilder.order(BookmarkEntity_.bookmarkTitle) } returns queryBuilder
    val query: Query<BookmarkEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    val bookmarkUrl = "bookmarkUrl"
    val propertyQuery: PropertyQuery = mockk()
    every { query.property(BookmarkEntity_.bookmarkUrl) } returns propertyQuery
    val bookmarkUrlArray = arrayOf<String>(bookmarkUrl, bookmarkUrl)
    every { propertyQuery.findStrings() } returns bookmarkUrlArray
    assertThat(newBookmarksDao.getCurrentZimBookmarksUrl(zimFileReader))
      .isEqualTo(listOf(bookmarkUrl))
  }

  @Test
  fun `getCurrentZimBookmarksUrl returns empty list with empty zimName and zimId`() {
    val queryBuilder: QueryBuilder<BookmarkEntity> = mockk()
    every { box.query() } returns queryBuilder
    every { queryBuilder.equal(BookmarkEntity_.zimName, "") } returns queryBuilder
    every { queryBuilder.or() } returns queryBuilder
    every { queryBuilder.equal(BookmarkEntity_.zimId, "") } returns queryBuilder
    every { queryBuilder.order(BookmarkEntity_.bookmarkTitle) } returns queryBuilder
    val query: Query<BookmarkEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    assertThat(newBookmarksDao.getCurrentZimBookmarksUrl(null)).isEmpty()
  }

  @Test
  fun `bookmark urls for current workbook`() {
    val zimFileReader: ZimFileReader = mockk()
    val queryBuilder: QueryBuilder<BookmarkEntity> = mockk()
    every { box.query() } returns queryBuilder
    every { zimFileReader.name } returns ""
    every { queryBuilder.equal(BookmarkEntity_.zimName, zimFileReader.name) } returns queryBuilder
    every { queryBuilder.or() } returns queryBuilder
    every { zimFileReader.id } returns ""
    every { queryBuilder.equal(BookmarkEntity_.zimId, zimFileReader.id) } returns queryBuilder
    every { queryBuilder.order(BookmarkEntity_.bookmarkTitle) } returns queryBuilder
    val query: Query<BookmarkEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    newBookmarksDao.bookmarkUrlsForCurrentBook(zimFileReader).test()
      .assertEmpty()
  }

  @Test
  fun `bookmark urls for current workbook without zimFileReader`() {
    val queryBuilder: QueryBuilder<BookmarkEntity> = mockk()
    every { box.query() } returns queryBuilder
    every { queryBuilder.equal(BookmarkEntity_.zimName, "") } returns queryBuilder
    every { queryBuilder.or() } returns queryBuilder
    every { queryBuilder.equal(BookmarkEntity_.zimId, "") } returns queryBuilder
    every { queryBuilder.order(BookmarkEntity_.bookmarkTitle) } returns queryBuilder
    val query: Query<BookmarkEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    newBookmarksDao.bookmarkUrlsForCurrentBook(null).test()
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
    val id = 0L
    val zimId = "zimId"
    val zimName = "zimName"
    val zimFilePath = "zimFilePath"
    val bookmarkUrl = "bookmarkUrl"
    val bookmarkTitle = "bookmarkTitle"
    val favicon: String? = "favicon"
    val bookmarkItem: BookmarkItem = mockk()
    val bookmarksList: List<BookmarkItem> = listOf(bookmarkItem)
    every { bookmarkItem.databaseId } returns id
    every { bookmarkItem.zimId } returns zimId
    every { bookmarkItem.zimName } returns zimName
    every { bookmarkItem.zimFilePath } returns zimFilePath
    every { bookmarkItem.bookmarkUrl } returns bookmarkUrl
    every { bookmarkItem.bookmarkTitle } returns bookmarkTitle
    every { bookmarkItem.favicon } returns favicon
    newBookmarksDao.deleteBookmarks(bookmarksList)
    verify {
      box.remove(
        listOf(
          bookmarkEntity(
            id = id,
            zimId = zimId,
            zimName = zimName,
            zimFilePath = zimFilePath,
            bookmarkUrl = bookmarkUrl,
            bookmarkTitle = bookmarkTitle,
            favicon = favicon
          )
        )
      )
    }
  }

  @Test
  fun `migrate insert`() {
    val queryBuilder: QueryBuilder<BookOnDiskEntity> = mockk()
    val boxBookOnDisk: Box<BookOnDiskEntity> = mockk(relaxed = true)
    every { boxBookOnDisk.query() } returns queryBuilder
    val bookmark = Bookmark()
    every { queryBuilder.equal(BookOnDiskEntity_.bookId, "") } returns queryBuilder
    val query: Query<BookOnDiskEntity> = mockk(relaxed = true)
    val bookOnDiskEntity = bookOnDiskEntity()
    val bookOnDiskEntities = mutableListOf(bookOnDiskEntity)
    every { query.find() } returns bookOnDiskEntities
    // val bookmarks = mutableListOf<Bookmark>(bookmark)
    // val bookDao: NewBookDao = mockk()
    // verify {
    //   box.put(bookmarks.zip(bookmarks.map(bookDao.getFavIconAndZimFile(book))).map(::BookmarkEntity))
    // }
  }
}
