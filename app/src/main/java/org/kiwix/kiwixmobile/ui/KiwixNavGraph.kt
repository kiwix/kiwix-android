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

package org.kiwix.kiwixmobile.ui

import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import org.kiwix.kiwixmobile.core.ViewModelFactory
import org.kiwix.kiwixmobile.core.help.HelpScreenRoute
import org.kiwix.kiwixmobile.core.main.BOOKMARK_SCREEN
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.DOWNLOAD_SCREEN
import org.kiwix.kiwixmobile.core.main.HELP_SCREEN
import org.kiwix.kiwixmobile.core.main.HISTORY_SCREEN
import org.kiwix.kiwixmobile.core.main.INTRO_SCREEN
import org.kiwix.kiwixmobile.core.main.LANGUAGE_SCREEN
import org.kiwix.kiwixmobile.core.main.LOCAL_FILE_TRANSFER_SCREEN
import org.kiwix.kiwixmobile.core.main.LOCAL_LIBRARY_SCREEN
import org.kiwix.kiwixmobile.core.main.NOTES_SCREEN
import org.kiwix.kiwixmobile.core.main.READER_FRAGMENT
import org.kiwix.kiwixmobile.core.main.SEARCH_SCREEN
import org.kiwix.kiwixmobile.core.main.SETTINGS_SCREEN
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.main.ZIM_HOST_NAV_DEEP_LINK
import org.kiwix.kiwixmobile.core.main.ZIM_HOST_SCREEN
import org.kiwix.kiwixmobile.core.main.note.AddNoteViewModel
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderScreen
import org.kiwix.kiwixmobile.core.page.bookmark.BookmarkScreenRoute
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.BookmarkViewModel
import org.kiwix.kiwixmobile.core.page.history.HistoryScreenRoute
import org.kiwix.kiwixmobile.core.page.history.viewmodel.HistoryViewModel
import org.kiwix.kiwixmobile.core.page.notes.NotesScreenRoute
import org.kiwix.kiwixmobile.core.page.notes.viewmodel.NotesViewModel
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.search.NAV_ARG_SEARCH_STRING
import org.kiwix.kiwixmobile.core.search.SearchScreenRoute
import org.kiwix.kiwixmobile.core.settings.SettingsScreenRoute
import org.kiwix.kiwixmobile.core.utils.EXTRA_IS_WIDGET_VOICE
import org.kiwix.kiwixmobile.core.utils.TAG_FROM_TAB_SWITCHER
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.help.KiwixHelpViewModel
import org.kiwix.kiwixmobile.intro.IntroScreenRoute
import org.kiwix.kiwixmobile.language.LanguageScreenRoute
import org.kiwix.kiwixmobile.localFileTransfer.LocalFileTransferScreenRoute
import org.kiwix.kiwixmobile.localFileTransfer.LocalFileTransferViewModel
import org.kiwix.kiwixmobile.localFileTransfer.URIS_KEY
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryRoute
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryRoute
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryViewModel
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel
import org.kiwix.kiwixmobile.nav.destination.reader.KiwixReaderViewModel
import org.kiwix.kiwixmobile.settings.KiwixSettingsViewModel
import org.kiwix.kiwixmobile.webserver.ZimHostRoute
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel

