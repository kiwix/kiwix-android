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
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.kiwix.kiwixmobile.core.main.BOOKMARK_FRAGMENT
import org.kiwix.kiwixmobile.core.main.DOWNLOAD_FRAGMENT
import org.kiwix.kiwixmobile.core.main.FIND_IN_PAGE_SEARCH_STRING
import org.kiwix.kiwixmobile.core.main.HELP_FRAGMENT
import org.kiwix.kiwixmobile.core.main.HISTORY_FRAGMENT
import org.kiwix.kiwixmobile.core.main.INTRO_FRAGMENT
import org.kiwix.kiwixmobile.core.main.LANGUAGE_FRAGMENT
import org.kiwix.kiwixmobile.core.main.LOCAL_FILE_TRANSFER_FRAGMENT
import org.kiwix.kiwixmobile.core.main.LOCAL_LIBRARY_FRAGMENT
import org.kiwix.kiwixmobile.core.main.NOTES_FRAGMENT
import org.kiwix.kiwixmobile.core.main.PAGE_URL_KEY
import org.kiwix.kiwixmobile.core.main.READER_FRAGMENT
import org.kiwix.kiwixmobile.core.main.SEARCH_FRAGMENT
import org.kiwix.kiwixmobile.core.main.SETTINGS_FRAGMENT
import org.kiwix.kiwixmobile.core.main.SHOULD_OPEN_IN_NEW_TAB
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.main.ZIM_HOST_FRAGMENT
import org.kiwix.kiwixmobile.core.main.reader.SEARCH_ITEM_TITLE_KEY
import org.kiwix.kiwixmobile.core.page.bookmark.BookmarksFragment
import org.kiwix.kiwixmobile.core.page.history.HistoryFragment
import org.kiwix.kiwixmobile.core.page.notes.NotesFragment
import org.kiwix.kiwixmobile.core.search.NAV_ARG_SEARCH_STRING
import org.kiwix.kiwixmobile.core.search.SearchFragment
import org.kiwix.kiwixmobile.core.utils.EXTRA_IS_WIDGET_VOICE
import org.kiwix.kiwixmobile.core.utils.TAG_FROM_TAB_SWITCHER
import org.kiwix.kiwixmobile.help.KiwixHelpFragment
import org.kiwix.kiwixmobile.intro.IntroFragment
import org.kiwix.kiwixmobile.language.LanguageFragment
import org.kiwix.kiwixmobile.localFileTransfer.LocalFileTransferFragment
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryFragment
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryFragment
import org.kiwix.kiwixmobile.nav.destination.reader.KiwixReaderFragment
import org.kiwix.kiwixmobile.settings.KiwixSettingsFragment
import org.kiwix.kiwixmobile.webserver.ZimHostFragment

