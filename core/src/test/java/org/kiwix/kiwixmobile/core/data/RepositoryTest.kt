/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.data

import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.dao.HistoryRoomDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.dao.NotesRoomDao
import org.kiwix.kiwixmobile.core.dao.RecentSearchRoomDao
import org.kiwix.kiwixmobile.core.dao.WebViewHistoryRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.WebViewHistoryEntity
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.bookmark.models.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.page.history.models.HistoryListItem
import org.kiwix.kiwixmobile.core.page.history.models.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.notes.models.NoteListItem
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.libkiwix.Book
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryTest {
  @JvmField
  @RegisterExtension
  val mainDispatcherRule = MainDispatcherRule()

  private val libkiwixBookOnDisk: LibkiwixBookOnDisk = mockk(relaxed = true)
  private val libkiwixBookmarks: LibkiwixBookmarks = mockk(relaxed = true)
  private val historyRoomDao: HistoryRoomDao = mockk(relaxed = true)
  private val webViewHistoryRoomDao: WebViewHistoryRoomDao = mockk(relaxed = true)
  private val notesRoomDao: NotesRoomDao = mockk(relaxed = true)
  private val recentSearchRoomDao: RecentSearchRoomDao = mockk(relaxed = true)
  private val zimReaderContainer: ZimReaderContainer = mockk(relaxed = true)

  private lateinit var repository: Repository

  @BeforeEach
  fun setUp() {
    clearMocks(
      libkiwixBookOnDisk,
      libkiwixBookmarks,
      historyRoomDao,
      webViewHistoryRoomDao,
      notesRoomDao,
      recentSearchRoomDao,
      zimReaderContainer
    )
    repository = Repository(
      libkiwixBookOnDisk,
      libkiwixBookmarks,
      historyRoomDao,
      webViewHistoryRoomDao,
      notesRoomDao,
      recentSearchRoomDao,
      zimReaderContainer,
      mainDispatcherRule.dispatcher
    )
  }

  @Nested
  inner class SaveBooksTests {
    @Test
    fun `saveBooks delegates to libkiwixBookOnDisk insert`() = runTest {
      val books = listOf<Book>(mockk(), mockk())
      coEvery { libkiwixBookOnDisk.insert(books) } just Runs

      repository.saveBooks(books)

      coVerify(exactly = 1) { libkiwixBookOnDisk.insert(books) }
    }

    @Test
    fun `saveBooks with empty list delegates to libkiwixBookOnDisk insert`() = runTest {
      val emptyBooks = emptyList<Book>()
      coEvery { libkiwixBookOnDisk.insert(emptyBooks) } just Runs

      repository.saveBooks(emptyBooks)

      coVerify(exactly = 1) { libkiwixBookOnDisk.insert(emptyBooks) }
    }

    @Test
    fun `saveBook wraps single book in list and delegates to insert`() = runTest {
      val book: Book = mockk()
      coEvery { libkiwixBookOnDisk.insert(listOf(book)) } just Runs

      repository.saveBook(book)

      coVerify(exactly = 1) { libkiwixBookOnDisk.insert(listOf(book)) }
    }
  }

  @Nested
  inner class HistoryTests {
    @Test
    fun `saveHistory delegates to historyRoomDao`() = runTest {
      val historyItem: HistoryItem = mockk()
      coEvery { historyRoomDao.saveHistory(historyItem) } just Runs

      repository.saveHistory(historyItem)

      coVerify(exactly = 1) { historyRoomDao.saveHistory(historyItem) }
    }

    @Test
    fun `deleteHistory filters HistoryItems and delegates to historyRoomDao`() = runTest {
      val historyItem1: HistoryItem = mockk()
      val historyItem2: HistoryItem = mockk()
      val dateItem: HistoryListItem.DateItem = mockk()
      val mixedList: List<HistoryListItem> = listOf(historyItem1, dateItem, historyItem2)

      every { historyRoomDao.deleteHistory(any()) } just Runs

      repository.deleteHistory(mixedList)

      // Only HistoryItem instances should be passed, DateItem should be filtered out
      verify(exactly = 1) {
        historyRoomDao.deleteHistory(listOf(historyItem1, historyItem2))
      }
    }

    @Test
    fun `deleteHistory with empty list delegates empty list`() = runTest {
      val emptyList = emptyList<HistoryListItem>()
      every { historyRoomDao.deleteHistory(any()) } just Runs

      repository.deleteHistory(emptyList)

      verify(exactly = 1) { historyRoomDao.deleteHistory(match { it.isEmpty() }) }
    }

    @Test
    fun `deleteHistory with only DateItems results in empty HistoryItem list`() = runTest {
      val dateItem1: HistoryListItem.DateItem = mockk()
      val dateItem2: HistoryListItem.DateItem = mockk()
      val dateOnlyList: List<HistoryListItem> = listOf(dateItem1, dateItem2)

      every { historyRoomDao.deleteHistory(any()) } just Runs

      repository.deleteHistory(dateOnlyList)

      verify(exactly = 1) { historyRoomDao.deleteHistory(match { it.isEmpty() }) }
    }

    @Test
    fun `clearHistory deletes all history and search history`() = runTest {
      every { historyRoomDao.deleteAllHistory() } just Runs
      every { recentSearchRoomDao.deleteSearchHistory() } just Runs

      repository.clearHistory()

      verify(exactly = 1) { historyRoomDao.deleteAllHistory() }
      verify(exactly = 1) { recentSearchRoomDao.deleteSearchHistory() }
    }
  }

  @Nested
  inner class BookmarkTests {
    @Test
    fun `getBookmarks returns flow from libkiwixBookmarks`() = runTest {
      val bookmarkItem1: LibkiwixBookmarkItem = mockk()
      val bookmarkItem2: LibkiwixBookmarkItem = mockk()
      val bookmarkFlow = flowOf(listOf(bookmarkItem1, bookmarkItem2) as List<Page>)

      every { libkiwixBookmarks.bookmarks() } returns bookmarkFlow

      repository.getBookmarks().test {
        val items = awaitItem()
        assertThat(items).hasSize(2)
        assertThat(items).containsExactly(bookmarkItem1, bookmarkItem2)
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `getBookmarks returns empty list when no bookmarks`() = runTest {
      val emptyFlow = flowOf(emptyList<Page>())
      every { libkiwixBookmarks.bookmarks() } returns emptyFlow

      repository.getBookmarks().test {
        val items = awaitItem()
        assertThat(items).isEmpty()
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `getCurrentZimBookmarksUrl delegates to libkiwixBookmarks`() = runTest {
      val zimFileReader: ZimFileReader = mockk()
      val expectedUrls = listOf("/article1", "/article2")

      every { zimReaderContainer.zimFileReader } returns zimFileReader
      coEvery {
        libkiwixBookmarks.getCurrentZimBookmarksUrl(zimFileReader)
      } returns expectedUrls

      val result = repository.getCurrentZimBookmarksUrl()

      assertThat(result).isEqualTo(expectedUrls)
      coVerify(exactly = 1) {
        libkiwixBookmarks.getCurrentZimBookmarksUrl(zimFileReader)
      }
    }

    @Test
    fun `getCurrentZimBookmarksUrl with null zimFileReader`() = runTest {
      every { zimReaderContainer.zimFileReader } returns null
      coEvery {
        libkiwixBookmarks.getCurrentZimBookmarksUrl(null)
      } returns emptyList()

      val result = repository.getCurrentZimBookmarksUrl()

      assertThat(result).isEmpty()
    }

    @Test
    fun `saveBookmark delegates to libkiwixBookmarks`() = runTest {
      val bookmarkItem: LibkiwixBookmarkItem = mockk()
      coEvery { libkiwixBookmarks.saveBookmark(bookmarkItem) } just Runs

      repository.saveBookmark(bookmarkItem)

      coVerify(exactly = 1) { libkiwixBookmarks.saveBookmark(bookmarkItem) }
    }

    @Test
    fun `deleteBookmarks delegates list to libkiwixBookmarks`() = runTest {
      val bookmarks = listOf<LibkiwixBookmarkItem>(mockk(), mockk())
      every { libkiwixBookmarks.deleteBookmarks(bookmarks) } just Runs

      repository.deleteBookmarks(bookmarks)

      verify(exactly = 1) { libkiwixBookmarks.deleteBookmarks(bookmarks) }
    }

    @Test
    fun `deleteBookmarks with empty list`() = runTest {
      val emptyBookmarks = emptyList<LibkiwixBookmarkItem>()
      every { libkiwixBookmarks.deleteBookmarks(emptyBookmarks) } just Runs

      repository.deleteBookmarks(emptyBookmarks)

      verify(exactly = 1) { libkiwixBookmarks.deleteBookmarks(emptyBookmarks) }
    }

    @Test
    fun `deleteBookmark delegates bookId and url to libkiwixBookmarks`() = runTest {
      val bookId = "test-book-id"
      val bookmarkUrl = "/article/test"
      every { libkiwixBookmarks.deleteBookmark(bookId, bookmarkUrl) } just Runs

      repository.deleteBookmark(bookId, bookmarkUrl)

      verify(exactly = 1) { libkiwixBookmarks.deleteBookmark(bookId, bookmarkUrl) }
    }

    @Test
    fun `deleteBookmark with empty bookId and url`() = runTest {
      every { libkiwixBookmarks.deleteBookmark("", "") } just Runs

      repository.deleteBookmark("", "")

      verify(exactly = 1) { libkiwixBookmarks.deleteBookmark("", "") }
    }
  }

  @Nested
  inner class NotesTests {
    @Test
    fun `saveNote delegates to notesRoomDao`() = runTest {
      val noteItem: NoteListItem = mockk()
      every { notesRoomDao.saveNote(noteItem) } just Runs

      repository.saveNote(noteItem)

      verify(exactly = 1) { notesRoomDao.saveNote(noteItem) }
    }

    @Test
    fun `deleteNote delegates noteTitle to notesRoomDao`() = runTest {
      val noteTitle = "Test Note Title"
      every { notesRoomDao.deleteNote(noteTitle) } just Runs

      repository.deleteNote(noteTitle)

      verify(exactly = 1) { notesRoomDao.deleteNote(noteTitle) }
    }

    @Test
    fun `deleteNote with empty title`() = runTest {
      every { notesRoomDao.deleteNote("") } just Runs

      repository.deleteNote("")

      verify(exactly = 1) { notesRoomDao.deleteNote("") }
    }

    @Test
    fun `clearNotes fetches notes and deletes them`() = runTest {
      val note1: NoteListItem = mockk()
      val note2: NoteListItem = mockk()
      // notes() returns Flow<List<Page>>, first() is used in the impl
      every { notesRoomDao.notes() } returns flowOf(listOf(note1, note2))
      every { notesRoomDao.deleteNotes(any()) } just Runs

      repository.clearNotes()

      verify(exactly = 1) { notesRoomDao.notes() }
      verify(exactly = 1) { notesRoomDao.deleteNotes(match { it.size == 2 && it.containsAll(listOf(note1, note2)) }) }
    }

    @Test
    fun `clearNotes with empty notes list`() = runTest {
      every { notesRoomDao.notes() } returns flowOf(emptyList())
      every { notesRoomDao.deleteNotes(any()) } just Runs

      repository.clearNotes()

      verify(exactly = 1) { notesRoomDao.deleteNotes(emptyList()) }
    }
  }

  @Nested
  inner class WebViewHistoryTests {
    @Test
    fun `insertWebViewPageHistoryItems delegates to webViewHistoryRoomDao`() = runTest {
      val entities = listOf<WebViewHistoryEntity>(mockk(), mockk())
      every {
        webViewHistoryRoomDao.insertWebViewPageHistoryItems(entities)
      } just Runs

      repository.insertWebViewPageHistoryItems(entities)

      verify(exactly = 1) {
        webViewHistoryRoomDao.insertWebViewPageHistoryItems(entities)
      }
    }

    @Test
    fun `insertWebViewPageHistoryItems with empty list`() = runTest {
      val emptyEntities = emptyList<WebViewHistoryEntity>()
      every {
        webViewHistoryRoomDao.insertWebViewPageHistoryItems(emptyEntities)
      } just Runs

      repository.insertWebViewPageHistoryItems(emptyEntities)

      verify(exactly = 1) {
        webViewHistoryRoomDao.insertWebViewPageHistoryItems(emptyEntities)
      }
    }

    @Test
    fun `getAllWebViewPagesHistory returns flow from webViewHistoryRoomDao`() = runTest {
      val entity1: WebViewHistoryEntity = mockk()
      val entity2: WebViewHistoryEntity = mockk()
      val historyFlow = flowOf(listOf(entity1, entity2))

      every { webViewHistoryRoomDao.getAllWebViewPagesHistory() } returns historyFlow

      repository.getAllWebViewPagesHistory().test {
        val items = awaitItem()
        assertThat(items).hasSize(2)
        assertThat(items).containsExactly(entity1, entity2)
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `getAllWebViewPagesHistory returns empty flow`() = runTest {
      every {
        webViewHistoryRoomDao.getAllWebViewPagesHistory()
      } returns flowOf(emptyList())

      repository.getAllWebViewPagesHistory().test {
        val items = awaitItem()
        assertThat(items).isEmpty()
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `clearWebViewPagesHistory delegates to webViewHistoryRoomDao`() = runTest {
      every { webViewHistoryRoomDao.clearWebViewPagesHistory() } just Runs

      repository.clearWebViewPagesHistory()

      verify(exactly = 1) { webViewHistoryRoomDao.clearWebViewPagesHistory() }
    }
  }

  @Nested
  inner class BooksOnDiskTests {
    @Test
    fun `getLanguageCategorizedBooks returns empty list when no books`() = runTest {
      every { libkiwixBookOnDisk.books() } returns flowOf(emptyList())

      repository.getLanguageCategorizedBooks().test {
        val items = awaitItem()
        assertThat(items).isEmpty()
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `getLanguageCategorizedBooks returns list of book`() = runTest {
      val bookOnDisk = BookOnDisk(LibkiwixBook())
      every { libkiwixBookOnDisk.books() } returns flowOf(listOf(bookOnDisk))

      repository.getLanguageCategorizedBooks().test {
        val items = awaitItem()
        assertThat(items[0]).isInstanceOf(BooksOnDiskListItem.LanguageItem::class.java)
        assertThat(items[1]).isEqualTo(bookOnDisk)
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `booksOnDiskAsListItems splits languages with commas but only keeps first due to distinctBy`() =
      runTest {
        val bookWithMultipleLanguages = LibkiwixBook(
          _id = "1",
          _title = "Book 1",
          _language = "en, fr",
          _path = "/path1"
        )
        val bookOnDisk = BookOnDisk(book = bookWithMultipleLanguages, zimReaderSource = mockk())

        every { libkiwixBookOnDisk.books() } returns flowOf(listOf(bookOnDisk))

        repository.booksOnDiskAsListItems().test {
          val items = awaitItem()
          // Current behavior: Only the first language survives the distinctBy { it.book.id }
          assertThat(items).hasSize(2) // 1 Header (en) + 1 Book

          assertThat(items[0]).isInstanceOf(BooksOnDiskListItem.LanguageItem::class.java)
          assertThat((items[0] as BooksOnDiskListItem.LanguageItem).id).isEqualTo("en")
          assertThat((items[1] as BookOnDisk).book.language).isEqualTo("en")
          cancelAndIgnoreRemainingEvents()
        }
      }

    @Test
    fun `booksOnDiskAsListItems sorts by language and title then adds headers`() = runTest {
      val bookEnB = LibkiwixBook(_id = "2", _title = "B", _language = "en", _path = "/p2")
      val bookEnA = LibkiwixBook(_id = "1", _title = "A", _language = "en", _path = "/p1")
      val bookFr = LibkiwixBook(_id = "3", _title = "C", _language = "fr", _path = "/p3")

      val list = listOf(
        BookOnDisk(book = bookEnB, zimReaderSource = mockk()),
        BookOnDisk(book = bookEnA, zimReaderSource = mockk()),
        BookOnDisk(book = bookFr, zimReaderSource = mockk())
      )

      every { libkiwixBookOnDisk.books() } returns flowOf(list)

      repository.booksOnDiskAsListItems().test {
        val items = awaitItem()
        // Expected order:
        // 1. LanguageItem(en)
        // 2. BookOnDisk(A, en)
        // 3. BookOnDisk(B, en)
        // 4. LanguageItem(fr)
        // 5. BookOnDisk(C, fr)

        assertThat(items).hasSize(5)
        assertThat((items[0] as BooksOnDiskListItem.LanguageItem).id).isEqualTo("en")
        assertThat((items[1] as BookOnDisk).book.title).isEqualTo("A")
        assertThat((items[2] as BookOnDisk).book.title).isEqualTo("B")
        assertThat((items[3] as BooksOnDiskListItem.LanguageItem).id).isEqualTo("fr")
        assertThat((items[4] as BookOnDisk).book.title).isEqualTo("C")
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `booksOnDiskAsListItems distincts by book id`() = runTest {
      val book1 = LibkiwixBook(_id = "1", _title = "Book 1", _language = "en", _path = "/p1")
      val book1Duplicate =
        LibkiwixBook(_id = "1", _title = "Book 1 Dup", _language = "en", _path = "/p1-alt")

      val list = listOf(
        BookOnDisk(book = book1, zimReaderSource = mockk()),
        BookOnDisk(book = book1Duplicate, zimReaderSource = mockk())
      )

      every { libkiwixBookOnDisk.books() } returns flowOf(list)

      repository.booksOnDiskAsListItems().test {
        val items = awaitItem()
        // Only one book with ID "1" should remain
        assertThat(items.filterIsInstance<BookOnDisk>()).hasSize(1)
        cancelAndIgnoreRemainingEvents()
      }
    }
  }
}
