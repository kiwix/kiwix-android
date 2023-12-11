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

import kotlinx.coroutines.Job
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem

data class SearchState(
  val searchTerm: String,
  val searchResultsWithTerm: SearchResultsWithTerm,
  val recentResults: List<SearchListItem.RecentSearchListItem>,
  val searchOrigin: SearchOrigin
) {
  @Suppress("NestedBlockDepth")
  fun getVisibleResults(
    startIndex: Int,
    job: Job? = null
  ): List<SearchListItem.RecentSearchListItem>? =
    if (searchTerm.isEmpty()) {
      recentResults
    } else {
      searchResultsWithTerm.suggestionSearch?.let {
        val searchResults = mutableListOf<SearchListItem.RecentSearchListItem>()
        if (job?.isActive == false) {
          // if the previous job is cancel then do not execute the code
          return@getVisibleResults searchResults
        }
        val safeEndIndex = startIndex + 100
        val searchIterator =
          it.getResults(startIndex, safeEndIndex)
        while (searchIterator.hasNext()) {
          if (job?.isActive == false) {
            // check if the previous job is cancel while retrieving the data for previous searched item
            // then break the execution of code.
            break
          }
          val entry = searchIterator.next()
          searchResults.add(SearchListItem.RecentSearchListItem(entry.title, entry.path))
        }
        /**
         * Returns null if there are no suggestions left in the iterator.
         * We check this in SearchFragment to avoid unnecessary data loading
         * while scrolling to the end of the list when there are no items available.
         */
        searchResults.ifEmpty { null }
      } ?: kotlin.run {
        recentResults
      }
    }

  val isLoading = searchTerm != searchResultsWithTerm.searchTerm
}

enum class SearchOrigin {
  FromWebView,
  FromTabView
}
