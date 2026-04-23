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

package org.kiwix.kiwixmobile.core.main

import android.os.Bundle
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.dao.entities.WebViewHistoryEntity
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.page.bookmark.models.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.page.history.models.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.page.notes.models.NoteListItem
import org.kiwix.libkiwix.Book
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class MainRepositoryActionsTest {
  @JvmField
  @RegisterExtension
  val mainDispatcherRule = MainDispatcherRule()

  private val dataSource: DataSource = mockk(relaxed = true)
  private lateinit var mainRepositoryActions: MainRepositoryActions

  @BeforeEach
  fun setUp() {
    clearMocks(dataSource)
    mainRepositoryActions = MainRepositoryActions(
      dataSource,
      mainDispatcherRule.dispatcher
    )
  }

  @Nested
  inner class SaveHistoryTests {
    @Test
    fun `saveHistory delegates to dataSource on success`() = runTest {
      val historyItem: HistoryItem = mockk()
      coEvery { dataSource.saveHistory(historyItem) } just Runs

      mainRepositoryActions.saveHistory(historyItem)

      coVerify(exactly = 1) { dataSource.saveHistory(historyItem) }
    }

    @Test
    fun `saveHistory handles failure gracefully`() = runTest {
      val historyItem: HistoryItem = mockk()
      coEvery { dataSource.saveHistory(historyItem) } throws RuntimeException("DB error")

      // Should not throw — onFailure logs the error
      mainRepositoryActions.saveHistory(historyItem)

      coVerify(exactly = 1) { dataSource.saveHistory(historyItem) }
    }
  }

  @Nested
  inner class SaveBookmarkTests {
    @Test
    fun `saveBookmark delegates to dataSource on success`() = runTest {
      val bookmark: LibkiwixBookmarkItem = mockk()
      coEvery { dataSource.saveBookmark(bookmark) } just Runs

      mainRepositoryActions.saveBookmark(bookmark)

      coVerify(exactly = 1) { dataSource.saveBookmark(bookmark) }
    }

    @Test
    fun `saveBookmark handles failure gracefully`() = runTest {
      val bookmark: LibkiwixBookmarkItem = mockk()
      coEvery { dataSource.saveBookmark(bookmark) } throws RuntimeException("DB error")

      mainRepositoryActions.saveBookmark(bookmark)

      coVerify(exactly = 1) { dataSource.saveBookmark(bookmark) }
    }
  }

  @Nested
  inner class DeleteBookmarkTests {
    @Test
    fun `deleteBookmark delegates to dataSource on success`() = runTest {
      val bookId = "test-book-id"
      val bookmarkUrl = "/article/test"
      coEvery { dataSource.deleteBookmark(bookId, bookmarkUrl) } just Runs

      mainRepositoryActions.deleteBookmark(bookId, bookmarkUrl)

      coVerify(exactly = 1) { dataSource.deleteBookmark(bookId, bookmarkUrl) }
    }

    @Test
    fun `deleteBookmark handles failure gracefully`() = runTest {
      val bookId = "test-book-id"
      val bookmarkUrl = "/article/test"
      coEvery { dataSource.deleteBookmark(bookId, bookmarkUrl) } throws RuntimeException("DB error")

      mainRepositoryActions.deleteBookmark(bookId, bookmarkUrl)
      coVerify(exactly = 1) { dataSource.deleteBookmark(bookId, bookmarkUrl) }
    }
  }

  @Nested
  inner class SaveNoteTests {
    @Test
    fun `saveNote delegates to dataSource on success`() = runTest {
      val note: NoteListItem = mockk()
      coEvery { dataSource.saveNote(note) } just Runs

      mainRepositoryActions.saveNote(note)

      coVerify(exactly = 1) { dataSource.saveNote(note) }
    }

    @Test
    fun `saveNote handles failure gracefully`() = runTest {
      val note: NoteListItem = mockk()
      coEvery { dataSource.saveNote(note) } throws RuntimeException("DB error")

      mainRepositoryActions.saveNote(note)

      coVerify(exactly = 1) { dataSource.saveNote(note) }
    }
  }

  @Nested
  inner class DeleteNoteTests {
    @Test
    fun `deleteNote delegates to dataSource on success`() = runTest {
      val noteTitle = "Test Note"
      coEvery { dataSource.deleteNote(noteTitle) } just Runs

      mainRepositoryActions.deleteNote(noteTitle)

      coVerify(exactly = 1) { dataSource.deleteNote(noteTitle) }
    }

    @Test
    fun `deleteNote handles failure gracefully`() = runTest {
      val noteTitle = "Test Note"
      coEvery { dataSource.deleteNote(noteTitle) } throws RuntimeException("DB error")

      mainRepositoryActions.deleteNote(noteTitle)

      coVerify(exactly = 1) { dataSource.deleteNote(noteTitle) }
    }
  }

  @Nested
  inner class SaveBookTests {
    @Test
    fun `saveBook delegates to dataSource on success`() = runTest {
      val book: Book = mockk()
      coEvery { dataSource.saveBook(book) } just Runs

      mainRepositoryActions.saveBook(book)

      coVerify(exactly = 1) { dataSource.saveBook(book) }
    }

    @Test
    fun `saveBook handles failure gracefully`() = runTest {
      val book: Book = mockk()
      coEvery { dataSource.saveBook(book) } throws RuntimeException("DB error")

      mainRepositoryActions.saveBook(book)

      coVerify(exactly = 1) { dataSource.saveBook(book) }
    }
  }

  @Nested
  inner class WebViewPageHistoryTests {
    @Test
    fun `saveWebViewPageHistory delegates to dataSource`() = runTest {
      val entities = listOf<WebViewHistoryEntity>(mockk(), mockk())
      coEvery { dataSource.insertWebViewPageHistoryItems(entities) } just Runs

      mainRepositoryActions.saveWebViewPageHistory(entities)

      coVerify(exactly = 1) { dataSource.insertWebViewPageHistoryItems(entities) }
    }

    @Test
    fun `clearWebViewPageHistory delegates to dataSource`() = runTest {
      coEvery { dataSource.clearWebViewPagesHistory() } just Runs

      mainRepositoryActions.clearWebViewPageHistory()

      coVerify(exactly = 1) { dataSource.clearWebViewPagesHistory() }
    }

    @Test
    fun `loadWebViewPagesHistory returns mapped WebViewHistoryItems`() = runTest {
      val bundle = mockk<Bundle>()
      val entity1 = WebViewHistoryEntity(
        id = 0,
        zimId = "demoZimId",
        webViewIndex = 0,
        webViewCurrentPosition = 1,
        webViewBackForwardListBundle = bundle
      )
      val entity2 = WebViewHistoryEntity(
        WebViewHistoryItem(
          databaseId = 1,
          zimId = "demoZimId1",
          webViewIndex = 1,
          webViewCurrentPosition = 2,
          webViewBackForwardListBundle = null
        )
      )
      coEvery { dataSource.getAllWebViewPagesHistory() } returns flowOf(listOf(entity1, entity2))

      val result = mainRepositoryActions.loadWebViewPagesHistory()

      assertThat(result).hasSize(2)
      assertThat(result[0].databaseId).isEqualTo(0)
      assertThat(result[0].zimId).isEqualTo("demoZimId")
      assertThat(result[0].webViewIndex).isEqualTo(0)
      assertThat(result[0].webViewCurrentPosition).isEqualTo(1)
      assertThat(result[0].webViewBackForwardListBundle).isEqualTo(bundle)
      assertThat(result[1].databaseId).isEqualTo(1)
      assertThat(result[1].zimId).isEqualTo("demoZimId1")
      assertThat(result[1].webViewIndex).isEqualTo(1)
      assertThat(result[1].webViewCurrentPosition).isEqualTo(2)
      assertThat(result[1].webViewBackForwardListBundle).isEqualTo(null)
    }

    @Test
    fun `loadWebViewPagesHistory returns empty list when no history`() = runTest {
      coEvery { dataSource.getAllWebViewPagesHistory() } returns flowOf(emptyList())
      val result = mainRepositoryActions.loadWebViewPagesHistory()

      assertThat(result).isEmpty()
    }
  }
}
