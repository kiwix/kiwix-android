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
import androidx.activity.compose.BackHandler
import androidx.annotation.VisibleForTesting
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.CollectSideEffectWithActivity
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
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
@Suppress("LongMethod")
@Composable
internal fun LanguageScreenRoute(navigateBack: () -> Unit) {
  val languageViewModel: LanguageViewModel = hiltViewModel()
  languageViewModel.setOnFinishCallback(navigateBack)
  val state by languageViewModel.state.collectAsStateWithLifecycle()

  languageViewModel.effects.CollectSideEffectWithActivity { effect, activity ->
    effect.invokeWith(activity)
  }

  var searchText by rememberSaveable { mutableStateOf("") }
  var isSaving by remember { mutableStateOf(false) }
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  fun resetSearchState() {
    keyboardController?.hide()
    focusManager.clearFocus(force = true)
    searchText = ""
    languageViewModel.actions.tryEmit(Action.Filter(searchText))
  }

  val saveAndNavigateBack: () -> Unit = {
    if (!isSaving) {
      isSaving = true
      keyboardController?.hide()
      focusManager.clearFocus(force = true)
      if (state is Content) {
        languageViewModel.actions.tryEmit(Action.Save)
      } else if (state !== State.Saving) {
        navigateBack()
      }
    }
  }

  val handleBack: () -> Unit = {
    if (searchText.isNotEmpty()) {
      resetSearchState()
    } else {
      saveAndNavigateBack()
    }
  }

  BackHandler(enabled = !isSaving && state !== State.Saving, onBack = handleBack)

  KiwixTheme {
    LanguageScreen(
      searchText = searchText,
      state = state,
      onClearClick = {
        searchText = ""
        languageViewModel.actions.tryEmit(Action.Filter(""))
      },
      onSearchTextChange = {
        searchText = it
        languageViewModel.actions.tryEmit(Action.Filter(it.trim()))
      },
      selectLanguageItem = { languageItem ->
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        languageViewModel.actions.tryEmit(Action.Select(languageItem))
      },
      onMoveUp = { languageItem ->
        languageViewModel.actions.tryEmit(Action.MoveUp(languageItem))
      },
      onMoveDown = { languageItem ->
        languageViewModel.actions.tryEmit(Action.MoveDown(languageItem))
      },
      navigationIcon = {
        NavigationIcon(
          iconItem = IconItem.Vector(Icons.AutoMirrored.Filled.ArrowBack),
          onClick = saveAndNavigateBack
        )
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ComposableLambdaParameterNaming")
@Suppress("LongParameterList")
@VisibleForTesting
@Composable
internal fun LanguageScreen(
  searchText: String = "",
  state: State,
  selectLanguageItem: (item: LanguageListItem.LanguageItem) -> Unit,
  onMoveUp: (item: LanguageListItem.LanguageItem) -> Unit = {},
  onMoveDown: (item: LanguageListItem.LanguageItem) -> Unit = {},
  onClearClick: () -> Unit = {},
  onSearchTextChange: (String) -> Unit = {},
  navigationIcon: @Composable () -> Unit
) {
  val listState: LazyListState = rememberLazyListState()
  val context = LocalContext.current

  Scaffold(topBar = {
    KiwixAppBar(
      title = stringResource(R.string.select_language),
      navigationIcon = navigationIcon
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
            searchText = searchText,
            onSearchTextChange = onSearchTextChange,
            onClearClick = onClearClick,
            selectLanguageItem = selectLanguageItem,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown
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
  val lifecycleOwner = LocalLifecycleOwner.current
  val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
      ContentLoadingProgressBar()
    }
  }
}
