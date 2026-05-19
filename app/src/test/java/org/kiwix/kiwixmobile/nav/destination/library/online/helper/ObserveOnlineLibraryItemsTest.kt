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

package org.kiwix.kiwixmobile.nav.destination.library.online.helper

import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.webserver.BookTestWrapper
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveOnlineLibraryItemsTest {
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val fat32Checker: Fat32Checker = mockk()

  @RegisterExtension
  @JvmField
  val dispatcherRule = MainDispatcherRule()

  private lateinit var observeOnlineLibraryItems: ObserveOnlineLibraryItems

  @BeforeEach
  fun setup() {
    observeOnlineLibraryItems = ObserveOnlineLibraryItems(
      kiwixDataStore,
      fat32Checker,
      dispatcherRule.dispatcher
    )
  }

  private fun book(id: String) = LibkiwixBook().apply {
    this.id = id
  }

  private fun download(book: LibkiwixBook) =
    mockk<DownloadModel> {
      every { this@mockk.book } returns book
      every { downloadId } returns book.id.toLong()
      every { bytesDownloaded } returns 100L
      every { totalSizeOfDownload } returns 1000L
      every { progress } returns 10
      every { etaInMilliSeconds } returns 100
      every { state } returns Status.DOWNLOADING
      every { error } returns Error.NONE
    }

  private val fsState = CanWrite4GbFile

  private val getString: (Int, Array<out Any>) -> String = { _, args ->
    "Your language: ${args[0]}"
  }

  private val getSimpleString: (Int) -> String = {
    "All languages"
  }

  @Test
  fun `emits books in main section when no downloads`() = runTest {
    every { fat32Checker.fileSystemStates } returns MutableStateFlow(fsState)
    every { kiwixDataStore.selectedOnlineContentLanguage } returns flowOf("")
    every { kiwixDataStore.selectedOnlineContentCategory } returns flowOf("")

    val result = observeOnlineLibraryItems(
      localBooks = flowOf(emptyList()),
      downloads = flowOf(emptyList()),
      networkBooks = flowOf(listOf(book("1"), book("2"))),
      getString = getString,
      getSimpleString = getSimpleString
    ).first()

    assertTrue(result.any { it is LibraryListItem.DividerItem })
    assertEquals(3, result.size)
  }

  @Test
  fun `local books are excluded from network books`() = runTest {
    val local = BookTestWrapper("id")
    val localLibBook = LibkiwixBook(local)

    every { fat32Checker.fileSystemStates } returns MutableStateFlow(fsState)
    every { kiwixDataStore.selectedOnlineContentLanguage } returns flowOf("")
    every { kiwixDataStore.selectedOnlineContentCategory } returns flowOf("")

    val result = observeOnlineLibraryItems(
      localBooks = flowOf(listOf(local)),
      downloads = flowOf(emptyList()),
      networkBooks = flowOf(listOf(localLibBook, book("2"))),
      getString = getString,
      getSimpleString = getSimpleString
    ).first()

    assertTrue(result.none { it is LibraryListItem.BookItem && it.book == localLibBook })
  }

  @Test
  fun `downloading books appear in downloading section`() = runTest {
    val b1 = book("1")
    val download = download(b1)

    every { fat32Checker.fileSystemStates } returns MutableStateFlow(fsState)
    every { kiwixDataStore.selectedOnlineContentLanguage } returns flowOf("")
    every { kiwixDataStore.selectedOnlineContentCategory } returns flowOf("")

    val result = observeOnlineLibraryItems(
      localBooks = flowOf(emptyList()),
      downloads = flowOf(listOf(download)),
      networkBooks = flowOf(listOf(b1)),
      getString = getString,
      getSimpleString = getSimpleString
    ).first()

    assertTrue(result.any { it is LibraryListItem.LibraryDownloadItem })
  }

  @Test
  fun `downloading books are removed from main section`() = runTest {
    val b1 = book("1")
    val download = download(b1)

    every { fat32Checker.fileSystemStates } returns MutableStateFlow(fsState)
    every { kiwixDataStore.selectedOnlineContentLanguage } returns flowOf("")
    every { kiwixDataStore.selectedOnlineContentCategory } returns flowOf("")

    val result = observeOnlineLibraryItems(
      localBooks = flowOf(emptyList()),
      downloads = flowOf(listOf(download)),
      networkBooks = flowOf(listOf(b1)),
      getString = getString,
      getSimpleString = getSimpleString
    ).first()

    val mainBooks = result.filterIsInstance<LibraryListItem.BookItem>()

    assertTrue(mainBooks.none { it.book == b1 })
  }

  @Test
  fun `uses language specific title when language selected`() = runTest {
    every { fat32Checker.fileSystemStates } returns MutableStateFlow(fsState)
    every { kiwixDataStore.selectedOnlineContentLanguage } returns flowOf("en")
    every { kiwixDataStore.selectedOnlineContentCategory } returns flowOf("")

    val result = observeOnlineLibraryItems(
      localBooks = flowOf(emptyList()),
      downloads = flowOf(emptyList()),
      networkBooks = flowOf(listOf(book("1"))),
      getString = getString,
      getSimpleString = getSimpleString
    ).first()

    val divider = result.first() as LibraryListItem.DividerItem

    assertTrue(divider.sectionTitle.contains("English"))
  }

  @Test
  fun `no section added when no books`() = runTest {
    every { fat32Checker.fileSystemStates } returns MutableStateFlow(fsState)
    every { kiwixDataStore.selectedOnlineContentLanguage } returns flowOf("")
    every { kiwixDataStore.selectedOnlineContentCategory } returns flowOf("")

    val result = observeOnlineLibraryItems(
      localBooks = flowOf(emptyList()),
      downloads = flowOf(emptyList()),
      networkBooks = flowOf(emptyList()),
      getString = getString,
      getSimpleString = getSimpleString
    ).first()

    assertTrue(result.isEmpty())
  }

  @Test
  fun `dispose should clear the fat32Checker`() {
    observeOnlineLibraryItems.dispose()

    verify { fat32Checker.dispose() }
  }
}
