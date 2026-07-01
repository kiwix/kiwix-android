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

package org.kiwix.kiwixmobile.custom.main

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.kiwix.kiwixmobile.core.help.HelpScreenRoute
import org.kiwix.kiwixmobile.core.main.BOOKMARK_SCREEN
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.DOWNLOAD_SCREEN
import org.kiwix.kiwixmobile.core.main.HELP_SCREEN
import org.kiwix.kiwixmobile.core.main.HISTORY_SCREEN
import org.kiwix.kiwixmobile.core.main.NOTES_SCREEN
import org.kiwix.kiwixmobile.core.main.READER_SCREEN
import org.kiwix.kiwixmobile.core.main.SEARCH_SCREEN
import org.kiwix.kiwixmobile.core.main.SETTINGS_SCREEN
import org.kiwix.kiwixmobile.core.main.note.AddNoteViewModel
import org.kiwix.kiwixmobile.core.main.reader.ReaderScreenRoute
import org.kiwix.kiwixmobile.core.page.bookmark.BookmarkScreenRoute
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.BookmarkViewModel
import org.kiwix.kiwixmobile.core.page.history.HistoryScreenRoute
import org.kiwix.kiwixmobile.core.page.history.viewmodel.HistoryViewModel
import org.kiwix.kiwixmobile.core.page.notes.NotesScreenRoute
import org.kiwix.kiwixmobile.core.page.notes.viewmodel.NotesViewModel
import org.kiwix.kiwixmobile.core.search.NAV_ARG_SEARCH_STRING
import org.kiwix.kiwixmobile.core.search.SearchScreenRoute
import org.kiwix.kiwixmobile.core.settings.SettingsScreenRoute
import org.kiwix.kiwixmobile.core.utils.EXTRA_IS_WIDGET_VOICE
import org.kiwix.kiwixmobile.core.utils.TAG_FROM_TAB_SWITCHER
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.custom.download.BrandedDownloadRoute
import org.kiwix.kiwixmobile.custom.download.BrandedDownloadViewModel
import org.kiwix.kiwixmobile.custom.help.BrandedHelpViewModel
import org.kiwix.kiwixmobile.custom.settings.BrandedSettingsViewModel

@Suppress("LongMethod")
@Composable
fun BrandedNavGraph(
  navController: NavHostController,
  modifier: Modifier = Modifier,
  viewModelFactory: ViewModelProvider.Factory,
  alertDialogShower: AlertDialogShower
) {
  NavHost(
    navController = navController,
    startDestination = CustomDestination.Reader.route,
    modifier = modifier
  ) {
    composable(route = CustomDestination.Reader.route) { backStackEntry ->
      val activity = LocalActivity.current as CoreMainActivity
      val addNoteViewModel: AddNoteViewModel = viewModel(factory = viewModelFactory)
      val brandedReaderViewModel: BrandedReaderViewModel = viewModel(factory = viewModelFactory)
      ReaderScreenRoute(
        viewModel = brandedReaderViewModel,
        addNoteViewModel = addNoteViewModel,
        navHostController = navController,
        alertDialogShower = alertDialogShower,
        activity = activity,
      )
    }
    composable(CustomDestination.History.route) {
      val historyViewModel: HistoryViewModel = viewModel(factory = viewModelFactory)
      HistoryScreenRoute(
        navigateBack = navController::popBackStack,
        viewModel = historyViewModel,
        alertDialogShower = alertDialogShower
      )
    }
    composable(CustomDestination.Notes.route) {
      val notesViewModel: NotesViewModel = viewModel(factory = viewModelFactory)
      NotesScreenRoute(
        navigateBack = navController::popBackStack,
        notesViewModel = notesViewModel,
        alertDialogShower = alertDialogShower
      )
    }
    composable(CustomDestination.Bookmarks.route) {
      val bookmarkViewModel: BookmarkViewModel = viewModel(factory = viewModelFactory)
      BookmarkScreenRoute(
        navigateBack = navController::popBackStack,
        viewModel = bookmarkViewModel,
        alertDialogShower = alertDialogShower
      )
    }
    composable(CustomDestination.Help.route) {
      val brandedHelpViewModel: BrandedHelpViewModel = viewModel(factory = viewModelFactory)
      HelpScreenRoute(
        navigateBack = navController::popBackStack,
        helpViewModel = brandedHelpViewModel
      )
    }
    composable(CustomDestination.Settings.route) {
      val brandedSettingsViewModel: BrandedSettingsViewModel = viewModel(factory = viewModelFactory)
      brandedSettingsViewModel.setAlertDialog(alertDialogShower)
      SettingsScreenRoute(
        brandedSettingsViewModel,
        navController::popBackStack
      )
    }
    composable(CustomDestination.Downloads.route) {
      val brandedDownloadViewModel: BrandedDownloadViewModel = viewModel(factory = viewModelFactory)
      BrandedDownloadRoute(
        brandedDownloadViewModel
      )
    }
    composable(
      route = CustomDestination.Search.route,
      arguments = listOf(
        navArgument(TAG_FROM_TAB_SWITCHER) {
          type = NavType.BoolType
          defaultValue = false
        },
        navArgument(EXTRA_IS_WIDGET_VOICE) {
          type = NavType.BoolType
          defaultValue = false
        },
        navArgument(NAV_ARG_SEARCH_STRING) {
          type = NavType.StringType
          defaultValue = ""
        }
      )
    ) { backStackEntry ->

      val context = LocalActivity.current
      val coreMainActivity = context as CoreMainActivity

      SearchScreenRoute(
        viewModelFactory = viewModelFactory,
        dialogShower = alertDialogShower,
        arguments = backStackEntry.arguments,
        coreMainActivity = coreMainActivity
      )
    }
  }
}

sealed class CustomDestination(val route: String) {
  object Reader : CustomDestination(READER_SCREEN)

  object History : CustomDestination(HISTORY_SCREEN)
  object Notes : CustomDestination(NOTES_SCREEN)
  object Bookmarks : CustomDestination(BOOKMARK_SCREEN)
  object Help : CustomDestination(HELP_SCREEN)
  object Settings : CustomDestination(SETTINGS_SCREEN)
  object Downloads : CustomDestination(DOWNLOAD_SCREEN)
  object Search : CustomDestination(
    SEARCH_SCREEN +
      "?$NAV_ARG_SEARCH_STRING={$NAV_ARG_SEARCH_STRING}" +
      "&$TAG_FROM_TAB_SWITCHER={$TAG_FROM_TAB_SWITCHER}" +
      "&$EXTRA_IS_WIDGET_VOICE={$EXTRA_IS_WIDGET_VOICE}"
  ) {
    fun createRoute(
      searchString: String = "",
      isOpenedFromTabView: Boolean = false,
      isVoice: Boolean = false
    ): String {
      return SEARCH_SCREEN +
        "?$NAV_ARG_SEARCH_STRING=$searchString" +
        "&$TAG_FROM_TAB_SWITCHER=$isOpenedFromTabView" +
        "&$EXTRA_IS_WIDGET_VOICE=$isVoice"
    }
  }
}
