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

package org.kiwix.kiwixmobile.nav.destination.library.online

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixSnackbarHost
import org.kiwix.kiwixmobile.core.ui.components.SwipeRefreshLayout
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.ui.theme.MineShaftGray700
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DOWNLOADING_LIBRARY_MESSAGE_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DOWNLOADING_LIBRARY_PROGRESSBAR_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DOWNLOADING_LIBRARY_PROGRESS_CARD_VIEW_CONTENT_MARGIN
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DOWNLOADING_LIBRARY_PROGRESS_CARD_VIEW_DEFAULT_MARGIN
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DOWNLOADING_LIBRARY_PROGRESS_CARD_VIEW_MAX_WIDTH
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOUR_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIX_DP
import org.kiwix.kiwixmobile.nav.destination.library.local.rememberScrollBehavior
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem.DividerItem

const val ONLINE_LIBRARY_LIST_TESTING_TAG = "onlineLibraryListTestingTag"

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ComposableLambdaParameterNaming")
@Composable
fun OnlineLibraryScreen(
  state: OnlineLibraryScreenState,
  listState: LazyListState
) {
  val (bottomNavHeight, lazyListState) =
    rememberScrollBehavior(state.bottomNavigationHeight, listState)
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

  KiwixTheme {
    Scaffold(
      snackbarHost = { KiwixSnackbarHost(snackbarHostState = state.snackBarHostState) },
      topBar = {
        KiwixAppBar(
          string.download,
          state.navigationIcon,
          state.actionMenuItems,
          scrollBehavior
        )
      },
      modifier = Modifier
        .systemBarsPadding()
        .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
      SwipeRefreshLayout(
        isRefreshing = state.swipeRefreshItem.first,
        isEnabled = state.swipeRefreshItem.second,
        onRefresh = state.onRefresh,
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(bottom = bottomNavHeight.value)
      ) {
        OnlineLibraryScreenContent(state, lazyListState)
      }
    }
  }
}

@Composable
private fun OnlineLibraryScreenContent(
  state: OnlineLibraryScreenState,
  lazyListState: LazyListState
) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    if (state.scanningProgressItem.first) {
      ShowFetchingLibraryLayout(state.scanningProgressItem.second)
    }
    if (state.noContentViewItem.second) {
      NoContentView(state.noContentViewItem.first)
    } else {
      OnlineLibraryList(state, lazyListState)
    }
  }
}

@Composable
private fun OnlineLibraryList(state: OnlineLibraryScreenState, lazyListState: LazyListState) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag(ONLINE_LIBRARY_LIST_TESTING_TAG),
    state = lazyListState
  ) {
    state.onlineLibraryList?.let { libraryList ->
      items(libraryList) {
        when (it) {
          is DividerItem -> ShowDividerItem(it)
          is LibraryListItem.BookItem -> OnlineBookItem(it)
          is LibraryListItem.LibraryDownloadItem -> DownloadBookItem(it)
        }
      }
    }
  }
}

@Composable
private fun ShowDividerItem(dividerItem: DividerItem) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = SIXTEEN_DP, vertical = EIGHT_DP)
      .minimumInteractiveComponentSize()
  ) {
    Text(
      text = stringResource(dividerItem.stringId),
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal)
    )
  }
}

@Composable
private fun NoContentView(noContentMessage: String) {
  Text(
    text = noContentMessage,
    textAlign = TextAlign.Center,
    modifier = Modifier.padding(horizontal = FOUR_DP)
  )
}

@Composable
private fun ShowFetchingLibraryLayout(message: String) {
  val cardContainerColor = if (isSystemInDarkTheme()) {
    MineShaftGray700
  } else {
    White
  }
  Card(
    modifier = Modifier
      .sizeIn(maxWidth = DOWNLOADING_LIBRARY_PROGRESS_CARD_VIEW_MAX_WIDTH)
      .padding(DOWNLOADING_LIBRARY_PROGRESS_CARD_VIEW_DEFAULT_MARGIN),
    shape = MaterialTheme.shapes.small,
    elevation = CardDefaults.cardElevation(defaultElevation = SIX_DP),
    colors = CardDefaults.cardColors(containerColor = cardContainerColor)
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(DOWNLOADING_LIBRARY_PROGRESS_CARD_VIEW_CONTENT_MARGIN)
    ) {
      ContentLoadingProgressBar(
        modifier = Modifier.size(DOWNLOADING_LIBRARY_PROGRESSBAR_SIZE),
        circularProgressBarStockWidth = 3.dp,
        progressBarTrackColor = cardContainerColor
      )
      Text(
        text = message,
        fontSize = DOWNLOADING_LIBRARY_MESSAGE_TEXT_SIZE,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = EIGHT_DP)
      )
    }
  }
}

// @Preview(device = "id:Nexus S")
// // @Preview(device = "id:Nexus S", name = "Night", uiMode = Configuration.UI_MODE_NIGHT_YES)
// @Composable
// private fun Preview() {
//   val context = LocalContext.current
//   val onlineLibraryState = OnlineLibraryScreenState(
//     listOf(
//       DividerItem(0, string.your_languages),
//       LibraryListItem.LibraryDownloadItem(
//         downloadId = 0,
//         favIcon = Base64String(null),
//         title = "100 Rabbits",
//         description = "Research and test low-tech solutions, and document findings",
//         bytesDownloaded = 500,
//         totalSizeBytes = 1000,
//         progress = 50,
//         eta = Seconds(6000),
//         downloadState = DownloadState.Paused,
//         currentDownloadState = Status.PAUSED,
//         id = 0
//       )
//     ),
//     56,
//     swipeRefreshItem = Pair(false, true),
//     snackBarHostState = SnackbarHostState(),
//     actionMenuItems = listOf(
//       ActionMenuItem(
//         IconItem.Drawable(org.kiwix.kiwixmobile.R.drawable.ic_baseline_mobile_screen_share_24px),
//         string.get_content_from_nearby_device,
//         { },
//         isEnabled = true,
//         testingTag = LOCAL_FILE_TRANSFER_MENU_BUTTON_TESTING_TAG
//       )
//     ),
//     navigationIcon = {
//       NavigationIcon(
//         iconItem = IconItem.Vector(Icons.Filled.Menu),
//         contentDescription = string.open_drawer,
//         onClick = { }
//       )
//     },
//     {},
//     scanningProgressItem = Pair(
//       false,
//       context.getString(string.starting_downloading_remote_library)
//     ),
//     noContentViewItem = Pair(context.getString(string.no_items_msg), false)
//   )
//   val lazyList = rememberLazyListState()
//   OnlineLibraryScreen(onlineLibraryState, lazyList)
// }
