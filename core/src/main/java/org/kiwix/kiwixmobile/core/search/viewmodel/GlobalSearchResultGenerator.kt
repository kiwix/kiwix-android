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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.search.SearchListItem.ZimSearchResultListItem
import org.kiwix.kiwixmobile.core.utils.files.Log
import kotlin.coroutines.coroutineContext

private const val TAG = "GlobalSearchGenerator"
const val GLOBAL_SEARCH_MAX_RESULTS_PER_BOOK = 10
const val GLOBAL_SEARCH_MAX_TOTAL_RESULTS = 60

/**
 * Runs a search across every ZIM file on the device and returns a flat list of
 * results, each tagged with the book it came from. Each book is opened with a
 * throw-away [ZimFileReader], searched, and immediately disposed, so no native
 * archives are kept alive between searches.
 */
interface GlobalSearchResultGenerator {
  suspend fun generateSearchResults(
    searchTerm: String,
    searchMode: SearchMode
  ): List<ZimSearchResultListItem>
}

class GlobalSearchResultGeneratorImpl @javax.inject.Inject constructor(
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk,
  private val zimFileReaderFactory: ZimFileReader.Factory,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : GlobalSearchResultGenerator {
  override suspend fun generateSearchResults(
    searchTerm: String,
    searchMode: SearchMode
  ): List<ZimSearchResultListItem> {
    if (searchTerm.isBlank()) return emptyList()
    return withContext(ioDispatcher) {
      val books = runCatching { libkiwixBookOnDisk.getBooks() }
        .getOrDefault(emptyList())
        .sortedBy { it.book.title.lowercase() }
      val results = mutableListOf<ZimSearchResultListItem>()
      for (bookOnDisk in books) {
        coroutineContext.ensureActive()
        if (results.size >= GLOBAL_SEARCH_MAX_TOTAL_RESULTS) break
        results += searchInBook(bookOnDisk, searchTerm, searchMode)
      }
      results.take(GLOBAL_SEARCH_MAX_TOTAL_RESULTS)
    }
  }

  @Suppress("TooGenericExceptionCaught")
  private suspend fun searchInBook(
    bookOnDisk: org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk,
    searchTerm: String,
    searchMode: SearchMode
  ): List<ZimSearchResultListItem> {
    val reader =
      runCatching {
        zimFileReaderFactory.create(bookOnDisk.zimReaderSource, false)
      }.getOrNull() ?: return emptyList()
    return try {
      collectResults(reader, bookOnDisk, searchTerm, searchMode)
    } catch (exception: Exception) {
      Log.e(TAG, "Could not search in ${bookOnDisk.book.title}. $exception")
      emptyList()
    } finally {
      runCatching { reader.dispose() }
    }
  }

  @Suppress("NestedBlockDepth", "ReturnCount")
  private fun collectResults(
    reader: ZimFileReader,
    bookOnDisk: org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk,
    searchTerm: String,
    searchMode: SearchMode
  ): List<ZimSearchResultListItem> {
    val bookTitle = bookOnDisk.book.title
    val sourceDb = bookOnDisk.zimReaderSource.toDatabase()
    val results = mutableListOf<ZimSearchResultListItem>()
    if (searchMode == SearchMode.PAGE_CONTENT) {
      val search = reader.searchFullText(searchTerm)
      if (search != null) {
        val iterator = search.getResults(0, GLOBAL_SEARCH_MAX_RESULTS_PER_BOOK)
        while (iterator.hasNext()) {
          // Read the snippet from the iterator's current position *before*
          // next() advances it; otherwise each result shows the next result's
          // snippet.
          val snippet = runCatching { iterator.snippet }.getOrNull()
          val entry = iterator.next()
          results.add(
            ZimSearchResultListItem(entry.title, entry.path, snippet, bookTitle, sourceDb)
          )
        }
        return results
      }
    }
    // Title search, or full text fallback for books without a full text index.
    val suggestionSearch = reader.searchSuggestions(searchTerm) ?: return results
    val iterator = suggestionSearch.getResults(0, GLOBAL_SEARCH_MAX_RESULTS_PER_BOOK)
    while (iterator.hasNext()) {
      val entry = iterator.next()
      results.add(
        ZimSearchResultListItem(entry.title, entry.path, null, bookTitle, sourceDb)
      )
    }
    return results
  }
}
