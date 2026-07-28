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

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.CollectSideEffectWithActivity
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.search.viewmodel.Action
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchViewModel
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost

const val NAV_ARG_SEARCH_STRING = "searchString"

@Composable
fun SearchScreenRoute(
  viewModelFactory: ViewModelProvider.Factory,
  arguments: Bundle?,
  coreMainActivity: CoreMainActivity
) {
  val alertDialogShower = remember { AlertDialogShower() }
  val viewModel: SearchViewModel = viewModel(factory = viewModelFactory)
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  // Voice Intent.
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

  // Handles SideEffects
  viewModel.effects.CollectSideEffectWithActivity { effect, activity ->
    effect.invokeWith(activity)
  }

  // Search Results
  LaunchedEffect(Unit) {
    viewModel.setAlertDialogShower(alertDialogShower)
    viewModel.actions.tryEmit(
      Action.CreatedWithArguments(Bundle(arguments))
    )
  }

  KiwixTheme {
    SearchScreen(
      uiState,
      viewModel,
      buildActionMenuItems(viewModel),
      {
        NavigationIcon(
          onClick = {
            viewModel.closeKeyboard()
            coreMainActivity.onBackPressedDispatcher.onBackPressed()
          }
        )
      }
    )
    DialogHost(alertDialogShower)
  }
}

private fun buildActionMenuItems(viewModel: SearchViewModel): List<ActionMenuItem> {
  return listOf(
    ActionMenuItem(
      contentDescription = R.string.search_label,
      icon = IconItem.Drawable(R.drawable.ic_mic_black_24dp),
      testingTag = VOICE_SEARCH_TESTING_TAG,
      isEnabled = true,
      onClick = {
        viewModel.actions.tryEmit(Action.ReceivedPromptForSpeechInput)
      }
    )
  )
}
