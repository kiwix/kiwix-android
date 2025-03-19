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

package org.kiwix.kiwixmobile.nav.destination.library.local

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import org.kiwix.kiwixmobile.R.string
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixButton
import org.kiwix.kiwixmobile.core.ui.components.KiwixSnackbarHost
import org.kiwix.kiwixmobile.core.ui.components.ProgressBarStyle
import org.kiwix.kiwixmobile.core.ui.components.SwipeRefreshLayout
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.theme.Black
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FAB_ICON_BOTTOM_MARGIN
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.ui.BookItem
import org.kiwix.kiwixmobile.ui.ZimFilesLanguageHeader
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ComposableLambdaParameterNaming", "LongParameterList")
@Composable
fun LocalLibraryScreen(
  state: FileSelectListState,
  snackBarHostState: SnackbarHostState,
  swipeRefreshItem: Pair<Boolean, Boolean>,
  onRefresh: () -> Unit,
  scanningProgressItem: Pair<Boolean, Int>,
  noFilesViewItem: Triple<String, String, Boolean>,
  onDownloadButtonClick: () -> Unit,
  fabButtonClick: () -> Unit,
  actionMenuItems: List<ActionMenuItem>,
  onClick: ((BookOnDisk) -> Unit)? = null,
  onLongClick: ((BookOnDisk) -> Unit)? = null,
  onMultiSelect: ((BookOnDisk) -> Unit)? = null,
  navigationIcon: @Composable () -> Unit
) {
  KiwixTheme {
    Scaffold(
      snackbarHost = { KiwixSnackbarHost(snackbarHostState = snackBarHostState) },
      topBar = { KiwixAppBar(R.string.library, navigationIcon, actionMenuItems) },
      modifier = Modifier.systemBarsPadding()
    ) { contentPadding ->
      SwipeRefreshLayout(
        isRefreshing = swipeRefreshItem.first,
        isEnabled = swipeRefreshItem.second,
        onRefresh = onRefresh,
        modifier = Modifier
          .fillMaxSize()
          .padding(contentPadding)
      ) {
        if (scanningProgressItem.first) {
          ContentLoadingProgressBar(
            progressBarStyle = ProgressBarStyle.HORIZONTAL,
            progress = scanningProgressItem.second
          )
        }
        if (noFilesViewItem.third) {
          NoFilesView(noFilesViewItem, onDownloadButtonClick)
        } else {
          BookItemList(state, onClick, onLongClick, onMultiSelect)
        }

        SelectFileButton(
          fabButtonClick,
          Modifier
            .align(Alignment.BottomEnd)
            .padding(end = SIXTEEN_DP, bottom = FAB_ICON_BOTTOM_MARGIN)
        )
      }
    }
  }
}

@Composable
private fun BookItemList(
  state: FileSelectListState,
  onClick: ((BookOnDisk) -> Unit)? = null,
  onLongClick: ((BookOnDisk) -> Unit)? = null,
  onMultiSelect: ((BookOnDisk) -> Unit)? = null,
) {
  LazyColumn(modifier = Modifier.fillMaxSize()) {
    itemsIndexed(state.bookOnDiskListItems) { index, bookItem ->
      when (bookItem) {
        is BooksOnDiskListItem.LanguageItem -> {
          ZimFilesLanguageHeader(bookItem)
        }

        is BookOnDisk -> {
          BookItem(
            index = index,
            bookOnDisk = bookItem,
            selectionMode = state.selectionMode,
            onClick = onClick,
            onLongClick = onLongClick,
            onMultiSelect = onMultiSelect
          )
        }
      }
    }
  }
}

@Composable
private fun SelectFileButton(fabButtonClick: () -> Unit, modifier: Modifier) {
  FloatingActionButton(
    onClick = fabButtonClick,
    modifier = modifier,
    containerColor = Black,
    shape = MaterialTheme.shapes.extraLarge
  ) {
    Icon(
      painter = painterResource(id = R.drawable.ic_add_blue_24dp),
      contentDescription = stringResource(id = string.select_zim_file),
      tint = White
    )
  }
}

@Composable
fun NoFilesView(
  noFilesViewItem: Triple<String, String, Boolean>,
  onDownloadButtonClick: () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = noFilesViewItem.first,
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
    )
    Spacer(modifier = Modifier.height(EIGHT_DP))
    KiwixButton(noFilesViewItem.second, onDownloadButtonClick)
  }
}

// @Preview
// @Preview(name = "NightMode", uiMode = Configuration.UI_MODE_NIGHT_YES)
// @Composable
// fun PreviewLocalLibrary() {
//   LocalLibraryScreen(
//     state = FileSelectListState(listOf(), SelectionMode.NORMAL),
//     snackBarHostState = SnackbarHostState(),
//     actionMenuItems = listOf(
//       ActionMenuItem(
//         IconItem.Drawable(org.kiwix.kiwixmobile.R.drawable.ic_baseline_mobile_screen_share_24px),
//         R.string.get_content_from_nearby_device,
//         { },
//         isEnabled = true,
//         testingTag = DELETE_MENU_BUTTON_TESTING_TAG
//       )
//     ),
//     onRefresh = {},
//     fabButtonClick = {},
//     swipeRefreshItem = false to true,
//     noFilesViewItem = Triple(
//       stringResource(R.string.no_files_here),
//       stringResource(R.string.download_books),
//       {}
//     )
//   ) {
//     NavigationIcon(IconItem.Vector(Icons.Filled.Menu), {})
//   }
// }
