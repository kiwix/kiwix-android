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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.kiwix.kiwixmobile.core.reader.ZimReader
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.ZimSearchResultListItem
import javax.inject.Inject

interface SearchResultGenerator {
  suspend fun generateSearchResults(
    searchTerm: String,
    zimReader: ZimReader?
  ): List<SearchListItem>
}

class ZimSearchResultGenerator @Inject constructor() : SearchResultGenerator {

  override suspend fun generateSearchResults(searchTerm: String, zimReader: ZimReader?) =
    withContext(Dispatchers.IO) {
      if (searchTerm.isNotEmpty()) readResultsFromZim(searchTerm, zimReader)
      else emptyList()
    }

  private suspend fun readResultsFromZim(
    searchTerm: String,
    reader: ZimReader?
  ) =
    reader.also { yield() }
      ?.searchSuggestions(searchTerm, 200)
      .also { yield() }
      .run { suggestionResults(reader) }

  private suspend fun suggestionResults(reader: ZimReader?) = createList {
    yield()
    reader?.getNextSuggestion()
      ?.let { ZimSearchResultListItem(it.title) }
  }
    .distinct()
    .toList()

  private suspend fun <T> createList(readSearchResult: suspend () -> T?): List<T> {
    return mutableListOf<T>().apply {
      while (true) readSearchResult()?.let(::add) ?: break
    }
  }
}
