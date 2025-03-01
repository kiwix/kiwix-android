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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.language.composables.LanguageListItem.HeaderItem
import org.kiwix.kiwixmobile.language.composables.LanguageListItem.LanguageItem
import org.kiwix.kiwixmobile.language.composables.LanguageItemRow
import org.kiwix.kiwixmobile.language.composables.TopBar
import org.kiwix.kiwixmobile.language.viewmodel.Action
import org.kiwix.kiwixmobile.language.viewmodel.Action.Select
import org.kiwix.kiwixmobile.language.viewmodel.LanguageViewModel
import org.kiwix.kiwixmobile.language.viewmodel.State
import org.kiwix.kiwixmobile.language.viewmodel.State.Content
import org.kiwix.kiwixmobile.ui.theme.KiwixTheme

@Suppress("all")
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LanguageScreen(
  languageViewModel: LanguageViewModel,
  onNavigationClick: () -> Unit
) {
  val state by languageViewModel.state.observeAsState(State.Loading)
  val listState: LazyListState = rememberLazyListState()
  val context = LocalContext.current
  val coroutineScope: CoroutineScope = rememberCoroutineScope()
  var searchText by remember { mutableStateOf("") }
  var isSearchActive by remember { mutableStateOf(false) }
  var updateListState by remember { mutableStateOf(false) }

  KiwixTheme {
    Scaffold(
      topBar = {
        TopBar(
          title = stringResource(id = R.string.select_languages),
          searchText = searchText,
          isSearchActive = isSearchActive,
          onNavigationClick = onNavigationClick,
          onSearchClick = { isSearchActive = true },
          onBackClick = {
            if (isSearchActive) {
              isSearchActive = false
              searchText = ""
              languageViewModel.actions.offer(Action.Filter(searchText))
            }
          },
          onSearchTextSubmit = {
            languageViewModel.actions.offer(Action.SaveAll)
            updateListState = true
          },
          onSearchTextClear = {
            searchText = ""
            languageViewModel.actions.offer(Action.Filter(searchText))
          },
          onSearchTextChanged = {
            searchText = it
            languageViewModel.actions.offer(Action.Filter(it))
          }
        )
      }
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // spacer to account for top app bar
        Spacer(modifier = Modifier.height(56.dp))
        when (state) {
          State.Loading -> {
            LoadingIndicator()
          }

          is Content -> {
            LazyColumn(
              state = listState
            ) {
              fun areBothHeadersVisible(): Boolean {
                val visibleItemInfo = listState.layoutInfo.visibleItemsInfo
                val selectedHeaderVisible = visibleItemInfo.any {
                  it.key == "header_${HeaderItem.SELECTED}"
                }
                val otherHeaderVisible = visibleItemInfo.any {
                  it.key == "header_${HeaderItem.OTHER}"
                }
                val firstItemVisible = visibleItemInfo.any {
                  it.index == 0
                }
                if (firstItemVisible) {
                  return true
                }
                return selectedHeaderVisible && otherHeaderVisible
              }

              // make the list stay on top if both headers are visible
              coroutineScope.launch {
                if (areBothHeadersVisible()) {
                  listState.scrollToItem(0)
                }
              }

              // to stop the cast exception, need to update item list
              val viewItem = if (!updateListState) {
                (state as Content).viewItems
              } else {
                emptyList()
              }

              items(
                items = viewItem,
                key = { item ->
                  when (item) {
                    is HeaderItem -> "header_${item.id}"
                    is LanguageItem -> "language_${item.language.id}"
                  }
                }
              ) { item ->
                when (item) {
                  is HeaderItem -> {
                    Text(
                      text = when (item.id) {
                        HeaderItem.SELECTED -> stringResource(R.string.your_languages)
                        HeaderItem.OTHER -> stringResource(R.string.other_languages)
                        else -> ""
                      },
                      modifier = Modifier
                        .padding(
                          horizontal = 16.dp,
                          vertical = 8.dp
                        )
                        .animateItem(),
                      fontSize = 16.sp,
                      style = MaterialTheme.typography.headlineMedium,
                      color = MaterialTheme.colorScheme.onSecondary
                    )
                  }

                  is LanguageItem -> {
                    LanguageItemRow(
                      context = context,
                      modifier = Modifier
                        .animateItem(),
                      item = item,
                      onCheckedChange = {
                        languageViewModel.actions.offer(Select(it))
                      }
                    )
                  }
                }
              }
            }
          }

          State.Saving -> {
            LoadingIndicator()
          }
        }
      }
    }
  }
}

@Composable
fun LoadingIndicator() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator()
  }
}
