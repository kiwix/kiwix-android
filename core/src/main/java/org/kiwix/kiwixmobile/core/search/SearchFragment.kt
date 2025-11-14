/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.core.search

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.cachedComponent
import org.kiwix.kiwixmobile.core.extensions.closeKeyboard
import org.kiwix.kiwixmobile.core.extensions.coreMainActivity
import org.kiwix.kiwixmobile.core.extensions.update
import org.kiwix.kiwixmobile.core.extensions.viewModel
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.search.viewmodel.Action
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ActivityResultReceived
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ClickedSearchInText
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.OnOpenInNewTabClick
import org.kiwix.kiwixmobile.core.search.viewmodel.MAX_SUGGEST_WORD_COUNT
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchOrigin.FromWebView
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchState
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchViewModel
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.utils.EXTRA_IS_WIDGET_VOICE
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.files.Log
import javax.inject.Inject

const val NAV_ARG_SEARCH_STRING = "searchString"

class SearchFragment : BaseFragment() {
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

  @Inject lateinit var dialogShower: DialogShower

  val searchViewModel by lazy { viewModel<SearchViewModel>(viewModelFactory) }
  private var isDataLoading = mutableStateOf(false)
  private var renderingJob: Job? = null

  /**
   * Represents the state of the FIND_IN_PAGE menu item.
   *
   * A [Pair] containing:
   *  - [Boolean]: Whether the menu item is enabled (clickable).
   *  - [Boolean]: Whether the menu item is visible.
   */
  private var findInPageMenuItem = mutableStateOf(false to false)
  private var composeView: ComposeView? = null
  private val searchScreenState = mutableStateOf(
    SearchScreenState(
      searchList = emptyList(),
      isLoading = true,
      shouldShowLoadingMoreProgressBar = false,
      searchText = "",
      onSearchViewClearClick = { onSearchClear() },
      onSearchViewValueChange = { onSearchValueChanged(it) },
      onItemClick = { onItemClick(it) },
      onNewTabIconClick = { onItemClickNewTab(it) },
      onItemLongClick = {
        searchViewModel.actions.trySend(OnItemLongClick(it)).isSuccess
      },
      navigationIcon = {
        NavigationIcon(onClick = { requireActivity().onBackPressedDispatcher.onBackPressed() })
      },
      onLoadMore = { loadMoreSearchResult() },
      onKeyboardSubmitButtonClick = {
        getSearchListItemForQuery(it)?.let(::onItemClick)
      },
      spellingCorrectionSuggestions = emptyList(),
      onSuggestionClick = { onSuggestionItemClick(it) }
    )
  )

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? = ComposeView(requireContext()).also {
    composeView = it
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    composeView?.apply {
      setContent {
        DisposableEffect(Unit) {
          (activity as CoreMainActivity).customBackHandler.value = { handleBackPress() }
          onDispose {
            (activity as CoreMainActivity).customBackHandler.value = null
          }
        }
        SearchScreen(
          searchScreenState.value,
          actionMenuItems(),
          isDataLoading.value
        )
        DialogHost(dialogShower as AlertDialogShower)
        DisposableEffect(Unit) {
          onDispose {
            // Dispose UI resources when this Compose view is removed. Compose disposes
            // its content before Fragment.onDestroyView(), so callback and listener cleanup
            // should happen here.
            destroyViews()
          }
        }
      }
    }
    searchViewModel.setAlertDialogShower(dialogShower as AlertDialogShower)
    observeViewModelData()
    handleSearchArgument()
  }

  private fun handleSearchArgument() {
    val searchStringFromArguments = arguments?.getString(NAV_ARG_SEARCH_STRING)
    if (searchStringFromArguments != null) {
      onSearchValueChanged(searchStringFromArguments)
    }
    val argsCopy = Bundle(arguments)
    searchViewModel.actions.trySend(Action.CreatedWithArguments(argsCopy)).isSuccess
    arguments?.remove(EXTRA_IS_WIDGET_VOICE)
  }

