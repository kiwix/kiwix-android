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

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.kiwix.kiwixmobile.core.page.bookmark.BookmarksFragment
import org.kiwix.kiwixmobile.core.page.history.HistoryFragment
import org.kiwix.kiwixmobile.core.page.notes.NotesFragment
import org.kiwix.kiwixmobile.core.search.SearchFragment
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
    startDestination = "readerFragment",
    modifier = modifier
  ) {
    composable("readerFragment") {
      FragmentContainer {
        KiwixReaderFragment().apply {
          arguments = Bundle().apply {
            putString("zimFileUri", "")
            putString("findInPageSearchString", "")
            putString("pageUrl", "")
            putBoolean("shouldOpenInNewTab", false)
            putString("searchItemTitle", "")
          }
        }
      }
    }
    composable("libraryFragment") {
      FragmentContainer {
        LocalLibraryFragment().apply {
          arguments = Bundle().apply {
            putString("zimFileUri", "")
          }
        }
      }
    }
    composable("downloadsFragment") {
      FragmentContainer {
        OnlineLibraryFragment()
      }
    }
    composable("bookmarksFragment") {
      FragmentContainer {
        BookmarksFragment()
      }
    }
    composable("notesFragment") {
      FragmentContainer {
        NotesFragment()
      }
    }
    composable("introFragment") {
      FragmentContainer {
        IntroFragment()
      }
    }
    composable("historyFragment") {
      FragmentContainer {
        HistoryFragment()
      }
    }
    composable("languageFragment") {
      FragmentContainer {
        LanguageFragment()
      }
    }
    composable("zimHostFragment") {
      FragmentContainer {
        ZimHostFragment()
      }
    }
    composable("helpFragment") {
      FragmentContainer {
        KiwixHelpFragment()
      }
    }
    composable("kiwixSettingsFragment") {
      FragmentContainer {
        KiwixSettingsFragment()
      }
    }
    composable("searchFragment") {
      FragmentContainer {
        SearchFragment()
      }
    }
    composable("localFileTransferFragment") {
      FragmentContainer {
        LocalFileTransferFragment().apply {
          arguments = Bundle().apply {
            putParcelableArray("uris", null)
          }
        }
      }
    }
  }
}

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
