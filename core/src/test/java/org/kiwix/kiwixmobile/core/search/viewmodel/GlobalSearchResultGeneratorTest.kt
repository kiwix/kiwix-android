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

package org.kiwix.kiwixmobile.core.search.viewmodel

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.search.SearchListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.sharedFunctions.MainDispatcherRule

internal class GlobalSearchResultGeneratorTest {
  @RegisterExtension
  @JvmField
  val mainDispatcherRule = MainDispatcherRule()

  private val libkiwixBookOnDisk: LibkiwixBookOnDisk = mockk()
  private val zimFileReaderFactory: ZimFileReader.Factory = mockk()

  private val generator =
    GlobalSearchResultGeneratorImpl(
      libkiwixBookOnDisk,
      zimFileReaderFactory,
      mainDispatcherRule.dispatcher
    )

  @Test
  fun `blank search term returns empty list`() = runTest {
    assertThat(generator.generateSearchResults("", SearchMode.PAGE_CONTENT)).isEmpty()
  }

  @Test
  fun `results from each book are tagged with the book title and source`() = runTest {
    val searchTerm = "kiwix"
    val bookTitle = "Wikipedia"
    val sourceDb = "/storage/wikipedia.zim"

    val zimReaderSource: ZimReaderSource = mockk()
    every { zimReaderSource.toDatabase() } returns sourceDb
    val libkiwixBook: LibkiwixBook = mockk()
    every { libkiwixBook.title } returns bookTitle
    val bookOnDisk: BookOnDisk = mockk()
    every { bookOnDisk.book } returns libkiwixBook
    every { bookOnDisk.zimReaderSource } returns zimReaderSource
    coEvery { libkiwixBookOnDisk.getBooks() } returns listOf(bookOnDisk)

    val reader: ZimFileReader = mockk(relaxed = true)
    coEvery { zimFileReaderFactory.create(zimReaderSource, false) } returns reader

    val suggestionSearch: SuggestionSearchWrapper = mockk()
    val iterator: SuggestionIteratorWrapper = mockk()
    val item: SuggestionItemWrapper = mockk()
    every { reader.searchFullText(searchTerm) } returns null
    every { reader.searchSuggestions(searchTerm) } returns suggestionSearch
    every { suggestionSearch.getResults(0, GLOBAL_SEARCH_MAX_RESULTS_PER_BOOK) } returns iterator
    every { iterator.hasNext() } returnsMany listOf(true, false)
    every { iterator.next() } returns item
    every { item.title } returns "Kiwix"
    every { item.path } returns "A/Kiwix"

    val results = generator.generateSearchResults(searchTerm, SearchMode.PAGE_CONTENT)

    assertThat(results).isEqualTo(
      listOf(
        SearchListItem.ZimSearchResultListItem(
          value = "Kiwix",
          url = "A/Kiwix",
          snippet = null,
          bookTitle = bookTitle,
          zimReaderSourceDatabaseValue = sourceDb
        )
      )
    )
  }
}
