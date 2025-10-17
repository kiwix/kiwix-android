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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.BOOKMARK_FRAGMENT
import org.kiwix.kiwixmobile.core.main.DOWNLOAD_FRAGMENT
import org.kiwix.kiwixmobile.core.main.HELP_FRAGMENT
import org.kiwix.kiwixmobile.core.main.HISTORY_FRAGMENT
import org.kiwix.kiwixmobile.core.main.NOTES_FRAGMENT
import org.kiwix.kiwixmobile.core.main.READER_FRAGMENT
import org.kiwix.kiwixmobile.core.main.SEARCH_FRAGMENT
import org.kiwix.kiwixmobile.core.main.SETTINGS_FRAGMENT
import org.kiwix.kiwixmobile.core.page.bookmark.BookmarksFragment
import org.kiwix.kiwixmobile.core.page.history.HistoryFragment
import org.kiwix.kiwixmobile.core.page.notes.NotesFragment
import org.kiwix.kiwixmobile.core.search.NAV_ARG_SEARCH_STRING
import org.kiwix.kiwixmobile.core.search.SearchFragment
import org.kiwix.kiwixmobile.core.utils.EXTRA_IS_WIDGET_VOICE
import org.kiwix.kiwixmobile.core.utils.TAG_FROM_TAB_SWITCHER
import org.kiwix.kiwixmobile.custom.download.CustomDownloadFragment
import org.kiwix.kiwixmobile.custom.help.CustomHelpFragment
import org.kiwix.kiwixmobile.custom.settings.CustomSettingsFragment

@Suppress("LongMethod")
@Composable
fun CustomNavGraph(
  navController: NavHostController,
  modifier: Modifier = Modifier
) {
  NavHost(
    navController = navController,
    startDestination = CustomDestination.Reader.route,
    modifier = modifier
  ) {
    composable(route = CustomDestination.Reader.route) { backStackEntry ->
      FragmentContainer(R.id.readerFragmentContainer) {
        CustomReaderFragment()
      }
    }
    composable(CustomDestination.History.route) {
      FragmentContainer(R.id.historyFragmentContainer) {
        HistoryFragment()
      }
    }
    composable(CustomDestination.Notes.route) {
      FragmentContainer(R.id.notesFragmentContainer) {
        NotesFragment()
      }
    }
    composable(CustomDestination.Bookmarks.route) {
      FragmentContainer(R.id.bookmarksFragmentContainer) {
        BookmarksFragment()
      }
    }
    composable(CustomDestination.Help.route) {
      FragmentContainer(R.id.helpFragmentContainer) {
        CustomHelpFragment()
      }
    }
    composable(CustomDestination.Settings.route) {
      FragmentContainer(R.id.settingsFragmentContainer) {
        CustomSettingsFragment()
      }
    }
    composable(CustomDestination.Downloads.route) {
      FragmentContainer(R.id.downloadFragmentContainer) {
        CustomDownloadFragment()
      }
    }
    composable(
      route = CustomDestination.Search.route,
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
      val searchString = backStackEntry.arguments?.getString(NAV_ARG_SEARCH_STRING).orEmpty()
      val isOpenedFromTabSwitcher =
        backStackEntry.arguments?.getBoolean(TAG_FROM_TAB_SWITCHER) ?: false
      val isVoice = backStackEntry.arguments?.getBoolean(EXTRA_IS_WIDGET_VOICE) ?: false
      FragmentContainer(R.id.searchFragmentContainer) {
        SearchFragment().apply {
          arguments = Bundle().apply {
            putString(NAV_ARG_SEARCH_STRING, searchString)
            putBoolean(TAG_FROM_TAB_SWITCHER, isOpenedFromTabSwitcher)
            putBoolean(EXTRA_IS_WIDGET_VOICE, isVoice)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FragmentContainer(
  fragmentId: Int,
  fragmentProvider: () -> Fragment
) {
  val context = LocalContext.current
  val fragmentManager = remember {
    (context as AppCompatActivity).supportFragmentManager
  }

  AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { ctx ->
      FragmentContainerView(ctx).apply { id = fragmentId }
    }
  )

  // Lifecycle-safe fragment transaction
  // LaunchedEffect ensures this runs once per fragmentManager + fragmentId combination
  LaunchedEffect(fragmentManager, fragmentId) {
    fragmentManager.commit(
      // Allow state loss only if the fragmentManager has already saved its state
      // This prevents IllegalStateException ("Can not perform this action after onSaveInstanceState")
      // Bug fix #4454
      allowStateLoss = fragmentManager.isStateSaved
    ) {
      replace(fragmentId, fragmentProvider())
    }
  }
}

sealed class CustomDestination(val route: String) {
  object Reader : CustomDestination(READER_FRAGMENT)

  object History : CustomDestination(HISTORY_FRAGMENT)
  object Notes : CustomDestination(NOTES_FRAGMENT)
  object Bookmarks : CustomDestination(BOOKMARK_FRAGMENT)
  object Help : CustomDestination(HELP_FRAGMENT)
  object Settings : CustomDestination(SETTINGS_FRAGMENT)
  object Downloads : CustomDestination(DOWNLOAD_FRAGMENT)
  object Search : CustomDestination(
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
}
