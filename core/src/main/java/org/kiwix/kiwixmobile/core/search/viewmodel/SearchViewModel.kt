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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.RecentSearchRoomDao
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.SearchListItem
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
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.VoiceSearchResult
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
import javax.inject.Inject

const val DEBOUNCE_DELAY = 150L
const val MAX_SUGGEST_WORD_COUNT = 1

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel @Inject constructor(
  private val recentSearchRoomDao: RecentSearchRoomDao,
  private val zimReaderContainer: ZimReaderContainer,
  private val searchResultGenerator: SearchResultGenerator,
  private val searchMutex: Mutex = Mutex()
) : ViewModel() {
  private val initialState = SearchState(
    "",
    SearchResultsWithTerm("", null, searchMutex),
    emptyList(),
    FromWebView
  )

  val state = MutableStateFlow(initialState)

  private var lastQuery: String = ""

  private val _searchText = MutableStateFlow("")
  val searchText = _searchText.asStateFlow()

  private val _spellingSuggestions = MutableStateFlow<List<String>>(emptyList())
  val spellingSuggestions = _spellingSuggestions.asStateFlow()

  private val _isLoadingMore = MutableStateFlow(false)
  val isLoadingMore = _isLoadingMore.asStateFlow()

  private val _visibleResults = MutableStateFlow<List<SearchListItem>>(emptyList())
  val visibleResults = _visibleResults.asStateFlow()

  private val _showFindInPage = MutableStateFlow(false)
  val showFindInPage = _showFindInPage.asStateFlow()

  private val _effects = Channel<SideEffect<*>>(Channel.UNLIMITED)
  val effects = _effects.receiveAsFlow()

  val actions = Channel<Action>(Channel.UNLIMITED)
  private val filter = MutableStateFlow("")
  private val searchOrigin = MutableStateFlow(FromWebView)
  private lateinit var alertDialogShower: AlertDialogShower
  private val debouncedSearchQuery = MutableStateFlow("")

  init {
    viewModelScope.launch { reducer() }
    viewModelScope.launch { actionMapper() }
    viewModelScope.launch { debouncedSearchQuery() }

    viewModelScope.launch {
      combine(searchOrigin, _searchText) { origin, text ->
        origin == FromWebView && text.isNotBlank()
      }.collect {
        _showFindInPage.value = it
      }
    }
  }

  private fun getSuggestedSpelledWords(word: String, maxCount: Int): List<String> =
    zimReaderContainer.zimFileReader?.getSuggestedSpelledWords(word, maxCount).orEmpty()

  fun setAlertDialogShower(alertDialogShower: AlertDialogShower) {
    this.alertDialogShower = alertDialogShower
  }

  @OptIn(FlowPreview::class)
  private suspend fun debouncedSearchQuery() {
    // Observe and collect the debounced search query
    debouncedSearchQuery
      // Applying debouncing to delay the emission of consecutive search queries
      .debounce(DEBOUNCE_DELAY)
      // Ensuring that only distinct search queries are processed
      .distinctUntilChanged()
      .collect { query ->
        actions.trySend(Filter(query))
      }
  }

  private suspend fun reducer() {
    combine(
      searchResults(),
      recentSearchRoomDao.recentSearches(zimReaderContainer.id),
      searchOrigin.asStateFlow()
    ) { searchResultsWithTerm, recentResults, searchOrigin ->
      SearchState(
        searchResultsWithTerm.searchTerm,
        searchResultsWithTerm,
        recentResults as List<SearchListItem.RecentSearchListItem>,
        searchOrigin
      )
    }
      .distinctUntilChanged()
      .collect {
        state.value = it

        if (it.searchTerm != lastQuery && !_isLoadingMore.value) {
          lastQuery = it.searchTerm
          val firstPage = it.getVisibleResults(0).orEmpty()
          _visibleResults.value = firstPage
        }

        if (it.searchTerm.isNotBlank()) {
          _spellingSuggestions.value =
            getSuggestedSpelledWords(it.searchTerm, MAX_SUGGEST_WORD_COUNT)
        } else {
          _spellingSuggestions.value = emptyList()
        }
      }
  }

  private fun searchResults() =
    filter.asStateFlow()
      .mapLatest {
        SearchResultsWithTerm(
          it,
          searchResultGenerator.generateSearchResults(it, zimReaderContainer.zimFileReader),
          searchMutex
        )
      }

  private suspend fun actionMapper() =
    actions.consumeEach {
      when (it) {
        ExitedSearch -> _effects.trySend(PopFragmentBackstack)
        is OnItemClick -> saveSearchAndOpenItem(it.searchListItem, false)
        is OnOpenInNewTabClick -> saveSearchAndOpenItem(it.searchListItem, true)
        is OnItemLongClick -> showDeleteDialog(it)
        is Filter -> filter.tryEmit(it.term)
        ClickedSearchInText -> searchPreviousScreenWhenStateIsValid()
        is ConfirmedDelete -> deleteItemAndShowToast(it)
        is CreatedWithArguments ->
          _effects.trySend(SearchArgumentProcessing(it.arguments, actions))

        ReceivedPromptForSpeechInput ->
          _effects.trySend(StartSpeechInput(actions))

        StartSpeechInputFailed ->
          _effects.trySend(ShowToast(R.string.speech_not_supported))

        is ActivityResultReceived ->
          _effects.trySend(
            ProcessActivityResult(
              it.requestCode,
              it.resultCode,
              it.data,
              actions
            )
          )

        is ScreenWasStartedFrom -> {
          searchOrigin.tryEmit(it.searchOrigin)
        }

        is VoiceSearchResult -> {
          updateSearchQuery(it.term)
        }
      }
    }

  private fun deleteItemAndShowToast(it: ConfirmedDelete) {
    _effects.trySend(
      DeleteRecentSearch(it.searchListItem, recentSearchRoomDao, viewModelScope)
    )
    _effects.trySend(ShowToast(R.string.delete_specific_search_toast))
  }

  private fun searchPreviousScreenWhenStateIsValid(): Any =
    _effects.trySend(SearchInPreviousScreen(state.value.searchTerm))

  private fun showDeleteDialog(longClick: OnItemLongClick) {
    _effects.trySend(
      ShowDeleteSearchDialog(
        longClick.searchListItem,
        actions,
        alertDialogShower
      )
    )
  }

  private fun saveSearchAndOpenItem(searchListItem: SearchListItem, openInNewTab: Boolean) {
    _effects.trySend(
      SaveSearchToRecents(
        recentSearchRoomDao,
        searchListItem,
        zimReaderContainer.id,
        viewModelScope
      )
    )
    _effects.trySendBlocking(OpenSearchItem(searchListItem, openInNewTab))
  }

  fun updateSearchQuery(query: String) {
    _searchText.value = query
    filter.value = query
    debouncedSearchQuery.value = query
  }

  suspend fun loadMoreSearchResults(startIndex: Int) {
    if (_isLoadingMore.value) return

    _isLoadingMore.value = true

    val more = state.value.getVisibleResults(startIndex) ?: run {
      _isLoadingMore.value = false
      return@loadMoreSearchResults
    }

    val current = _visibleResults.value
    val newItems = more.filter { newItem ->
      current.none { it == newItem }
    }

    _visibleResults.update { old ->
      if (newItems.isEmpty()) old else old + newItems
    }
    _isLoadingMore.value = false
  }
}

data class SearchResultsWithTerm(
  val searchTerm: String,
  val suggestionSearch: SuggestionSearch?,
  val searchMutex: Mutex
)
