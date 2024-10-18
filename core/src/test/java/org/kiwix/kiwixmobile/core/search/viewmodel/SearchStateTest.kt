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

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.RecentSearchListItem
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchOrigin.FromWebView

internal class SearchStateTest {

  @Test
  internal fun `visibleResults use searchResults when searchTerm is not empty`() = runTest {
    val searchTerm = "notEmpty"
    val pageUrl = ""
    val suggestionSearchWrapper: SuggestionSearchWrapper = mockk()
    val searchIteratorWrapper: SuggestionIteratorWrapper = mockk()
    val entryWrapper: SuggestionItemWrapper = mockk()
    val estimatedMatches = 100
    every { suggestionSearchWrapper.estimatedMatches } returns estimatedMatches.toLong()
    // Settings list to hasNext() to ensure it returns true only for the first call.
    // Otherwise, if we do not set this, the method will always return true when checking if the iterator has a next value,
    // causing our test case to get stuck in an infinite loop due to this explicit setting.
    every { searchIteratorWrapper.hasNext() } returnsMany listOf(true, false)
    every { searchIteratorWrapper.next() } returns entryWrapper
    every { entryWrapper.title } returns searchTerm
    every { entryWrapper.path } returns pageUrl
    every {
      suggestionSearchWrapper.getResults(
        0,
        estimatedMatches
      )
    } returns searchIteratorWrapper
    assertThat(
      SearchState(
        searchTerm,
        SearchResultsWithTerm("", suggestionSearchWrapper, mockk()),
        emptyList(),
        FromWebView
      ).getVisibleResults(0)
    ).isEqualTo(listOf(RecentSearchListItem(searchTerm, "")))
  }

  @Test
  internal fun `visibleResults use recentResults when searchTerm is empty`() = runTest {
    val results = listOf(RecentSearchListItem("", ""))
    assertThat(
      SearchState(
        "",
        SearchResultsWithTerm("", null, mockk()),
        results,
        FromWebView
      ).getVisibleResults(0)
    ).isEqualTo(results)
  }

  @Test
  internal fun `isLoading when searchTerm is not equal to ResultTerm`() {
    assertThat(
      SearchState(
        "",
        SearchResultsWithTerm("notEqual", null, mockk()),
        emptyList(),
        FromWebView
      ).isLoading
    ).isTrue
  }

  @Test
  internal fun `is not Loading when searchTerm is equal to ResultTerm`() {
    val searchTerm = "equal"
    assertThat(
      SearchState(
        searchTerm,
        SearchResultsWithTerm(searchTerm, null, mockk()),
        emptyList(),
        FromWebView
      ).isLoading
    ).isFalse
  }

  @Test
  fun `test search cancellation`() = runTest {
    val searchTerm = "cancelTest"
    val suggestionSearchWrapper: SuggestionSearchWrapper = mockk()
    val searchIteratorWrapper: SuggestionIteratorWrapper = mockk()
    val entryWrapper: SuggestionItemWrapper = mockk()

    every { suggestionSearchWrapper.estimatedMatches } returns 100
    every { searchIteratorWrapper.hasNext() } returnsMany listOf(true, false)
    every { searchIteratorWrapper.next() } returns entryWrapper
    every { entryWrapper.title } returns "Result"
    every { entryWrapper.path } returns "path"
    every { suggestionSearchWrapper.getResults(any(), any()) } returns searchIteratorWrapper

    val searchResultsWithTerm = SearchResultsWithTerm(searchTerm, suggestionSearchWrapper, mockk())
    val searchState = SearchState(searchTerm, searchResultsWithTerm, emptyList(), FromWebView)
    var list: List<SearchListItem.RecentSearchListItem>? = emptyList()
    var list1: List<SearchListItem.RecentSearchListItem>? = emptyList()
    val job = launch(Dispatchers.IO) {
      delay(1000)
      list = searchState.getVisibleResults(0)
    }

    job.cancelAndJoin()
    // test the coroutine job is cancelled properly
    assertThat(job.isCancelled).isTrue
    assertThat(list?.size).isEqualTo(0)

    val job1 = launch(Dispatchers.IO) {
      list1 = searchState.getVisibleResults(0)
    }
    job1.invokeOnCompletion {
      // test the second job is successfully return the data
      assertThat(job1.isCompleted).isTrue
      assertThat(list1?.size).isEqualTo(1)
      assertThat(list1?.get(0)?.url).isEqualTo("path")
      assertThat(list1?.get(0)?.value).isEqualTo("Result")
    }
  }
}
