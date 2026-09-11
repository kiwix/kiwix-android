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

package org.kiwix.kiwixmobile.core.main.reader.helper

import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.BookTestWrapper
import org.kiwix.kiwixmobile.core.LibkiwixBookFactory
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer

class BookmarkManagerTest {
  private lateinit var bookmarkManager: BookmarkManager

  private val libkiwixBookmarks = mockk<LibkiwixBookmarks>()
  private val zimReaderContainer = mockk<ZimReaderContainer>()
  private val mainRepositoryActions = mockk<MainRepositoryActions>()
  private val libkiwixBookFactory = mockk<LibkiwixBookFactory>()

  @BeforeEach
  fun setup() {
    clearAllMocks()
    bookmarkManager = BookmarkManager(
      libkiwixBookmarks,
      zimReaderContainer,
      mainRepositoryActions,
      libkiwixBookFactory
    )
  }

  @Nested
  inner class AddBookmarkTest {
    @Test
    fun `addBookmark returns Failure when page title is null`() = runTest {
      every { zimReaderContainer.zimFileReader } returns mockZimReader()

      val result = bookmarkManager.addBookmark(
        pageTitle = null,
        articleUrl = "article",
        isBookmarked = false
      )

      assertEquals(
        BookmarkManager.BookmarkSaveResult.Failure(
          string.unable_to_add_to_bookmarks
        ),
        result
      )

      coVerify(exactly = 0) {
        mainRepositoryActions.saveBookmark(any())
      }
    }

    @Test
    fun `addBookmark returns Failure when article url is null`() = runTest {
      every { zimReaderContainer.zimFileReader } returns mockZimReader()

      val result = bookmarkManager.addBookmark(
        pageTitle = "Title",
        articleUrl = null,
        isBookmarked = false
      )

      assertEquals(
        BookmarkManager.BookmarkSaveResult.Failure(
          string.unable_to_add_to_bookmarks
        ),
        result
      )
    }

    @Test
    fun `addBookmark returns Failure when zim reader is null`() = runTest {
      every { zimReaderContainer.zimFileReader } returns null

      val result = bookmarkManager.addBookmark(
        "Title",
        "article",
        false
      )

      assertEquals(
        BookmarkManager.BookmarkSaveResult.Failure(
          string.unable_to_add_to_bookmarks
        ),
        result
      )
    }

    @Test
    fun `addBookmark saves bookmark`() = runTest {
      every { zimReaderContainer.zimFileReader } returns mockZimReader()
      every { libkiwixBookFactory.create() } returns BookTestWrapper("id")

      coEvery {
        mainRepositoryActions.saveBookmark(any())
      } just Runs

      val result = bookmarkManager.addBookmark(
        "Title",
        "article",
        false
      )

      assertEquals(
        BookmarkManager.BookmarkSaveResult.BookmarkAdded,
        result
      )

      coVerify {
        mainRepositoryActions.saveBookmark(any())
      }
    }

    @Test
    fun `addBookmark removes bookmark when already bookmarked`() = runTest {
      every { zimReaderContainer.zimFileReader } returns mockZimReader()
      every { libkiwixBookFactory.create() } returns BookTestWrapper("id")
      coEvery {
        mainRepositoryActions.deleteBookmark(any(), any())
      } just Runs

      val result = bookmarkManager.addBookmark(
        "Title",
        "article",
        true
      )

      assertEquals(
        BookmarkManager.BookmarkSaveResult.BookmarkRemoved,
        result
      )

      coVerify {
        mainRepositoryActions.deleteBookmark(any(), "article")
      }
    }

    @Test
    fun `addBookmark returns Failure when save throws`() = runTest {
      every { zimReaderContainer.zimFileReader } returns mockZimReader()

      coEvery {
        mainRepositoryActions.saveBookmark(any())
      } throws RuntimeException()

      val result = bookmarkManager.addBookmark(
        "Title",
        "article",
        false
      )

      assertEquals(
        BookmarkManager.BookmarkSaveResult.Failure(
          string.unable_to_add_to_bookmarks
        ),
        result
      )
    }

    @Test
    fun `addBookmark returns Failure when delete throws`() = runTest {
      every { zimReaderContainer.zimFileReader } returns mockZimReader()

      coEvery {
        mainRepositoryActions.deleteBookmark(any(), any())
      } throws RuntimeException()

      val result = bookmarkManager.addBookmark(
        "Title",
        "article",
        true
      )

      assertEquals(
        BookmarkManager.BookmarkSaveResult.Failure(
          string.unable_to_add_to_bookmarks
        ),
        result
      )
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Nested
  inner class ObserveBookmarksTest {
    @Test
    fun `observeBookmarks updates bookmark state when current url is bookmarked`() = runTest {
      val currentUrl = MutableStateFlow("article1")
      val bookmarkUrls = MutableStateFlow(listOf("article1", "article2"))

      every {
        libkiwixBookmarks.bookmarkUrlsForCurrentBook("zim-id")
      } returns bookmarkUrls

      bookmarkManager.bookmarkState.test {
        // Initial value
        assertFalse(awaitItem().isBookmarked)

        bookmarkManager.observeBookmarks(
          scope = backgroundScope,
          zimFileReaderId = "zim-id",
          webUrlsFlow = currentUrl
        )

        assertTrue(awaitItem().isBookmarked)
      }
    }

    @Test
    fun `observeBookmarks updates bookmark state when current url is not bookmarked`() = runTest {
      val currentUrl = MutableStateFlow("article3")
      val bookmarkUrls = MutableStateFlow(listOf("article1", "article2"))
      every {
        libkiwixBookmarks.bookmarkUrlsForCurrentBook("zim-id")
      } returns bookmarkUrls

      bookmarkManager.bookmarkState.test {
        bookmarkManager.observeBookmarks(
          scope = backgroundScope,
          zimFileReaderId = "zim-id",
          webUrlsFlow = currentUrl
        )

        assertFalse(awaitItem().isBookmarked)
      }
    }

    @Test
    fun `observeBookmarks updates state when current url changes`() = runTest {
      val currentUrl = MutableStateFlow("article1")
      val bookmarkUrls = MutableStateFlow(listOf("article1", "article2"))
      every {
        libkiwixBookmarks.bookmarkUrlsForCurrentBook("zim-id")
      } returns bookmarkUrls

      bookmarkManager.bookmarkState.test {
        // Initial value
        assertFalse(awaitItem().isBookmarked)

        bookmarkManager.observeBookmarks(
          scope = backgroundScope,
          zimFileReaderId = "zim-id",
          webUrlsFlow = currentUrl
        )

        assertTrue(awaitItem().isBookmarked)
        currentUrl.value = "article5"
        assertFalse(awaitItem().isBookmarked)
      }
    }
  }

  @Nested
  inner class StopObservingTest {
    @Test
    fun `stopObserving without active observer does not throw`() {
      bookmarkManager.stopObserving()

      assertEquals(
        BookmarkManager.BookmarkState(),
        bookmarkManager.bookmarkState.value
      )
    }
  }

  private fun mockZimReader(): ZimFileReader = mockk<ZimFileReader>(relaxed = true).apply {
    every { jniKiwixReader } returns mockk(relaxed = true)
  }
}
