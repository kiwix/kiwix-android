/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

import android.os.Build
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.page.bookmark.models.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.libkiwix.Book
import org.kiwix.libkiwix.Bookmark
import org.kiwix.libkiwix.Library
import org.kiwix.libkiwix.Manager
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(
  sdk = [Build.VERSION_CODES.R],
  application = TestApplication::class,
  instrumentedPackages = ["org.kiwix.libkiwix"]
)
class LibkiwixBookmarksTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private val library: Library = mockk(relaxed = true)
  private val manager: Manager = mockk(relaxed = true)
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk = mockk()
  private val testDispatcher = UnconfinedTestDispatcher()
  private lateinit var libkiwixBookmarks: LibkiwixBookmarks

  @Before
  fun setUp() {
    coEvery { kiwixDataStore.defaultStorage() } returns temporaryFolder.root.path
    every { library.booksIds } returns arrayOf(FIRST_BOOK_ID, SECOND_BOOK_ID)
    every { library.getBookById(FIRST_BOOK_ID) } returns book(FIRST_BOOK_ID)
    every { library.getBookById(SECOND_BOOK_ID) } returns book(SECOND_BOOK_ID)
    every { library.getBookmarks(false) } returns
      arrayOf(
        bookmark(FIRST_BOOK_ID, SHARED_URL, "Shared title"),
        bookmark(SECOND_BOOK_ID, SHARED_URL, "Shared title")
      )
    libkiwixBookmarks =
      LibkiwixBookmarks(
        library,
        manager,
        kiwixDataStore,
        libkiwixBookOnDisk,
        null,
        testDispatcher
      )
  }

  @Test
  fun `cached bookmark list keeps a bookmark whose url is also bookmarked in another book`() =
    runTest(testDispatcher) {
      val zimFileReader: ZimFileReader = mockk()
      every { zimFileReader.id } returns SECOND_BOOK_ID

      val freshBookmarkUrls = libkiwixBookmarks.getCurrentZimBookmarksUrl(zimFileReader)
      val cachedBookmarkUrls = libkiwixBookmarks.getCurrentZimBookmarksUrl(zimFileReader)

      assertEquals(listOf(SHARED_URL), freshBookmarkUrls)
      assertEquals(listOf(SHARED_URL), cachedBookmarkUrls)
    }

  @Test
  fun `deleting a bookmark keeps the book in the library while another of its bookmarks exists`() =
    runTest(testDispatcher) {
      libkiwixBookmarks.deleteBookmarks(
        listOf(LibkiwixBookmarkItem(zimId = SECOND_BOOK_ID, bookmarkUrl = DELETED_URL))
      )

      verify(exactly = 0) { library.removeBookById(SECOND_BOOK_ID) }
    }

  private fun book(bookId: String): Book =
    mockk {
      every { id } returns bookId
      every { path } returns temporaryFolder.root.resolve("$bookId.zim").path
      every { getIllustration(any()) } returns null
    }

  private fun bookmark(ofBookId: String, bookmarkUrl: String, bookmarkTitle: String): Bookmark =
    mockk {
      every { bookId } returns ofBookId
      every { bookTitle } returns ofBookId
      every { url } returns bookmarkUrl
      every { title } returns bookmarkTitle
    }

  companion object {
    private const val FIRST_BOOK_ID = "first-book-id"
    private const val SECOND_BOOK_ID = "second-book-id"
    private const val SHARED_URL = "https://kiwix.app/A/Main_Page"
    private const val DELETED_URL = "https://kiwix.app/A/Other_Page"
  }
}