  private fun observeViewModelData() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          searchViewModel.effects.collect { effect ->
            effect.invokeWith(this@SearchFragment.coreMainActivity)
          }
        }

        launch {
          searchViewModel.state.collect { state ->
            render(state)
          }
        }
      }
    }
    searchViewModel.voiceSearchResult.observe(viewLifecycleOwner) { searchTerm ->
      searchTerm?.let {
        onSearchValueChanged(it)
      }
    }
  }

  /**
   * Loads more search results and appends them to the existing search results list in the RecyclerView.
   * This function is typically triggered when the RecyclerView is near about its last item.
   */
  private fun loadMoreSearchResult() {
    if (isDataLoading.value) return
    isDataLoading.value = true
    val searchList = searchScreenState.value.searchList
    // Show a loading indicator while data is being loaded
    searchScreenState.update { copy(shouldShowLoadingMoreProgressBar = true) }
    lifecycleScope.launch {
      // Request more search results from the ViewModel, providing the start
      // index and existing results
      searchViewModel.loadMoreSearchResults(searchList.size, searchList)
        .let { searchResults ->
          // Hide the loading indicator when data loading is complete
          searchScreenState.update { copy(shouldShowLoadingMoreProgressBar = false) }
          // Update data loading status based on the received search results
          isDataLoading.value = when {
            searchResults == null -> true
            searchResults.isEmpty() -> false
            else -> {
              // Append the new search results to the existing list
              searchScreenState.update {
                copy(searchList = searchScreenState.value.searchList + searchResults)
              }
              false
            }
          }
        }
    }
  }

  private fun handleBackPress(): FragmentActivityExtensions.Super {
    goBack()
    return FragmentActivityExtensions.Super.ShouldCall
  }

  override fun onDestroyView() {
    super.onDestroyView()
    destroyViews()
  }

  private fun destroyViews() {
    renderingJob?.cancel()
    renderingJob = null
    activity?.intent?.action = null
    composeView?.disposeComposition()
    composeView = null
  }

  private fun goBack() {
    val readerFragmentRoute = (activity as CoreMainActivity).readerFragmentRoute
    (requireActivity() as CoreMainActivity).navController.popBackStack(readerFragmentRoute, false)
  }

  private fun getSearchListItemForQuery(query: String): SearchListItem? =
    searchScreenState.value.searchList.firstOrNull {
      it.value.equals(query, ignoreCase = true)
    }

  private fun onSearchClear() {
    searchScreenState.update { copy(searchText = "") }
    setIsPageSearchEnabled("")
    searchEntryForSearchTerm("")
  }

  private fun onSearchValueChanged(searchText: String) {
    searchScreenState.update { copy(searchText = searchText) }
    setIsPageSearchEnabled(searchText)
    searchEntryForSearchTerm(searchText)
  }

  private fun searchEntryForSearchTerm(searchText: String) {
    searchViewModel.searchResults(searchText.trim())
  }

  private fun onSuggestionItemClick(suggestionText: String) {
    searchScreenState.update { copy(spellingCorrectionSuggestions = emptyList()) }
    onSearchValueChanged(suggestionText)
  }

  private fun actionMenuItems() = listOfNotNull(
    // Check if the `FIND_IN_PAGE` is visible or not.
    // If visible then show it in menu.
    if (findInPageMenuItem.value.second) {
      ActionMenuItem(
        contentDescription = R.string.menu_search_in_text,
        onClick = {
          searchViewModel.actions.trySend(ClickedSearchInText).isSuccess
        },
        testingTag = FIND_IN_PAGE_TESTING_TAG,
        iconButtonText = requireActivity().getString(R.string.menu_search_in_text),
        isEnabled = findInPageMenuItem.value.first
      )
    } else {
      // If `FIND_IN_PAGE` is not visible return null so that it will not show on the menu item.
      null
    }
  )

  private suspend fun render(state: SearchState) {
    renderingJob?.apply {
      // cancel the children job. Since we are getting the result on IO thread
      // with `withContext` that is child for this job
      cancelChildren()
      // `cancelAndJoin` cancels the previous running job and waits for it to completely cancel.
      cancelAndJoin()
    }
    // Check if the fragment is visible on the screen. This method called multiple times
    // (7-14 times) when an item in the search list is clicked, which leads to unnecessary
    // data loading and also causes a crash.
    // The issue arises because the searchViewModel takes a moment to detach from the window,
    // and during this time, this method is called multiple times due to the rendering process.
    // To avoid unnecessary data loading and prevent crashes, we check if the search screen is
    // visible to the user before proceeding. If the screen is not visible,
    // we skip the data loading process.
    if (!isVisible) return
    isDataLoading.value = false
    findInPageMenuItem.value = findInPageMenuItem.value.first to (state.searchOrigin == FromWebView)
    setIsPageSearchEnabled(state.searchTerm)
    searchScreenState.update { copy(isLoading = true) }
    renderingJob =
      lifecycleScope.launch {
        try {
          val searchResult = state.getVisibleResults(ZERO, coroutineContext[Job])
          searchResult?.let {
            searchScreenState.update {
              copy(searchList = it)
            }
          }
        } catch (ignore: CancellationException) {
          Log.e("SEARCH_RESULT", "Cancelled the previous job ${ignore.message}")
        } catch (ignore: Exception) {
          Log.e(
            "SEARCH_RESULT",
            "Error in getting searched result\nOriginal exception ${ignore.message}"
          )
        } finally {
          updateSuggestedWords()
        }
      }
  }

  /**
   * Updates the suggested word list using the libkiwix spellings database.
   */
  private suspend fun updateSuggestedWords() {
    val onlyRecentSearches =
      searchScreenState.value.searchList.all { it is SearchListItem.RecentSearchListItem }

    if (onlyRecentSearches && searchScreenState.value.searchText.isNotEmpty()) {
      val suggestedWords = searchViewModel.getSuggestedSpelledWords(
        searchScreenState.value.searchText,
        MAX_SUGGEST_WORD_COUNT
      )

      searchScreenState.update {
        copy(spellingCorrectionSuggestions = suggestedWords, isLoading = false)
      }
    } else {
      searchScreenState.update {
        copy(spellingCorrectionSuggestions = emptyList(), isLoading = false)
      }
    }
  }

  private fun setIsPageSearchEnabled(searchText: String) {
    findInPageMenuItem.value = searchText.isNotBlank() to findInPageMenuItem.value.second
  }

  private fun onItemClick(it: SearchListItem) {
    closeKeyboard()
    searchViewModel.actions.trySend(OnItemClick(it)).isSuccess
  }

  private fun onItemClickNewTab(it: SearchListItem) {
    closeKeyboard()
    searchViewModel.actions.trySend(OnOpenInNewTabClick(it)).isSuccess
  }

  @Suppress("DEPRECATION")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    searchViewModel.actions.trySend(
      ActivityResultReceived(
        requestCode,
        resultCode,
        data
      )
    ).isSuccess
  }
}
