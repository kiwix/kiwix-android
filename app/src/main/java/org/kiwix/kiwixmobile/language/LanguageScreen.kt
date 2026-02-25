/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.language

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.CollectSideEffectWithActivity
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.search.SEARCH_FIELD_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixSearchView
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOUR_DP
import org.kiwix.kiwixmobile.language.composables.LanguageList
import org.kiwix.kiwixmobile.language.composables.LanguageListItem
import org.kiwix.kiwixmobile.language.viewmodel.Action
import org.kiwix.kiwixmobile.language.viewmodel.LanguageViewModel
import org.kiwix.kiwixmobile.language.viewmodel.State
import org.kiwix.kiwixmobile.language.viewmodel.State.Content
import org.kiwix.kiwixmobile.nav.destination.library.online.NO_CONTENT_VIEW_TEXT_TESTING_TAG

const val SAVE_ICON_TESTING_TAG = "saveLanguages"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguageScreenRoute(
  viewModelFactory: ViewModelProvider.Factory,
  navigateBack: () -> Unit,
) {
  val languageViewModel: LanguageViewModel = viewModel(factory = viewModelFactory)
  val state by languageViewModel.state.collectAsStateWithLifecycle()

  languageViewModel.effects.CollectSideEffectWithActivity { effect, activity ->
    effect.invokeWith(activity)
  }

  var searchText by rememberSaveable { mutableStateOf("") }
  var isSearchActive by rememberSaveable { mutableStateOf(false) }

  fun resetSearchState() {
    // clears the search text and resets the filter
    searchText = ""
    languageViewModel.actions.tryEmit(Action.Filter(searchText))
  }

  KiwixTheme {
    LanguageScreen(
      searchText = searchText,
      isSearchActive = isSearchActive,
      state = state,
      actionMenuItemList = appBarActionMenuList(
        isSearchActive = isSearchActive,
        onSearchClick = { isSearchActive = true },
        onSaveClick = {
          languageViewModel.actions.tryEmit(Action.Save)
        }
      ),
      onClearClick = { resetSearchState() },
      onAppBarValueChange = {
        searchText = it
        languageViewModel.actions.tryEmit(Action.Filter(it.trim()))
      },
      selectLanguageItem = { languageItem ->
        languageViewModel.actions.tryEmit(Action.Select(languageItem))
      },
      navigationIcon = {
        NavigationIcon(
          iconItem = if (isSearchActive) {
            IconItem.Vector(Icons.AutoMirrored.Filled.ArrowBack)
          } else {
            IconItem.Drawable(
              R.drawable.ic_close_white_24dp
            )
          },
          onClick = {
            if (isSearchActive) {
              isSearchActive = false
              resetSearchState()
            } else {
              navigateBack()
            }
          }
        )
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ComposableLambdaParameterNaming")
@Suppress("LongParameterList")
@Composable
private fun LanguageScreen(
  searchText: String,
  isSearchActive: Boolean,
  state: State,
  actionMenuItemList: List<ActionMenuItem>,
  selectLanguageItem: (item: LanguageListItem.LanguageItem) -> Unit,
  onClearClick: () -> Unit,
  onAppBarValueChange: (String) -> Unit,
  navigationIcon: @Composable() () -> Unit
) {
  val listState: LazyListState = rememberLazyListState()
  val context = LocalContext.current

  Scaffold(topBar = {
    KiwixAppBar(
      title = stringResource(R.string.select_language),
      navigationIcon = navigationIcon,
      actionMenuItems = actionMenuItemList,
      searchBar = if (isSearchActive) {
        {
          KiwixSearchView(
            value = searchText,
            searchViewTextFiledTestTag = SEARCH_FIELD_TESTING_TAG,
            onValueChange = onAppBarValueChange,
            onClearClick = onClearClick,
            modifier = Modifier
          )
        }
      } else {
        null
      }
    )
  }) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        // setting bottom padding to zero to avoid accounting for Bottom bar
        .padding(
          top = innerPadding.calculateTopPadding(),
          start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
          end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
          bottom = 0.dp
        )
    ) {
      when (state) {
        State.Loading, State.Saving -> {
          LoadingScreen()
        }

        is Content -> {
          LanguageList(
            state = state,
            context = context,
            listState = listState,
            selectLanguageItem = selectLanguageItem
          )
        }

        is State.Error -> ShowErrorMessage(state.errorMessage)
      }
    }
  }
}

@Composable
fun ShowErrorMessage(errorMessage: String) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = errorMessage,
      textAlign = TextAlign.Center,
      modifier = Modifier
        .padding(horizontal = FOUR_DP)
        .semantics { testTag = NO_CONTENT_VIEW_TEXT_TESTING_TAG }
    )
  }
}

@Composable
fun LoadingScreen() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    ContentLoadingProgressBar()
  }
}

private fun appBarActionMenuList(
  isSearchActive: Boolean,
  onSearchClick: () -> Unit,
  onSaveClick: () -> Unit
): List<ActionMenuItem> {
  return listOfNotNull(
    when {
      !isSearchActive -> ActionMenuItem(
        icon = IconItem.Drawable(R.drawable.action_search),
        contentDescription = R.string.search_label,
        onClick = onSearchClick,
        testingTag = SEARCH_ICON_TESTING_TAG
      )

      else -> null // Handle the case when both conditions are false
    },
    // Second item: always included
    ActionMenuItem(
      icon = IconItem.Vector(Icons.Default.Check),
      contentDescription = R.string.save_languages,
      onClick = onSaveClick,
      testingTag = SAVE_ICON_TESTING_TAG
    )
  )
}
