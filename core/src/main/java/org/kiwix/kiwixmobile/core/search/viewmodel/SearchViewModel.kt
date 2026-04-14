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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.RecentSearchRoomDao
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.SearchListItem
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ActivityResultReceived
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ClickedSearchInText
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ConfirmedDelete
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.CreatedWithArguments
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ExitedSearch
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.LoadMoreResults
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
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.effects.CloseKeyboard
import org.kiwix.libzim.SuggestionSearch
import javax.inject.Inject

const val DEBOUNCE_DELAY = 150L
const val MAX_SUGGEST_WORD_COUNT = 1

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel @Inject constructor(
  private val recentSearchRoomDao: RecentSearchRoomDao,
  private val zimReaderContainer: ZimReaderContainer,
  private val searchResultGenerator: SearchResultGenerator,
  private val searchMutex: Mutex = Mutex(),
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
  private val _uiState = MutableStateFlow(SearchScreenUiState())
  val uiState = _uiState.asStateFlow()

  private val _effects = MutableSharedFlow<SideEffect<*>>(extraBufferCapacity = Int.MAX_VALUE)
  val effects = _effects.asSharedFlow()

  val actions = MutableSharedFlow<Action>(extraBufferCapacity = Int.MAX_VALUE)
  private val filter = MutableStateFlow("")
  private val searchOrigin = MutableStateFlow(FromWebView)
  private lateinit var alertDialogShower: AlertDialogShower
  private val debouncedSearchQuery = MutableStateFlow("")

  init {
    viewModelScope.launch { reducer() }
    viewModelScope.launch { actionMapper() }
    viewModelScope.launch { debouncedSearchQuery() }
  }

  private suspend fun getSuggestedSpelledWords(word: String, maxCount: Int): List<String> =
    withContext(ioDispatcher) {
      zimReaderContainer.zimFileReader?.getSuggestedSpelledWords(word, maxCount).orEmpty()
    }

  fun setAlertDialogShower(alertDialogShower: AlertDialogShower) {
    this.alertDialogShower = alertDialogShower
  }

  private fun requireAlertDialogShower(): AlertDialogShower {
    if (!::alertDialogShower.isInitialized) {
      throw IllegalStateException(
        "AlertDialogShower is not initialized. " +
          "Call setAlertDialogShower(AlertDialogShower) " +
          "before deleting the SearchedItems."
      )
    }
    return alertDialogShower
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
        actions.tryEmit(Filter(query))
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
        recentResults,
        searchOrigin
      )
    }.mapLatest { searchState ->
      // When getting the search results show the loading progressBar.
      updateUiState { it.copy(isLoading = true, isLoadingMore = false) }

      val firstPage = withContext(ioDispatcher) {
        searchState.getVisibleResults(ZERO, ioDispatcher = ioDispatcher).orEmpty()
      }

      val suggestions =
        getSuggestedWordsList(searchList = firstPage, searchText = searchState.searchTerm)

      updateUiState {
        it.copy(
          searchState = searchState,
          searchList = firstPage,
          spellingCorrectionSuggestions = suggestions,
          isLoading = false,
          findInPageMenuItem =
            searchState.searchTerm.isNotBlank() to (searchOrigin.value == FromWebView)
        )
      }
    }.collect {
      // Do nothing as state is already updated.
    }
  }

  /**
   * Return the suggested word list using the libkiwix spellings database.
   */
  private suspend fun getSuggestedWordsList(
    searchList: List<SearchListItem>,
    searchText: String
  ): List<String> {
    val suggestedWordsList = arrayListOf<String>()

    val onlyRecentSearches =
      searchList.all { it is SearchListItem.RecentSearchListItem }

    if (onlyRecentSearches && searchText.isNotEmpty()) {
      suggestedWordsList.addAll(
        getSuggestedSpelledWords(searchText, MAX_SUGGEST_WORD_COUNT)
      )
    }
    return suggestedWordsList
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

  @Suppress("CyclomaticComplexMethod")
  private suspend fun actionMapper() {
    actions.collect {
      when (it) {
        ExitedSearch -> _effects.tryEmit(PopFragmentBackstack)
        is OnItemClick -> saveSearchAndOpenItem(it.searchListItem, false)
        is OnOpenInNewTabClick -> saveSearchAndOpenItem(it.searchListItem, true)
        is OnItemLongClick -> showDeleteDialog(it)
        is Filter -> filter.tryEmit(it.term)
        ClickedSearchInText -> searchPreviousScreenWhenStateIsValid()
        is ConfirmedDelete -> deleteItemAndShowToast(it)
        is CreatedWithArguments -> _effects.tryEmit(SearchArgumentProcessing(it.arguments, actions))
        ReceivedPromptForSpeechInput -> _effects.tryEmit(StartSpeechInput(actions))
        is ScreenWasStartedFrom -> searchOrigin.tryEmit(it.searchOrigin)
        is VoiceSearchResult -> onSearchValueChanged(it.term)
        is LoadMoreResults -> loadMoreSearchResults(it.startIndex)
        is Action.CloseKeyboard -> _effects.tryEmit(CloseKeyboard)
        StartSpeechInputFailed ->
          _effects.tryEmit(ShowToast(R.string.speech_not_supported))

        is ActivityResultReceived ->
          _effects.tryEmit(
            ProcessActivityResult(
              it.requestCode,
              it.resultCode,
              it.data,
              actions
            )
          )
      }
    }
  }

  private fun deleteItemAndShowToast(it: ConfirmedDelete) {
    _effects.tryEmit(
      DeleteRecentSearch(it.searchListItem, recentSearchRoomDao, viewModelScope)
    )
    _effects.tryEmit(ShowToast(R.string.delete_specific_search_toast))
  }

  private fun searchPreviousScreenWhenStateIsValid(): Any =
    _effects.tryEmit(SearchInPreviousScreen(uiState.value.searchState.searchTerm))

  private fun showDeleteDialog(longClick: OnItemLongClick) {
    _effects.tryEmit(
      ShowDeleteSearchDialog(
        longClick.searchListItem,
        actions,
        requireAlertDialogShower()
      )
    )
  }

  private fun saveSearchAndOpenItem(searchListItem: SearchListItem, openInNewTab: Boolean) {
    _effects.tryEmit(
      SaveSearchToRecents(
        recentSearchRoomDao,
        searchListItem,
        zimReaderContainer.id,
        viewModelScope
      )
    )
    _effects.tryEmit(OpenSearchItem(searchListItem, openInNewTab))
  }

  private fun updateSearchQuery(query: String) {
    updateUiState { it.copy(searchText = query) }
    debouncedSearchQuery.value = query.trim()
  }

  private suspend fun loadMoreSearchResults(startIndex: Int) {
    val uiState = uiState.value
    if (uiState.isLoadingMore) return

    // Show load more progressBar.
    updateUiState {
      it.copy(isLoading = false, isLoadingMore = true)
    }

    val moreResults = withContext(ioDispatcher) {
      uiState.searchState.getVisibleResults(startIndex, ioDispatcher = ioDispatcher)
    }

    val current = uiState.searchList
    val newItems = moreResults?.filter { newItem ->
      current.none { it == newItem }
    }
    val updatedState = when {
      // When there are no more items available in libkiwix to show.
      // We should keep the isLoadingMore = true so that it can not ask again for more results.
      moreResults == null -> uiState.copy(isLoadingMore = true)
      // Set the load more false because some duplicate items comes
      // from libkiwix that are already showing.
      newItems.isNullOrEmpty() -> uiState.copy(isLoadingMore = false)
      else -> uiState.copy(searchList = current + newItems, isLoadingMore = false)
    }
    updateUiState { updatedState }
  }

  private fun setIsPageSearchEnabled(searchText: String) {
    updateUiState {
      it.copy(findInPageMenuItem = searchText.isNotBlank() to it.findInPageMenuItem.second)
    }
  }

  fun onItemClick(it: SearchListItem) {
    closeKeyboard()
    actions.tryEmit(OnItemClick(it))
  }

  fun onItemLongClick(it: SearchListItem) {
    closeKeyboard()
    actions.tryEmit(OnItemLongClick(it))
  }

  fun onNewTabIconClick(it: SearchListItem) {
    closeKeyboard()
    actions.tryEmit(OnOpenInNewTabClick(it))
  }

  fun onSearchClear() {
    updateSearchQuery("")
    setIsPageSearchEnabled("")
  }

  fun onSearchValueChanged(searchText: String) {
    updateSearchQuery(searchText)
    setIsPageSearchEnabled(searchText)
  }

  fun onSuggestionItemClick(suggestionText: String) {
    updateUiState { it.copy(spellingCorrectionSuggestions = emptyList()) }
    onSearchValueChanged(suggestionText)
  }

  fun onKeyboardSubmitButtonClick(query: String) {
    uiState.value.searchList.firstOrNull {
      it.value.equals(query, ignoreCase = true)
    }?.let { onItemClick(it) }
  }

  fun closeKeyboard() {
    actions.tryEmit(Action.CloseKeyboard)
  }

  fun loadMoreSearchResults() {
    actions.tryEmit(Action.LoadMoreResults(uiState.value.searchList.size))
  }

  private inline fun updateUiState(updatedState: (SearchScreenUiState) -> SearchScreenUiState) {
    _uiState.update(updatedState)
  }
}

data class SearchResultsWithTerm(
  val searchTerm: String,
  val suggestionSearch: SuggestionSearch?,
  val searchMutex: Mutex?
)

data class SearchScreenUiState(
  val searchList: List<SearchListItem> = emptyList(),
  val searchText: String = "",
  val isLoading: Boolean = false,
  val isLoadingMore: Boolean = false,
  val spellingCorrectionSuggestions: List<String> = emptyList(),
  /**
   * Represents the state of the FIND_IN_PAGE menu item.
   *
   * A [Pair] containing:
   *  - [Boolean]: Whether the menu item is enabled (clickable).
   *  - [Boolean]: Whether the menu item is visible.
   */
  val findInPageMenuItem: Pair<Boolean, Boolean> = false to true,
  val searchOrigin: SearchOrigin = FromWebView,
  val searchState: SearchState = SearchState(
    "",
    SearchResultsWithTerm("", null, null),
    emptyList(),
    FromWebView
  )
)
