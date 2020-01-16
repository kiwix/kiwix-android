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

import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.ZimSearchResultListItem
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

interface SearchResultGenerator {
  fun generateSearchResults(searchTerm: String): List<SearchListItem>
}

class ZimSearchResultGenerator @Inject constructor(
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val zimReaderContainer: ZimReaderContainer
) : SearchResultGenerator {
  override fun generateSearchResults(searchTerm: String) =
    if (searchTerm.isNotEmpty()) readResultsFromZim(searchTerm)
    else emptyList()

  private fun readResultsFromZim(it: String) =
    if (sharedPreferenceUtil.prefFullTextSearch)
      zimReaderContainer.search(it, 200).run { fullTextResults() }
    else
      zimReaderContainer.searchSuggestions(it, 200).run { suggestionResults() }

  private fun fullTextResults() = generateSequence {
    zimReaderContainer.getNextResult()?.title?.let(::ZimSearchResultListItem)
  }.filter { it.value.isNotBlank() }
    .toList()

  private fun suggestionResults() = generateSequence {
    zimReaderContainer.getNextSuggestion()?.let { ZimSearchResultListItem(it.title) }
  }.distinct()
    .toList()
}
