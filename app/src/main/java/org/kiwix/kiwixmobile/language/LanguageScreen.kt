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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.language.composables.AppBarTextField
import org.kiwix.kiwixmobile.language.composables.LanguageList
import org.kiwix.kiwixmobile.language.composables.LanguageListItem.LanguageItem
import org.kiwix.kiwixmobile.language.composables.LoadingIndicator
import org.kiwix.kiwixmobile.language.viewmodel.State
import org.kiwix.kiwixmobile.language.viewmodel.State.Content

const val SEARCH_ICON_TESTING_TAG = "search"
const val SAVE_ICON_TESTING_TAG = "saveLanguages"
const val SEARCH_FIELD_TESTING_TAG = "searchField"

@Suppress("all")
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LanguageScreen(
  viewModelState: MutableLiveData<State>,
  selectLanguageItem: (LanguageItem) -> Unit,
  filterText: (String) -> Unit,
  onNavigationClick: () -> Unit,
  saveLanguages: () -> Unit
) {
  val state by viewModelState.observeAsState(State.Loading)
  val context = LocalContext.current
  var searchText by remember { mutableStateOf("") }
  var isSearchActive by remember { mutableStateOf(false) }
  var updateListState by remember { mutableStateOf(false) }
  val listState: LazyListState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val scrollToTop = {
    coroutineScope.launch {
      listState.scrollToItem(0)
    }
  }

  KiwixTheme {
    Scaffold(
      topBar = {
        KiwixAppBar(
          R.string.select_languages,
          {
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
                  searchText = ""
                  filterText(searchText)
                } else {
                  onNavigationClick()
                  scrollToTop()
                }
              }
            )
          },
          listOfNotNull(
            // First item: conditionally include based on search state
            when {
              !isSearchActive -> ActionMenuItem(
                icon = IconItem.Drawable(R.drawable.action_search),
                contentDescription = R.string.search_label,
                onClick = {
                  isSearchActive = true
                },
                iconTint = Color.White,
                isEnabled = true,
                testingTag = SEARCH_ICON_TESTING_TAG
              )

              searchText.isNotEmpty() -> ActionMenuItem(
                icon = IconItem.Drawable(R.drawable.ic_clear_white_24dp),
                contentDescription = R.string.search_label,
                onClick = {
                  searchText = ""
                  filterText(searchText)
                },
                iconTint = Color.White,
                isEnabled = true,
                testingTag = ""
              )

              else -> null // Handle the case when both conditions are false
            },
            // Second item: always included
            ActionMenuItem(
              icon = IconItem.Vector(Icons.Default.Check),
              contentDescription = R.string.save_languages,
              onClick = {
                saveLanguages()
                updateListState = true
              },
              iconTint = Color.White,
              isEnabled = true,
              testingTag = SAVE_ICON_TESTING_TAG
            )
          ),
          searchBar = if (isSearchActive) {
            {
              AppBarTextField(
                value = searchText,
                onValueChange = {
                  searchText = it
                  filterText(it)
                },
                testTag = SEARCH_FIELD_TESTING_TAG,
                hint = stringResource(R.string.search_label),
                keyboardOptions = KeyboardOptions.Default,
                keyboardActions = KeyboardActions.Default
              )
            }
          } else {
            null
          }
        )
      }
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // spacer to account for top app bar
        Spacer(modifier = Modifier.height(56.dp))
        when (state) {
          State.Loading, State.Saving -> {
            LoadingIndicator()
          }

          is Content -> {
            val viewItem = if (!updateListState) {
              (state as Content).viewItems
            } else {
              emptyList()
            }
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
              selectLanguageItem = {
                selectLanguageItem(it)
              }
            )
          }
        }
      }
    }
  }
}

@Preview
@Composable
fun LanguageScreenPreview() {
  val languages = listOf(
    Language(
      id = 1,
      active = true,
      occurencesOfLanguage = 142,
      language = "English",
      languageLocalized = "English",
      languageCode = "eng",
      languageCodeISO2 = "en"
    ),
    Language(
      id = 2,
      active = true,
      occurencesOfLanguage = 86,
      language = "German",
      languageLocalized = "Deutsch",
      languageCode = "deu",
      languageCodeISO2 = "de"
    ),
    Language(
      id = 3,
      active = true,
      occurencesOfLanguage = 72,
      language = "Italian",
      languageLocalized = "Italiano",
      languageCode = "ita",
      languageCodeISO2 = "it"
    ),
    Language(
      id = 4,
      active = true,
      occurencesOfLanguage = 93,
      language = "French",
      languageLocalized = "Français",
      languageCode = "fra",
      languageCodeISO2 = "fr"
    ),
    Language(
      id = 5,
      active = true,
      occurencesOfLanguage = 104,
      language = "Spanish",
      languageLocalized = "Español",
      languageCode = "spa",
      languageCodeISO2 = "es"
    )
  )
  LanguageScreen(
    viewModelState = MutableLiveData<State>().apply { value = Content(languages) },
    selectLanguageItem = {},
    filterText = {},
    saveLanguages = {},
    onNavigationClick = {}
  )
}
