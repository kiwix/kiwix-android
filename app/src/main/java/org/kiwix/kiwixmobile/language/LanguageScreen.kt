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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.components.AppBarTextField
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.language.composables.LanguageList
import org.kiwix.kiwixmobile.language.viewmodel.Action
import org.kiwix.kiwixmobile.language.viewmodel.LanguageViewModel
import org.kiwix.kiwixmobile.language.viewmodel.State
import org.kiwix.kiwixmobile.language.viewmodel.State.Content

@Composable
fun LanguageScreen(
  searchText: String,
  isSearchActive: Boolean,
  languageViewModel: LanguageViewModel,
  actionMenuItemList: List<ActionMenuItem>,
  onAppBarValueChange: (String) -> Unit,
  appBarTextFieldTestTag: String,
  content: @Composable() () -> Unit,
) {
  val state by languageViewModel.state.observeAsState(State.Loading)
  val listState: LazyListState = rememberLazyListState()
  val context = LocalContext.current

  Scaffold(topBar = {
    KiwixAppBar(
      titleId = R.string.select_languages,
      navigationIcon = content,
      actionMenuItems = actionMenuItemList,
      searchBar = if (isSearchActive) {
        {
          AppBarTextField(
            value = searchText,
            testTag = appBarTextFieldTestTag,
            onValueChange = onAppBarValueChange
          )
        }
      } else {
        null
      }
    )
  }) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize()
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
          val viewItem = (state as Content).viewItems

          LaunchedEffect(viewItem) {
            snapshotFlow(listState::firstVisibleItemIndex)
              .collect {
                if (listState.firstVisibleItemIndex == 2) {
                  listState.animateScrollToItem(0)
                }
              }
          }

          LanguageList(
            context = context,
            listState = listState,
            viewItem = viewItem,
            selectLanguageItem = { languageItem ->
              languageViewModel.actions.offer(Action.Select(languageItem))
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