@Composable
fun KiwixNavGraph(
  navController: NavHostController,
  modifier: Modifier = Modifier
) {
  NavHost(
    navController = navController,
    startDestination = KiwixDestination.Reader.route,
    modifier = modifier
  ) {
    composable(
      route = KiwixDestination.Reader.route,
      arguments = listOf(
        navArgument(ZIM_FILE_URI_KEY) { type = NavType.StringType; defaultValue = "" },
        navArgument(FIND_IN_PAGE_SEARCH_STRING) { type = NavType.StringType; defaultValue = "" },
        navArgument(PAGE_URL_KEY) { type = NavType.StringType; defaultValue = "" },
        navArgument(SHOULD_OPEN_IN_NEW_TAB) { type = NavType.BoolType; defaultValue = false },
        navArgument(SEARCH_ITEM_TITLE_KEY) { type = NavType.StringType; defaultValue = "" }
      )
    ) { backStackEntry ->
      val zimFileUri = backStackEntry.arguments?.getString(ZIM_FILE_URI_KEY).orEmpty()
      val findInPageSearchString =
        backStackEntry.arguments?.getString(FIND_IN_PAGE_SEARCH_STRING).orEmpty()
      val pageUrl = backStackEntry.arguments?.getString(PAGE_URL_KEY).orEmpty()
      val shouldOpenInNewTab = backStackEntry.arguments?.getBoolean(SHOULD_OPEN_IN_NEW_TAB) ?: false
      val searchItemTitle = backStackEntry.arguments?.getString(SEARCH_ITEM_TITLE_KEY).orEmpty()

      FragmentContainer {
        KiwixReaderFragment().apply {
          arguments = Bundle().apply {
            putString(ZIM_FILE_URI_KEY, zimFileUri)
            putString(FIND_IN_PAGE_SEARCH_STRING, findInPageSearchString)
            putString(PAGE_URL_KEY, pageUrl)
            putBoolean(SHOULD_OPEN_IN_NEW_TAB, shouldOpenInNewTab)
            putString(SEARCH_ITEM_TITLE_KEY, searchItemTitle)
          }
        }
      }
    }
    composable(
      route = KiwixDestination.Library.route,
      arguments = listOf(
        navArgument(ZIM_FILE_URI_KEY) { type = NavType.StringType; defaultValue = "" }
      )
    ) { backStackEntry ->
      val zimFileUri = backStackEntry.arguments?.getString(ZIM_FILE_URI_KEY).orEmpty()

      FragmentContainer {
        LocalLibraryFragment().apply {
          arguments = Bundle().apply {
            putString(ZIM_FILE_URI_KEY, zimFileUri)
          }
        }
      }
    }
    composable(KiwixDestination.Downloads.route) {
      FragmentContainer {
        OnlineLibraryFragment()
      }
    }
    composable(KiwixDestination.Bookmarks.route) {
      FragmentContainer {
        BookmarksFragment()
      }
    }
    composable(KiwixDestination.Notes.route) {
      FragmentContainer {
        NotesFragment()
      }
    }
    composable(KiwixDestination.Intro.route) {
      FragmentContainer {
        IntroFragment()
      }
    }
    composable(KiwixDestination.History.route) {
      FragmentContainer {
        HistoryFragment()
      }
    }
    composable(KiwixDestination.Language.route) {
      FragmentContainer {
        LanguageFragment()
      }
    }
    composable(KiwixDestination.ZimHost.route) {
      FragmentContainer {
        ZimHostFragment()
      }
    }
    composable(KiwixDestination.Help.route) {
      FragmentContainer {
        KiwixHelpFragment()
      }
    }
    composable(KiwixDestination.Settings.route) {
      FragmentContainer {
        KiwixSettingsFragment()
      }
    }
    composable(
      route = KiwixDestination.Search.route,
      arguments = listOf(
        navArgument(NAV_ARG_SEARCH_STRING) { type = NavType.StringType; defaultValue = "" },
        navArgument(TAG_FROM_TAB_SWITCHER) { type = NavType.BoolType; defaultValue = false },
        navArgument(EXTRA_IS_WIDGET_VOICE) { type = NavType.BoolType; defaultValue = false }
      )
    ) { backStackEntry ->
      val searchString = backStackEntry.arguments?.getString(NAV_ARG_SEARCH_STRING).orEmpty()
      val isOpenedFromTabSwitcher =
        backStackEntry.arguments?.getBoolean(TAG_FROM_TAB_SWITCHER) ?: false
      val isVoice = backStackEntry.arguments?.getBoolean(EXTRA_IS_WIDGET_VOICE) ?: false
      FragmentContainer {
        SearchFragment().apply {
          arguments = Bundle().apply {
            putString(NAV_ARG_SEARCH_STRING, searchString)
            putBoolean(TAG_FROM_TAB_SWITCHER, isOpenedFromTabSwitcher)
            putBoolean(EXTRA_IS_WIDGET_VOICE, isVoice)
          }
        }
      }
    }
    composable(
      route = KiwixDestination.LocalFileTransfer.route,
      arguments = listOf(
        navArgument("uris") {
          type = NavType.StringType
          nullable = true
          defaultValue = null
        }
      )
    ) { backStackEntry ->
      val urisParam = backStackEntry.arguments?.getString("uris")
      val uris: List<Uri>? =
        urisParam?.takeIf { it != "null" }?.split(",")?.map {
          Uri.decode(it).toUri()
        }

      FragmentContainer {
        LocalFileTransferFragment().apply {
          arguments = Bundle().apply {
            putParcelableArray(
              "uris",
              uris?.toTypedArray()
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FragmentContainer(
  fragmentProvider: () -> Fragment
) {
  val context = LocalContext.current
  val fragmentManager = remember {
    (context as AppCompatActivity).supportFragmentManager
  }
  val viewId = remember { View.generateViewId() }

  AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { ctx ->
      FragmentContainerView(ctx).apply {
        id = viewId
        doOnAttach {
          fragmentManager.commit {
            if (fragmentManager.findFragmentById(viewId) == null) {
              add(viewId, fragmentProvider())
            }
          }
        }
      }
    }
  )
}

sealed class KiwixDestination(val route: String) {
  object Reader : KiwixDestination(
    READER_FRAGMENT +
      "?$ZIM_FILE_URI_KEY={$ZIM_FILE_URI_KEY}" +
      "&$FIND_IN_PAGE_SEARCH_STRING={$FIND_IN_PAGE_SEARCH_STRING}" +
      "&$PAGE_URL_KEY={$PAGE_URL_KEY}" +
      "&$SHOULD_OPEN_IN_NEW_TAB={$SHOULD_OPEN_IN_NEW_TAB}" +
      "&$SEARCH_ITEM_TITLE_KEY={$SEARCH_ITEM_TITLE_KEY}"
  ) {
    fun createRoute(
      zimFileUri: String = "",
      findInPageSearchString: String = "",
      pageUrl: String = "",
      shouldOpenInNewTab: Boolean = false,
      searchItemTitle: String = ""
    ): String {
      return READER_FRAGMENT +
        "?$ZIM_FILE_URI_KEY=${Uri.encode(zimFileUri)}" +
        "&$FIND_IN_PAGE_SEARCH_STRING=${Uri.encode(findInPageSearchString)}" +
        "&$PAGE_URL_KEY=${Uri.encode(pageUrl)}" +
        "&$SHOULD_OPEN_IN_NEW_TAB=$shouldOpenInNewTab" +
        "&$SEARCH_ITEM_TITLE_KEY=${Uri.encode(searchItemTitle)}"
    }
  }

  object Library :
    KiwixDestination("$LOCAL_LIBRARY_FRAGMENT?$ZIM_FILE_URI_KEY={$ZIM_FILE_URI_KEY}") {
    fun createRoute(zimFileUri: String = "") =
      "$LOCAL_LIBRARY_FRAGMENT?$ZIM_FILE_URI_KEY=${Uri.encode(zimFileUri)}"
  }

  object Downloads : KiwixDestination(DOWNLOAD_FRAGMENT)
  object Bookmarks : KiwixDestination(BOOKMARK_FRAGMENT)
  object Notes : KiwixDestination(NOTES_FRAGMENT)
  object Intro : KiwixDestination(INTRO_FRAGMENT)
  object History : KiwixDestination(HISTORY_FRAGMENT)
  object Language : KiwixDestination(LANGUAGE_FRAGMENT)
  object ZimHost : KiwixDestination(ZIM_HOST_FRAGMENT)
  object Help : KiwixDestination(HELP_FRAGMENT)
  object Settings : KiwixDestination(SETTINGS_FRAGMENT)
  object Search : KiwixDestination(
    SEARCH_FRAGMENT +
      "?$NAV_ARG_SEARCH_STRING={$NAV_ARG_SEARCH_STRING}" +
      "&$TAG_FROM_TAB_SWITCHER={$TAG_FROM_TAB_SWITCHER}" +
      "&$EXTRA_IS_WIDGET_VOICE={$EXTRA_IS_WIDGET_VOICE}"
  ) {
    fun createRoute(
      searchString: String = "",
      isOpenedFromTabView: Boolean = false,
      isVoice: Boolean = false
    ): String {
      return SEARCH_FRAGMENT +
        "?$NAV_ARG_SEARCH_STRING=$searchString" +
        "&$TAG_FROM_TAB_SWITCHER=$isOpenedFromTabView" +
        "&$EXTRA_IS_WIDGET_VOICE=$isVoice"
    }
  }

  object LocalFileTransfer : KiwixDestination("$LOCAL_FILE_TRANSFER_FRAGMENT?uris={uris}") {
    fun createRoute(uris: String? = null): String {
      return if (uris != null)
        "$LOCAL_FILE_TRANSFER_FRAGMENT?uris=${Uri.encode(uris)}"
      else
        "$LOCAL_FILE_TRANSFER_FRAGMENT?uris=null"
    }
  }
}

fun List<Uri>.toUriParam(): String =
  joinToString(",") { Uri.encode(it.toString()) }