@Suppress("LongMethod")
@Composable
fun KiwixNavGraph(
  navController: NavHostController,
  startDestination: String,
  modifier: Modifier = Modifier,
  viewModelFactory: ViewModelProvider.Factory,
  alertDialogShower: AlertDialogShower,
  snackBarHostState: SnackbarHostState
) {
  NavHost(
    navController = navController,
    startDestination = startDestination,
    modifier = modifier
  ) {
    composable(route = KiwixDestination.Reader.route) { backStackEntry ->
      val activity = LocalActivity.current as CoreMainActivity
      val addNoteViewModel: AddNoteViewModel = viewModel(factory = viewModelFactory)
      val kiwixReaderViewModel: KiwixReaderViewModel = viewModel(factory = viewModelFactory)
      LaunchedEffect(Unit) {
        kiwixReaderViewModel.initialize(activity, alertDialogShower)
      }
      CoreReaderScreen(
        viewModel = kiwixReaderViewModel,
        addNoteViewModel = addNoteViewModel,
        navHostController = navController,
        alertDialogShower = alertDialogShower,
        activity = activity,
      )
    }
    composable(
      route = KiwixDestination.Library.route,
      arguments = listOf(
        navArgument(ZIM_FILE_URI_KEY) {
          type = NavType.StringType
          defaultValue = ""
        }
      )
    ) { backStackEntry ->
      val activity = LocalActivity.current as KiwixMainActivity
      val validateZimViewModel: ValidateZimViewModel = viewModel(factory = viewModelFactory)
      val localLibraryViewModel: LocalLibraryViewModel = viewModel(factory = viewModelFactory)
      LaunchedEffect(Unit) {
        localLibraryViewModel.apply {
          initialize(
            activity.getStorageDeviceList(),
            validateZimViewModel,
            alertDialogShower,
            snackBarHostState
          )
        }
      }
      val zimFileUri = backStackEntry.arguments?.getString(ZIM_FILE_URI_KEY).orEmpty()
      LocalLibraryRoute(
        localLibraryViewModel = localLibraryViewModel,
        navController = navController,
        zimFileUriArg = zimFileUri,
        snackBarHostState = snackBarHostState
      )
    }

    composable(KiwixDestination.Downloads.route) {
      val activity = LocalActivity.current as KiwixMainActivity
      val onlineLibraryViewModel: OnlineLibraryViewModel =
        viewModel(viewModelStoreOwner = activity, factory = viewModelFactory)
      val categoryViewModel: CategoryViewModel =
        viewModel(viewModelStoreOwner = activity, factory = viewModelFactory)
      OnlineLibraryRoute(
        onlineLibraryViewModel = onlineLibraryViewModel,
        categoryViewModel = categoryViewModel,
        alertDialogShower = alertDialogShower,
        navController = navController,
        activity = activity
      )
    }
    composable(KiwixDestination.Bookmarks.route) {
      val bookmarkViewModel: BookmarkViewModel = viewModel(factory = viewModelFactory)
      BookmarkScreenRoute(
        navigateBack = navController::popBackStack,
        viewModel = bookmarkViewModel,
        alertDialogShower = alertDialogShower
      )
    }
    composable(KiwixDestination.Notes.route) {
      val notesViewModel: NotesViewModel = viewModel(factory = viewModelFactory)
      NotesScreenRoute(
        navigateBack = navController::popBackStack,
        notesViewModel = notesViewModel,
        alertDialogShower = alertDialogShower
      )
    }
    composable(KiwixDestination.Intro.route) {
      IntroScreenRoute(
        viewModelFactory = viewModelFactory as ViewModelFactory,
        navigateToLibrary = {
          val navOptions = NavOptions.Builder()
            .setPopUpTo(KiwixDestination.Intro.route, inclusive = true)
            .build()
          navController.navigate(KiwixDestination.Library.route, navOptions)
        }
      )
    }
    composable(KiwixDestination.History.route) {
      val historyViewModel: HistoryViewModel = viewModel(factory = viewModelFactory)
      HistoryScreenRoute(
        navigateBack = navController::popBackStack,
        viewModel = historyViewModel,
        alertDialogShower = alertDialogShower
      )
    }
    composable(KiwixDestination.Language.route) {
      LanguageScreenRoute(
        viewModelFactory = viewModelFactory,
        navigateBack = navController::popBackStack
      )
    }
    composable(
      KiwixDestination.ZimHost.route,
      deepLinks = listOf(navDeepLink { uriPattern = ZIM_HOST_NAV_DEEP_LINK })
    ) {
      val activity = LocalActivity.current as KiwixMainActivity
      val viewModel: ZimHostViewModel = viewModel(factory = viewModelFactory)
      ZimHostRoute(viewModel, alertDialogShower, activity)
    }
    composable(KiwixDestination.Help.route) {
      val kiwixHelpViewModel: KiwixHelpViewModel = viewModel(factory = viewModelFactory)
      HelpScreenRoute(
        navigateBack = navController::popBackStack,
        helpViewModel = kiwixHelpViewModel
      )
    }
    composable(KiwixDestination.Settings.route) {
      val kiwixSettingsViewModel: KiwixSettingsViewModel = viewModel(factory = viewModelFactory)
      kiwixSettingsViewModel.setAlertDialog(alertDialogShower)
      SettingsScreenRoute(
        kiwixSettingsViewModel,
        navController::popBackStack
      )
    }
    composable(
      route = KiwixDestination.Search.route,
      arguments = listOf(
        navArgument(NAV_ARG_SEARCH_STRING) {
          type = NavType.StringType
          defaultValue = ""
        },
        navArgument(TAG_FROM_TAB_SWITCHER) {
          type = NavType.BoolType
          defaultValue = false
        },
        navArgument(EXTRA_IS_WIDGET_VOICE) {
          type = NavType.BoolType
          defaultValue = false
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

    composable(
      route = KiwixDestination.LocalFileTransfer.route,
      arguments = listOf(
        navArgument(URIS_KEY) {
          type = NavType.StringType
          nullable = true
          defaultValue = null
        }
      )
    ) { backStackEntry ->
      val uris: List<Uri> = backStackEntry.arguments
        ?.getString(URIS_KEY)
        ?.takeIf { it != "null" }
        ?.split(",")
        ?.map { Uri.decode(it).toUri() }
        .orEmpty()

      val viewModel: LocalFileTransferViewModel = viewModel(factory = viewModelFactory)
      viewModel.initialize(uris, alertDialogShower)

      LocalFileTransferScreenRoute(
        navigateBack = navController::popBackStack,
        viewModel = viewModel,
        alertDialogShower = alertDialogShower
      )
    }
  }
}

sealed class KiwixDestination(val route: String) {
  object Reader : KiwixDestination(READER_FRAGMENT)

  object Library :
    KiwixDestination("$LOCAL_LIBRARY_SCREEN?$ZIM_FILE_URI_KEY={$ZIM_FILE_URI_KEY}") {
    fun createRoute(zimFileUri: String = "") =
      "$LOCAL_LIBRARY_SCREEN?$ZIM_FILE_URI_KEY=${Uri.encode(zimFileUri)}"
  }

  object Downloads : KiwixDestination(DOWNLOAD_SCREEN)
  object Bookmarks : KiwixDestination(BOOKMARK_SCREEN)
  object Notes : KiwixDestination(NOTES_SCREEN)
  object Intro : KiwixDestination(INTRO_SCREEN)
  object History : KiwixDestination(HISTORY_SCREEN)
  object Language : KiwixDestination(LANGUAGE_SCREEN)
  object ZimHost : KiwixDestination(ZIM_HOST_SCREEN)
  object Help : KiwixDestination(HELP_SCREEN)
  object Settings : KiwixDestination(SETTINGS_SCREEN)
  object Search : KiwixDestination(
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

  object LocalFileTransfer : KiwixDestination("$LOCAL_FILE_TRANSFER_SCREEN?$URIS_KEY={$URIS_KEY}") {
    fun createRoute(uris: String? = null): String {
      return if (uris != null) {
        "$LOCAL_FILE_TRANSFER_SCREEN?$URIS_KEY=${Uri.encode(uris)}"
      } else {
        "$LOCAL_FILE_TRANSFER_SCREEN?$URIS_KEY=null"
      }
    }
  }
}

fun List<Uri>.toUriParam(): String =
  joinToString(",") { Uri.encode("$it") }
