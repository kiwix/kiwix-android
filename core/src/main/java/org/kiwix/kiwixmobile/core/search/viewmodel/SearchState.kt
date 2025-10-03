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

package org.kiwix.kiwixmobile.core.search.viewmodel

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.kiwix.kiwixmobile.core.search.SearchListItem
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.libzim.SuggestionSearch

data class SearchState(
  val searchTerm: String,
  val searchResultsWithTerm: SearchResultsWithTerm,
  val recentResults: List<SearchListItem.RecentSearchListItem>,
  val searchOrigin: SearchOrigin
) {
  suspend fun getVisibleResults(
    startIndex: Int,
    job: Job? = null,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
  ): List<SearchListItem>? {
    if (searchTerm.isEmpty()) return recentResults
    return searchResultsWithTerm.searchMutex.withLock {
      searchResultsWithTerm.suggestionSearch?.let {
        yield()
        withContext(ioDispatcher) {
          fetchSearchResults(it, startIndex, job)
        }
      } ?: kotlin.run {
        recentResults
      }
    }
  }

  @Suppress("MagicNumber")
  private suspend fun fetchSearchResults(
    suggestionSearch: SuggestionSearch,
    startIndex: Int,
    job: Job?
  ): List<SearchListItem.ZimSearchResultListItem>? {
    val results = mutableListOf<SearchListItem.ZimSearchResultListItem>()

    // if the previous job is cancel then do not execute the code
    if (job?.isActive == false) return results

    runCatching {
      val safeEndIndex = startIndex + 20
      yield()
      val searchIterator = suggestionSearch.getResults(startIndex, safeEndIndex)
      while (searchIterator.hasNext()) {
        // check if the previous job is cancel while retrieving the data for
        // previous searched item then break the execution of code.
        if (job?.isActive == false) break
        yield()
        val entry = searchIterator.next()
        results.add(SearchListItem.ZimSearchResultListItem(entry.title, entry.path))
      }
    }.onFailure {
      Log.e(
        "SearchState",
        "Could not get the searched result for searchTerm $searchTerm\n" +
          "Original exception = $it"
      )
    }

    /**
     * Returns null if there are no suggestions left in the iterator.
     * We check this in SearchFragment to avoid unnecessary data loading
     * while scrolling to the end of the list when there are no items available.
     */
    return results.ifEmpty { null }
  }

  val isLoading = searchTerm != searchResultsWithTerm.searchTerm
}

enum class SearchOrigin {
  FromWebView,
  FromTabView
}
