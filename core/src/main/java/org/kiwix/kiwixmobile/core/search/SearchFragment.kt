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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.cachedComponent
import org.kiwix.kiwixmobile.core.extensions.closeKeyboard
import org.kiwix.kiwixmobile.core.extensions.coreMainActivity
import org.kiwix.kiwixmobile.core.extensions.update
import org.kiwix.kiwixmobile.core.extensions.viewModel
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.search.viewmodel.Action
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchViewModel
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.utils.EXTRA_IS_WIDGET_VOICE
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import javax.inject.Inject

const val NAV_ARG_SEARCH_STRING = "searchString"

class SearchFragment : BaseFragment() {
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

  @Inject lateinit var dialogShower: DialogShower

  private val searchViewModel by lazy {
    viewModel<SearchViewModel>(viewModelFactory)
  }

  private var composeView: ComposeView? = null

  private val searchScreenState = mutableStateOf(
    SearchScreenState(
      searchList = emptyList(),
      isLoading = false,
      shouldShowLoadingMoreProgressBar = false,
      searchText = "",
      onSearchViewClearClick = { onSearchClear() },
      onSearchViewValueChange = { onSearchValueChanged(it) },
      onItemClick = { onItemClick(it) },
      onNewTabIconClick = { onItemClickNewTab(it) },
      onItemLongClick = {
        searchViewModel.actions.trySend(Action.OnItemLongClick(it)).isSuccess
      },
      navigationIcon = {
        NavigationIcon(onClick = {
          requireActivity().onBackPressedDispatcher.onBackPressed()
        })
      },
      onLoadMore = {
        searchViewModel.loadMore()
      },
      onKeyboardSubmitButtonClick = {
        onSearchValueChanged(it)
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
  ): View =
    ComposeView(requireContext()).also { composeView = it }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    composeView?.setContent {
      DisposableEffect(Unit) {
        (activity as CoreMainActivity).customBackHandler.value = {
          handleBackPress()
        }
        onDispose {
          (activity as CoreMainActivity).customBackHandler.value = null
        }
      }

      SearchScreen(
        searchScreenState.value,
        actionMenuItems(),
        searchScreenState.value.shouldShowLoadingMoreProgressBar
      )

      DialogHost(dialogShower as AlertDialogShower)
    }

    searchViewModel.setAlertDialogShower(dialogShower as AlertDialogShower)
    observeViewModel()
    handleArguments()
  }

  private fun observeViewModel() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          searchViewModel.state.collect { state ->
            searchScreenState.update {
              copy(
                searchList = state.results,
                isLoading = state.isLoading,
                shouldShowLoadingMoreProgressBar = !state.canLoadMore,
                searchText = state.searchTerm
              )
            }
          }
        }

        launch {
          searchViewModel.effects
            .receiveAsFlow()
            .collect {
              it.invokeWith(this@SearchFragment.coreMainActivity)
            }
        }
      }
    }

    searchViewModel.voiceSearchResult.observe(viewLifecycleOwner) {
      it?.let(::onSearchValueChanged)
    }
  }

  private fun handleArguments() {
    arguments?.getString(NAV_ARG_SEARCH_STRING)?.let {
      onSearchValueChanged(it)
    }

    searchViewModel.actions.trySend(
      Action.CreatedWithArguments(Bundle(arguments))
    )

    arguments?.remove(EXTRA_IS_WIDGET_VOICE)
  }

  private fun onSearchClear() {
    searchScreenState.update { copy(searchText = "") }
    searchViewModel.actions.trySend(Filter(""))
  }

  private fun onSearchValueChanged(text: String) {
    searchScreenState.update { copy(searchText = text) }
    searchViewModel.actions.trySend(Filter(text))
  }

  private fun onSuggestionItemClick(text: String) {
    onSearchValueChanged(text)
  }

  private fun onItemClick(item: SearchListItem) {
    closeKeyboard()
    searchViewModel.actions.trySend(Action.OnItemClick(item))
  }

  private fun onItemClickNewTab(item: SearchListItem) {
    closeKeyboard()
    searchViewModel.actions.trySend(Action.OnOpenInNewTabClick(item))
  }

  private fun handleBackPress(): FragmentActivityExtensions.Super {
    val readerRoute =
      (requireActivity() as CoreMainActivity).readerFragmentRoute
    (requireActivity() as CoreMainActivity)
      .navController
      .popBackStack(readerRoute, false)
    return FragmentActivityExtensions.Super.ShouldCall
  }

  private fun actionMenuItems(): List<ActionMenuItem> =
    listOf(
      ActionMenuItem(
        contentDescription = R.string.menu_search_in_text,
        iconButtonText = getString(R.string.menu_search_in_text),
        testingTag = FIND_IN_PAGE_TESTING_TAG,
        isEnabled = searchScreenState.value.searchText.isNotBlank(),
        onClick = {
          searchViewModel.actions.trySend(Action.ClickedSearchInText)
        }
      )
    )

  @Suppress("DEPRECATION")
  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    searchViewModel.actions.trySend(
      Action.ActivityResultReceived(requestCode, resultCode, data)
    )
  }

  override fun onDestroyView() {
    super.onDestroyView()
    composeView?.disposeComposition()
    composeView = null
  }
}
