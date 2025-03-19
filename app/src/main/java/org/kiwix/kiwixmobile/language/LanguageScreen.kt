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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.kiwix.kiwixmobile.language.composables.LanguageList
import org.kiwix.kiwixmobile.language.composables.LoadingIndicator
import org.kiwix.kiwixmobile.language.viewmodel.Action
import org.kiwix.kiwixmobile.language.viewmodel.LanguageViewModel
import org.kiwix.kiwixmobile.language.viewmodel.State
import org.kiwix.kiwixmobile.language.viewmodel.State.Content

@Composable
fun LanguageScreen(
  languageViewModel: LanguageViewModel
) {
  val state by languageViewModel.state.observeAsState(State.Loading)
  val context = LocalContext.current
  val listState: LazyListState = rememberLazyListState()

  Column(modifier = Modifier.fillMaxSize()) {
    // spacer to account for top app bar
    Spacer(modifier = Modifier.height(56.dp))
    when (state) {
      State.Loading, State.Saving -> {
        LoadingIndicator()
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
