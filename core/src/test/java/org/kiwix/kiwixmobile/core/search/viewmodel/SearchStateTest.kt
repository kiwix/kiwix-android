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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.RecentSearchListItem
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.ZimSearchResultListItem
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchOrigin.FromWebView

internal class SearchStateTest {

  @Test
  internal fun `visibleResults use searchResults when searchTerm is not empty`() {
    val results = listOf(ZimSearchResultListItem(""))
    assertThat(
      SearchState(
        "notEmpty",
        SearchResultsWithTerm("", results),
        emptyList(),
        FromWebView
      ).visibleResults
    ).isEqualTo(results)
  }

  @Test
  internal fun `visibleResults use recentResults when searchTerm is empty`() {
    val results = listOf(RecentSearchListItem(""))
    assertThat(
      SearchState(
        "",
        SearchResultsWithTerm("", emptyList()),
        results,
        FromWebView
      ).visibleResults
    ).isEqualTo(results)
  }

  @Test
  internal fun `isLoading when searchTerm is not equal to ResultTerm`() {
    assertThat(
      SearchState(
        "",
        SearchResultsWithTerm("notEqual", emptyList()),
        emptyList(),
        FromWebView
      ).isLoading
    ).isTrue()
  }

  @Test
  internal fun `is not Loading when searchTerm is equal to ResultTerm`() {
    val searchTerm = "equal"
    assertThat(
      SearchState(
        searchTerm,
        SearchResultsWithTerm(searchTerm, emptyList()),
        emptyList(),
        FromWebView
      ).isLoading
    ).isFalse()
  }
}
