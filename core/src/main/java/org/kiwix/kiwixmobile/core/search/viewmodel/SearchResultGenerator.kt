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

import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.ZimSearchResultListItem
import javax.inject.Inject

interface SearchResultGenerator {
  fun generateSearchResults(searchTerm: String): List<SearchListItem>
}

class ZimSearchResultGenerator @Inject constructor(
  private val zimReaderContainer: ZimReaderContainer
) : SearchResultGenerator {
  override fun generateSearchResults(searchTerm: String) =
    if (searchTerm.isNotEmpty()) readResultsFromZim(searchTerm, zimReaderContainer.copyReader())
    else emptyList()

  private fun readResultsFromZim(
    it: String,
    reader: ZimFileReader?
  ) =
    reader?.searchSuggestions(it, 200).run { suggestionResults(reader) }

  private fun suggestionResults(reader: ZimFileReader?) = generateSequence {
    reader?.getNextSuggestion()?.let { ZimSearchResultListItem(it.title) }
  }
    .distinct()
    .toList()
    .also { reader?.dispose() }
}
