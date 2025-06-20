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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.CollectSideEffectWithActivity
import org.kiwix.kiwixmobile.core.search.SEARCH_FIELD_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixSearchView
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.language.composables.LanguageList
import org.kiwix.kiwixmobile.language.viewmodel.Action
import org.kiwix.kiwixmobile.language.viewmodel.LanguageViewModel
import org.kiwix.kiwixmobile.language.viewmodel.State
import org.kiwix.kiwixmobile.language.viewmodel.State.Content

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ComposableLambdaParameterNaming")
@Composable
fun LanguageScreen(
  searchText: String,
  isSearchActive: Boolean,
  languageViewModel: LanguageViewModel,
  actionMenuItemList: List<ActionMenuItem>,
  onClearClick: () -> Unit,
  onAppBarValueChange: (String) -> Unit,
  navigationIcon: @Composable() () -> Unit
) {
  val state by languageViewModel.state.collectAsState(State.Loading)
  val listState: LazyListState = rememberLazyListState()
  val context = LocalContext.current
  languageViewModel.effects.CollectSideEffectWithActivity { effect, activity ->
    effect.invokeWith(activity)
  }
  Scaffold(topBar = {
    KiwixAppBar(
      title = stringResource(R.string.select_languages),
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
            selectLanguageItem = { languageItem ->
              languageViewModel.actions.tryEmit(Action.Select(languageItem))
            }
          )
        }
      }
    }
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
