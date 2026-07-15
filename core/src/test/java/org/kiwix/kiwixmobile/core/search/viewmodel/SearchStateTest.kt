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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.search.SearchListItem
import org.kiwix.kiwixmobile.core.search.SearchListItem.RecentSearchListItem
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchOrigin.FromWebView
import org.kiwix.sharedFunctions.MainDispatcherRule

internal class SearchStateTest {
  @RegisterExtension
  @JvmField
  val mainDispatcherRule = MainDispatcherRule()

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  internal fun `visibleResults use searchResults when searchTerm is not empty`() =
    runTest {
      val searchTerm = "notEmpty"
      val pageUrl = ""
      val suggestionSearchWrapper: SuggestionSearchWrapper = mockk()
      val searchIteratorWrapper: SuggestionIteratorWrapper = mockk()
      val entryWrapper: SuggestionItemWrapper = mockk()
      val estimatedMatches = 20
      every { suggestionSearchWrapper.estimatedMatches } returns estimatedMatches.toLong()
      // Settings list to hasNext() to ensure it returns true only for the first call.
      // Otherwise, if we do not set this, the method will always return true when
      // checking if the iterator has a next value, causing our test case to get stuck
      // in an infinite loop due to this explicit setting.
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
          SearchResultsWithTerm("", ZimSearchResultSet.Title(suggestionSearchWrapper), mockk()),
          emptyList(),
          FromWebView
        ).getVisibleResults(0, ioDispatcher = mainDispatcherRule.dispatcher)
      ).isEqualTo(listOf(SearchListItem.ZimSearchResultListItem(searchTerm, "")))
    }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  internal fun `visibleResults use full text search results when in page content mode`() =
    runTest {
      val searchTerm = "notEmpty"
      val pageUrl = "A/page"
      val snippet = "a sentence with the <b>notEmpty</b> match"
      val searchWrapper: SearchWrapper = mockk()
      val searchIteratorWrapper: SearchIteratorWrapper = mockk()
      val entry: EntryWrapper = mockk()
      every { searchIteratorWrapper.hasNext() } returnsMany listOf(true, false)
      every { searchIteratorWrapper.next() } returns entry
      every { searchIteratorWrapper.snippet } returns snippet
      every { entry.title } returns searchTerm
      every { entry.path } returns pageUrl
      every { searchWrapper.getResults(0, 20) } returns searchIteratorWrapper
      assertThat(
        SearchState(
          searchTerm,
          SearchResultsWithTerm("", ZimSearchResultSet.PageContent(searchWrapper), mockk()),
          emptyList(),
          FromWebView
        ).getVisibleResults(0, ioDispatcher = mainDispatcherRule.dispatcher)
      ).isEqualTo(listOf(SearchListItem.ZimSearchResultListItem(searchTerm, pageUrl, snippet)))
    }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  internal fun `each page content result keeps its own snippet, not the next one's`() =
    runTest {
      // Regression test: the snippet must be read from the iterator's current
      // position *before* next() advances it. libzim's getSnippet() reflects the
      // iterator's current position, and next() returns the current entry then
      // advances. If the snippet is read after next(), every result shows the
      // *next* result's snippet. This stateful mock reproduces those semantics.
      val titles = listOf("first", "second")
      val paths = listOf("A/first", "A/second")
      val snippets = listOf("first snippet", "second snippet")
      var position = 0
      val searchWrapper: SearchWrapper = mockk()
      val searchIteratorWrapper: SearchIteratorWrapper = mockk()
      every { searchIteratorWrapper.hasNext() } answers { position < titles.size }
      // getSnippet() reads the *current* position.
      every { searchIteratorWrapper.snippet } answers { snippets[position] }
      // next() returns the current entry, then advances the cursor.
      every { searchIteratorWrapper.next() } answers {
        val entry: EntryWrapper = mockk()
        every { entry.title } returns titles[position]
        every { entry.path } returns paths[position]
        position++
        entry
      }
      every { searchWrapper.getResults(0, 20) } returns searchIteratorWrapper
      assertThat(
        SearchState(
          "term",
          SearchResultsWithTerm("", ZimSearchResultSet.PageContent(searchWrapper), mockk()),
          emptyList(),
          FromWebView
        ).getVisibleResults(0, ioDispatcher = mainDispatcherRule.dispatcher)
      ).isEqualTo(
        listOf(
          SearchListItem.ZimSearchResultListItem(titles[0], paths[0], snippets[0]),
          SearchListItem.ZimSearchResultListItem(titles[1], paths[1], snippets[1])
        )
      )
    }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  internal fun `visibleResults use recentResults when searchTerm is empty`() =
    runTest {
      val results = listOf(RecentSearchListItem("", ""))
      assertThat(
        SearchState(
          "",
          SearchResultsWithTerm("", null, mockk()),
          results,
          FromWebView
        ).getVisibleResults(0, ioDispatcher = mainDispatcherRule.dispatcher)
      ).isEqualTo(results)
    }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  internal fun `visibleResults use recentResults when zimSearchResultSet is null`() =
    runTest {
      val results = listOf(RecentSearchListItem("", ""))
      assertThat(
        SearchState(
          "notEmpty",
          SearchResultsWithTerm("notEmpty", null, mockk()),
          results,
          FromWebView
        ).getVisibleResults(0, ioDispatcher = mainDispatcherRule.dispatcher)
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

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `test search cancellation`() =
    runTest {
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

      val searchResultsWithTerm =
        SearchResultsWithTerm(
          searchTerm,
          ZimSearchResultSet.Title(suggestionSearchWrapper),
          mockk()
        )
      val searchState = SearchState(searchTerm, searchResultsWithTerm, emptyList(), FromWebView)
      var list: List<SearchListItem>? = emptyList()
      var list1: List<SearchListItem>? = emptyList()
      val job =
        launch(mainDispatcherRule.dispatcher) {
          list =
            searchState.getVisibleResults(0, ioDispatcher = mainDispatcherRule.dispatcher)
        }

      job.cancelAndJoin()
      // test the coroutine job is cancelled properly
      assertThat(job.isCancelled).isTrue
      assertThat(list?.size).isEqualTo(0)

      val job1 =
        launch(mainDispatcherRule.dispatcher) {
          list1 =
            searchState.getVisibleResults(0, ioDispatcher = mainDispatcherRule.dispatcher)
        }
      advanceUntilIdle()
      job1.invokeOnCompletion {
        // test the second job is successfully return the data
        assertThat(job1.isCompleted).isTrue
        assertThat(list1?.size).isEqualTo(1)
        assertThat(list1?.get(0)?.url).isEqualTo("path")
        assertThat(list1?.get(0)?.value).isEqualTo("Result")
      }
    }
}
