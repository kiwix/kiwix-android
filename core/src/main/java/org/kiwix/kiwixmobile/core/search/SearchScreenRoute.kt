/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.CollectSideEffectWithActivity
import org.kiwix.kiwixmobile.core.extensions.closeKeyboard
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.search.viewmodel.Action
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchViewModel
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower

const val NAV_ARG_SEARCH_STRING = "searchString"

@Suppress("LongMethod")
@Composable
fun SearchScreenRoute(
  viewModelFactory: ViewModelProvider.Factory,
  dialogShower: DialogShower,
  arguments: Bundle?,
  coreMainActivity: CoreMainActivity
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  val viewModel: SearchViewModel = viewModel(factory = viewModelFactory)

  // Voice Intent
  DisposableEffect(Unit) {
    coreMainActivity.activityResultForwarder =
      { requestCode, resultCode, data ->
        viewModel.actions.tryEmit(
          Action.ActivityResultReceived(
            requestCode,
            resultCode,
            data
          )
        )
      }

    onDispose {
      coreMainActivity.activityResultForwarder = null
    }
  }

  val state by viewModel.uiState.collectAsStateWithLifecycle()

  // Handles SideEffects
  viewModel.effects.CollectSideEffectWithActivity { effect, activity ->
    effect.invokeWith(activity)
  }

  // Search Results
  LaunchedEffect(Unit) {
    viewModel.setAlertDialogShower(dialogShower as AlertDialogShower)

    arguments?.getString(NAV_ARG_SEARCH_STRING)?.let {
      viewModel.updateSearchQuery(it)
    }

    viewModel.actions.tryEmit(
      Action.CreatedWithArguments(Bundle(arguments))
    )
  }
  val screenState = SearchScreenState(
    searchList = state.searchList,
    isLoading = state.isLoading,
    shouldShowLoadingMoreProgressBar = state.isLoadingMore,
    searchText = state.searchText,
    onSearchViewClearClick = {
      viewModel.updateSearchQuery("")
    },
    onSearchViewValueChange = {
      viewModel.updateSearchQuery(it)
    },
    onItemClick = {
      coreMainActivity.currentFocus?.closeKeyboard()
      viewModel.actions.tryEmit(Action.OnItemClick(it))
    },
    onItemLongClick = {
      viewModel.actions.tryEmit(Action.OnItemLongClick(it))
    },
    onNewTabIconClick = {
      coreMainActivity.currentFocus?.closeKeyboard()
      viewModel.actions.tryEmit(Action.OnOpenInNewTabClick(it))
    },
    onKeyboardSubmitButtonClick = { query ->
      state.searchList.firstOrNull { it.value.equals(query, true) }
        ?.let { viewModel.actions.tryEmit(Action.OnItemClick(it)) }
    },
    navigationIcon = {
      NavigationIcon(
        onClick = {
          coreMainActivity.currentFocus?.closeKeyboard()
          coreMainActivity.onBackPressedDispatcher.onBackPressed()
        }
      )
    },
    spellingCorrectionSuggestions = state.spellingSuggestions,
    onSuggestionClick = {
      viewModel.updateSearchQuery(it)
    },
    onLoadMore = {
      coroutineScope.launch {
        viewModel.loadMoreSearchResults(state.searchList.size)
      }
    }
  )

  SearchScreen(
    screenState,
    buildActionMenuItems(viewModel, context, state.showFindInPage),
    state.isLoadingMore
  )
}

private fun buildActionMenuItems(
  viewModel: SearchViewModel,
  context: Context,
  showFindInPage: Boolean
): List<ActionMenuItem> {
  return listOf(
    ActionMenuItem(
      contentDescription = R.string.search_label,
      icon = IconItem.Drawable(R.drawable.ic_mic_black_24dp),
      testingTag = VOICE_SEARCH_TESTING_TAG,
      isEnabled = true,
      onClick = {
        viewModel.actions.tryEmit(Action.ReceivedPromptForSpeechInput)
      }
    ),
    ActionMenuItem(
      contentDescription = R.string.menu_search_in_text,
      iconButtonText = context.getString(R.string.menu_search_in_text),
      testingTag = FIND_IN_PAGE_TESTING_TAG,
      isEnabled = showFindInPage,
      onClick = {
        viewModel.actions.tryEmit(Action.ClickedSearchInText)
      }
    )
  )
}
