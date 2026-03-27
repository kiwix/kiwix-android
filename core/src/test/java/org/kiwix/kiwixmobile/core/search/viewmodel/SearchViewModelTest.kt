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

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.RecentSearchRoomDao
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.SearchListItem.RecentSearchListItem
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ActivityResultReceived
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ClickedSearchInText
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ConfirmedDelete
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.CreatedWithArguments
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ExitedSearch
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.OnOpenInNewTabClick
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ReceivedPromptForSpeechInput
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ScreenWasStartedFrom
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.StartSpeechInputFailed
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchOrigin.FromWebView
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.DeleteRecentSearch
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.OpenSearchItem
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.PopFragmentBackstack
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.ProcessActivityResult
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SaveSearchToRecents
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SearchArgumentProcessing
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SearchInPreviousScreen
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.ShowDeleteSearchDialog
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.ShowToast
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.StartSpeechInput
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.libzim.SuggestionSearch

@OptIn(ExperimentalCoroutinesApi::class)
internal class SearchViewModelTest {
  private val recentSearchRoomDao: RecentSearchRoomDao = mockk()
  private val zimReaderContainer: ZimReaderContainer = mockk()
  private val searchResultGenerator: SearchResultGenerator = mockk()
  private val zimFileReader: ZimFileReader = mockk()
  private val dialogShower = mockk<AlertDialogShower>(relaxed = true)
  private val testDispatcher = StandardTestDispatcher()
  private val searchMutex: Mutex = mockk()

  lateinit var viewModel: SearchViewModel

  @AfterAll
  fun teardown() {
    Dispatchers.resetMain()
  }

  private lateinit var recentsFromDb: Channel<List<RecentSearchListItem>>

  @BeforeEach
  fun init() {
    Dispatchers.resetMain()
    Dispatchers.setMain(testDispatcher)
    clearAllMocks()
    recentsFromDb = Channel(Channel.UNLIMITED)
    every { zimReaderContainer.zimFileReader } returns zimFileReader
    every {
      zimFileReader.getSuggestedSpelledWords(any(), any())
    } returns emptyList()
    coEvery {
      searchResultGenerator.generateSearchResults(any(), zimFileReader)
    } returns null
    every { zimReaderContainer.id } returns "id"
    every { recentSearchRoomDao.recentSearches("id") } returns recentsFromDb.consumeAsFlow()
    viewModel =
      SearchViewModel(
        recentSearchRoomDao,
        zimReaderContainer,
        searchResultGenerator,
        searchMutex,
        testDispatcher
      ).apply {
        setAlertDialogShower(dialogShower)
      }
  }

