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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.RecentSearchRoomDao
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.SearchListItem
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

private const val PAGE_SIZE = 20
private const val DEBOUNCE_DELAY = 150L

class SearchViewModel @Inject constructor(
  private val recentSearchRoomDao: RecentSearchRoomDao,
  private val zimReaderContainer: ZimReaderContainer,
  private val searchResultGenerator: SearchResultGenerator
) : ViewModel() {
  val state = MutableStateFlow(
    SearchState(
      searchTerm = "",
      results = emptyList(),
      recentResults = emptyList(),
      isLoading = false,
      canLoadMore = true,
      searchOrigin = SearchOrigin.FromWebView
    )
  )

  val effects = Channel<SideEffect<*>>(Channel.UNLIMITED)
  val actions = Channel<Action>(Channel.UNLIMITED)

  val voiceSearchResult = MutableLiveData<String?>(null)

  private lateinit var alertDialogShower: AlertDialogShower

  private val searchQuery = MutableStateFlow("")
  private var suggestionSearch: SuggestionSearch? = null
  private var currentIndex = 0
  private var canLoadMore = true

  @Suppress("InjectDispatcher")
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

  init {
    observeActions()
    observeSearchQuery()
    observeRecentSearches()
  }

  fun setAlertDialogShower(alertDialogShower: AlertDialogShower) {
    this.alertDialogShower = alertDialogShower
  }

  private fun observeSearchQuery() {
    viewModelScope.launch {
      searchQuery
        .debounce(DEBOUNCE_DELAY)
        .distinctUntilChanged()
        .collect { query ->
          performNewSearch(query)
        }
    }
  }

  private fun observeActions() {
    viewModelScope.launch {
      actions.consumeEach { action ->
        when (action) {
          is Action.Filter -> searchQuery.value = action.term
          is Action.OnItemClick -> openItem(action.searchListItem, false)
          is Action.OnOpenInNewTabClick -> openItem(action.searchListItem, true)
          is Action.OnItemLongClick -> showDeleteDialog(action)
          is Action.ConfirmedDelete -> deleteRecent(action)
          Action.ClickedSearchInText ->
            effects.trySend(SearchInPreviousScreen(state.value.searchTerm))

          Action.ExitedSearch -> effects.trySend(PopFragmentBackstack)
          is Action.VoiceSearchResult -> voiceSearchResult.value = action.term
          is Action.ScreenWasStartedFrom ->
            state.value = state.value.copy(searchOrigin = action.searchOrigin)

          is Action.CreatedWithArguments ->
            effects.trySend(SearchArgumentProcessing(action.arguments, actions))

          Action.ReceivedPromptForSpeechInput ->
            effects.trySend(StartSpeechInput(actions))

          Action.StartSpeechInputFailed ->
            effects.trySend(ShowToast(R.string.speech_not_supported))

          is Action.ActivityResultReceived ->
            effects.trySend(
              ProcessActivityResult(
                action.requestCode,
                action.resultCode,
                action.data,
                actions
              )
            )
        }
      }
    }
  }

  private fun observeRecentSearches() {
    viewModelScope.launch {
      recentSearchRoomDao
        .recentSearches(zimReaderContainer.id)
        .collect { recents ->
          // Only update recents; actual list rendering is decided in Fragment
          state.value = state.value.copy(
            recentResults = recents
          )
        }
    }
  }

  private fun performNewSearch(query: String) {
    currentIndex = 0
    canLoadMore = true

    viewModelScope.launch {
      if (query.isBlank()) {
        state.value = state.value.copy(
          searchTerm = "",
          results = state.value.recentResults,
          isLoading = false,
          canLoadMore = false
        )
        return@launch
      }

      currentIndex = 0
      canLoadMore = true
      state.value = state.value.copy(
        searchTerm = query,
        results = emptyList()
      )

      suggestionSearch =
        searchResultGenerator.generateSearchResults(
          query,
          zimReaderContainer.zimFileReader
        )

      val firstPage = loadNextPage()

      state.value = state.value.copy(
        results = firstPage,
        canLoadMore = firstPage.isNotEmpty()
      )
    }
  }

  fun loadMore() {
    if (!canLoadMore) return

    viewModelScope.launch {
      val more = loadNextPage()
      canLoadMore = more.isNotEmpty()
    }
  }

  private suspend fun loadNextPage(): List<SearchListItem> =
    withContext(ioDispatcher) {
      val iterator =
        suggestionSearch?.getResults(currentIndex, currentIndex + PAGE_SIZE)
          ?: return@withContext emptyList()

      val list = mutableListOf<SearchListItem>()
      while (iterator.hasNext()) {
        val entry = iterator.next()
        list.add(SearchListItem.ZimSearchResultListItem(entry.title, entry.path))
      }

      currentIndex += list.size
      list
    }

  private fun openItem(item: SearchListItem, newTab: Boolean) {
    effects.trySend(
      SaveSearchToRecents(
        recentSearchRoomDao,
        item,
        zimReaderContainer.id,
        viewModelScope
      )
    )
    effects.trySend(OpenSearchItem(item, newTab))
  }

  private fun showDeleteDialog(action: Action.OnItemLongClick) {
    effects.trySend(
      ShowDeleteSearchDialog(
        action.searchListItem,
        actions,
        alertDialogShower
      )
    )
  }

  private fun deleteRecent(action: Action.ConfirmedDelete) {
    effects.trySend(
      DeleteRecentSearch(
        action.searchListItem,
        recentSearchRoomDao,
        viewModelScope
      )
    )
    effects.trySend(ShowToast(R.string.delete_specific_search_toast))
  }
}