  @Nested
  inner class DebouncedTest {
    @Test
    fun searchState_whenDebounced_returnsLatestQuery() = runTest {
      val searchTerm1 = "query1"
      val searchTerm2 = "query2"
      val searchTerm3 = "query3"
      val suggestionSearch: SuggestionSearch = mockk()

      viewModel.uiState.test {
        skipItems(1)
        searchResult(searchTerm1, suggestionSearch, testScheduler, DEBOUNCE_DELAY / 3)
        searchResult(searchTerm2, suggestionSearch, testScheduler, DEBOUNCE_DELAY / 3)
        searchResult(searchTerm3, suggestionSearch, testScheduler, DEBOUNCE_DELAY)

        advanceUntilIdle()

        val result = expectMostRecentItem()
        assertThat(result.searchState.searchTerm).isEqualTo(searchTerm3)
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun searchState_whenUserIsTypingAndDebouncedIsNotComplete_returnsInitialState() = runTest {
      val searchTerm1 = "query1"
      val searchTerm2 = "query2"
      val searchTerm3 = "query3"
      val suggestionSearch: SuggestionSearch = mockk()

      viewModel.uiState.test {
        skipItems(1)
        searchResult(searchTerm1, suggestionSearch, testScheduler, DEBOUNCE_DELAY / 3)
        searchResult(searchTerm2, suggestionSearch, testScheduler, DEBOUNCE_DELAY / 3)
        searchResult(searchTerm3, suggestionSearch, testScheduler, DEBOUNCE_DELAY / 3)

        val latest = expectMostRecentItem()
        assertThat(latest.searchText).isEqualTo(searchTerm3)
        assertThat(latest.searchState.searchTerm).isEqualTo("")

        cancelAndIgnoreRemainingEvents()
      }
    }

    private fun searchResult(
      searchTerm: String,
      suggestionSearch: SuggestionSearch,
      testScheduler: TestCoroutineScheduler,
      timeout: Long
    ) {
      coEvery {
        searchResultGenerator.generateSearchResults(searchTerm, zimFileReader)
      } returns suggestionSearch
      viewModel.onSearchValueChanged(searchTerm)
      recentsFromDb.trySend(emptyList()).isSuccess
      viewModel.actions.tryEmit(ScreenWasStartedFrom(FromWebView))
      testScheduler.apply {
        advanceTimeBy(timeout)
        runCurrent()
      }
    }
  }

  @Nested
  inner class StateTests {
    @Test
    fun uiState_whenInitialized_returnsDefaultState() =
      runTest {
        viewModel.uiState.test {
          val initial = awaitItem()

          assertThat(initial.searchState.searchTerm).isEqualTo("")
          assertThat(initial.searchState.recentResults).isEmpty()
          assertThat(initial.searchState.searchOrigin).isEqualTo(FromWebView)
          assertThat(initial.searchText).isEqualTo("")
          assertThat(initial.isLoading).isFalse()
        }
      }

    @Test
    fun reducer_whenSearched_returnsCombinedResult() =
      runTest {
        val searchTerm = "searchTerm"
        val searchOrigin = FromWebView
        val suggestionSearch: SuggestionSearch = mockk()
        viewModel.uiState.test {
          skipItems(1)

          emissionOf(
            searchTerm = searchTerm,
            suggestionSearch = suggestionSearch,
            databaseResults = listOf(RecentSearchListItem("", "")),
            searchOrigin = searchOrigin
          )
          advanceUntilIdle()

          skipItems(1)

          val item = awaitItem()
          assertThat(item.searchState.searchTerm).isEqualTo(searchTerm)
          assertThat(item.searchState.recentResults).isEqualTo(listOf(RecentSearchListItem("", "")))
          assertThat(item.searchState.searchOrigin).isEqualTo(searchOrigin)

          cancelAndIgnoreRemainingEvents()
        }
      }

    @Test
    fun onSearchValueChanged_whenNonBlank_returnsFindInPageMenuItemIsEnabled() = runTest {
      viewModel.onSearchValueChanged("kiwix")
      advanceUntilIdle()
      assertThat(viewModel.uiState.value.findInPageMenuItem.first).isTrue()
    }

    @Test
    fun onSearchClear_whenCalled_returnsFindInPageMenuItemIsDisabled() = runTest {
      viewModel.onSearchValueChanged("hello")
      advanceUntilIdle()
      viewModel.onSearchClear()
      advanceUntilIdle()
      assertThat(viewModel.uiState.value.findInPageMenuItem.first).isFalse()
    }

    @Test
    fun onSearchValueChanged_whenCalled_returnsUpdatedSearchText() = runTest {
      val query = "kiwix"
      viewModel.onSearchValueChanged(query)
      advanceUntilIdle()
      assertThat(viewModel.uiState.value.searchText).isEqualTo(query)
    }

    @Test
    fun onSearchClear_whenCalled_returnsEmptyText() = runTest {
      viewModel.onSearchValueChanged("hello")
      advanceUntilIdle()
      viewModel.onSearchClear()
      advanceUntilIdle()
      assertThat(viewModel.uiState.value.searchText).isEqualTo("")
    }

    @Test
    fun onSuggestionItemClick_whenCalled_returnsEmptySpellingCorrectionSuggestions() = runTest {
      every { zimFileReader.getSuggestedSpelledWords(any(), any()) } returns listOf("suggested")
      viewModel.onSearchValueChanged("suggeste")
      advanceUntilIdle()
      viewModel.onSuggestionItemClick("suggested")
      advanceUntilIdle()
      assertThat(viewModel.uiState.value.spellingCorrectionSuggestions).isEmpty()
    }

    @Test
    fun onSuggestionItemClick_whenCalled_returnsSpellingCorrectionSuggestions() = runTest {
      val suggestion = "corrected"
      viewModel.onSuggestionItemClick(suggestion)
      advanceUntilIdle()
      assertThat(viewModel.uiState.value.searchText).isEqualTo(suggestion)
    }
  }

  @Nested
  inner class ActionMapping {
    @Test
    fun `ExitedSearch offers PopFragmentBackstack`() =
      runTest {
        actionResultsInEffects(ExitedSearch, PopFragmentBackstack)
      }

    @Test
    fun `OnItemClick offers Saves and Opens`() =
      runTest {
        val searchListItem = RecentSearchListItem("", "")
        actionResultsInEffects(
          OnItemClick(searchListItem),
          SaveSearchToRecents(
            recentSearchRoomDao,
            searchListItem,
            "id",
            viewModel.viewModelScope
          ),
          OpenSearchItem(searchListItem, false)
        )
      }

    @Test
    fun `OnOpenInNewTabClick offers Saves and Opens in new tab`() =
      runTest {
        val searchListItem = RecentSearchListItem("", "")
        actionResultsInEffects(
          OnOpenInNewTabClick(searchListItem),
          SaveSearchToRecents(
            recentSearchRoomDao,
            searchListItem,
            "id",
            viewModel.viewModelScope
          ),
          OpenSearchItem(searchListItem, true)
        )
      }

    @Test
    fun `OnItemLongClick offers Saves and Opens`() =
      runTest {
        val searchListItem = RecentSearchListItem("", "")
        actionResultsInEffects(
          OnItemLongClick(searchListItem),
          ShowDeleteSearchDialog(searchListItem, viewModel.actions, dialogShower)
        )
      }

    @Test
    fun `ClickedSearchInText offers SearchInPreviousScreen`() =
      runTest {
        actionResultsInEffects(ClickedSearchInText, SearchInPreviousScreen(""))
      }

    @Test
    fun `ConfirmedDelete offers Delete and Toast`() =
      runTest {
        val searchListItem = RecentSearchListItem("", "")
        actionResultsInEffects(
          ConfirmedDelete(searchListItem),
          DeleteRecentSearch(searchListItem, recentSearchRoomDao, viewModel.viewModelScope),
          ShowToast(R.string.delete_specific_search_toast)
        )
      }

    @Test
    fun `CreatedWithArguments offers SearchArgumentProcessing`() =
      runTest {
        val bundle = mockk<Bundle>()
        actionResultsInEffects(
          CreatedWithArguments(bundle),
          SearchArgumentProcessing(bundle, viewModel.actions)
        )
      }

    @Test
    fun `ReceivedPromptForSpeechInput offers StartSpeechInput`() =
      runTest {
        actionResultsInEffects(
          ReceivedPromptForSpeechInput,
          StartSpeechInput(viewModel.actions)
        )
      }

    @Test
    fun `StartSpeechInputFailed offers ShowToast`() =
      runTest {
        actionResultsInEffects(
          StartSpeechInputFailed,
          ShowToast(string.speech_not_supported)
        )
      }

    @Test
    fun voiceSearchResult_whenEmitted_returnsUpdatedSearchText() = runTest {
      val voiceTerm = "kiwix"
      viewModel.actions.tryEmit(Action.VoiceSearchResult(voiceTerm))
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.searchText).isEqualTo(voiceTerm)
    }

    @Test
    fun onKeyboardSubmitButtonClick_whenNoMatchFound_returnsNothing() = runTest {
      recentsFromDb.trySend(emptyList())
      advanceUntilIdle()

      viewModel.effects.test {
        viewModel.onKeyboardSubmitButtonClick("no match here")

        expectNoEvents()

        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun loadMoreSearchResults_whenCalled_returnsLoadMoreResults() = runTest {
      viewModel.actions.test {
        viewModel.loadMoreSearchResults()

        val action = awaitItem()

        assertThat(action).isInstanceOf(Action.LoadMoreResults::class.java)

        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `ActivityResultReceived offers ProcessActivityResult`() =
      runTest {
        actionResultsInEffects(
          ActivityResultReceived(0, 1, null),
          ProcessActivityResult(0, 1, null, viewModel.actions)
        )
      }
  }

  private suspend fun TestScope.actionResultsInEffects(
    action: Action,
    vararg effects: SideEffect<*>
  ) {
    viewModel.effects.test {
      viewModel.actions.tryEmit(action)
      advanceUntilIdle()
      effects.forEach { expected ->
        assertThat(awaitItem()).isEqualTo(expected)
      }
      expectNoEvents()
    }
  }

  private fun emissionOf(
    searchTerm: String,
    suggestionSearch: SuggestionSearch,
    databaseResults: List<RecentSearchListItem>,
    searchOrigin: SearchOrigin
  ) {
    coEvery {
      searchResultGenerator.generateSearchResults(searchTerm, zimFileReader)
    } returns suggestionSearch
    viewModel.actions.tryEmit(Filter(searchTerm))
    recentsFromDb.trySend(databaseResults).isSuccess
    viewModel.actions.tryEmit(ScreenWasStartedFrom(searchOrigin))
  }
}
