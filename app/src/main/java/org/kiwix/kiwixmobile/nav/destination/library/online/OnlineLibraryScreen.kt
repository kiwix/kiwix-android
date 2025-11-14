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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.downloader.downloadManager.FIVE
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.extensions.hideKeyboardOnLazyColumnScroll
import org.kiwix.kiwixmobile.core.main.reader.OnBackPressed
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixSearchView
import org.kiwix.kiwixmobile.core.ui.components.KiwixSnackbarHost
import org.kiwix.kiwixmobile.core.ui.components.SwipeRefreshLayout
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.ui.theme.MineShaftGray700
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DOWNLOADING_LIBRARY_MESSAGE_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DOWNLOADING_LIBRARY_PROGRESSBAR_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DOWNLOADING_LIBRARY_PROGRESS_CARD_VIEW_CONTENT_MARGIN
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DOWNLOADING_LIBRARY_PROGRESS_CARD_VIEW_DEFAULT_MARGIN
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DOWNLOADING_LIBRARY_PROGRESS_CARD_VIEW_WIDTH
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOUR_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIX_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.THREE_DP
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.DividerItem

const val ONLINE_LIBRARY_SEARCH_VIEW_TESTING_TAG = "onlineLibrarySearchViewTestingTag"
const val ONLINE_LIBRARY_SEARCH_VIEW_CLOSE_BUTTON_TESTING_TAG =
  "onlineLibrarySearchViewCloseButtonTestingTag"
const val NO_CONTENT_VIEW_TEXT_TESTING_TAG = "noContentViewTextTestingTag"
const val SHOW_FETCHING_LIBRARY_LAYOUT_TESTING_TAG = "showFetchingLibraryLayoutTestingTag"
const val ONLINE_DIVIDER_ITEM_TEXT_TESTING_TAG = "onlineDividerItemTextTag"

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ComposableLambdaParameterNaming")
@Composable
fun OnlineLibraryScreen(
  state: OnlineLibraryScreenState,
  actionMenuItems: List<ActionMenuItem>,
  listState: LazyListState,
  bottomAppBarScrollBehaviour: BottomAppBarScrollBehavior?,
  onUserBackPressed: () -> FragmentActivityExtensions.Super,
  navHostController: NavHostController,
  navigationIcon: @Composable () -> Unit,
) {
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

  KiwixTheme {
    Scaffold(
      snackbarHost = { KiwixSnackbarHost(snackbarHostState = state.snackBarHostState) },
      topBar = {
        KiwixAppBar(
          title = stringResource(string.download),
          navigationIcon = navigationIcon,
          actionMenuItems = actionMenuItems,
          topAppBarScrollBehavior = scrollBehavior,
          searchBar = searchBarIfActive(state)
        )
      },
      modifier = Modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .let { baseModifier ->
          bottomAppBarScrollBehaviour?.let {
            baseModifier.nestedScroll(it.nestedScrollConnection)
          } ?: baseModifier
        }
    ) { paddingValues ->
      SwipeRefreshLayout(
        isRefreshing = state.isRefreshing && !state.scanningProgressItem.first,
        isEnabled = !state.scanningProgressItem.first,
        onRefresh = state.onRefresh,
        modifier = Modifier
          .fillMaxSize()
          .padding(
            top = paddingValues.calculateTopPadding(),
            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
          )
      ) {
        OnBackPressed(onUserBackPressed, navHostController)
        OnlineLibraryScreenContent(state, listState)
      }
    }
  }
}

@Composable
private fun searchBarIfActive(
  state: OnlineLibraryScreenState
): (@Composable () -> Unit)? = if (state.isSearchActive) {
  {
    KiwixSearchView(
      value = state.searchText,
      searchViewTextFiledTestTag = ONLINE_LIBRARY_SEARCH_VIEW_TESTING_TAG,
      clearButtonTestTag = ONLINE_LIBRARY_SEARCH_VIEW_CLOSE_BUTTON_TESTING_TAG,
      onValueChange = { state.searchValueChangedListener(it) },
      onClearClick = { state.clearSearchButtonClickListener.invoke() }
    )
  }
} else {
  null
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
    if (state.noContentViewItem.second) {
      NoContentView(state.noContentViewItem.first)
    } else {
      OnlineLibraryList(state, lazyListState)
    }
    if (state.scanningProgressItem.first) {
      ShowFetchingLibraryLayout(state.scanningProgressItem.second)
    }
  }
}

@Composable
private fun OnlineLibraryList(state: OnlineLibraryScreenState, lazyListState: LazyListState) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      // hides keyboard when scrolled
      .hideKeyboardOnLazyColumnScroll(lazyListState),
    state = lazyListState
  ) {
    state.onlineLibraryList?.let { libraryList ->
      itemsIndexed(libraryList) { index, item ->
        when (item) {
          is DividerItem -> ShowDividerItem(item)
          is LibraryListItem.BookItem -> OnlineBookItem(
            index = index,
            item = item,
            state.bookUtils,
            state.availableSpaceCalculator,
            state.onBookItemClick
          )

          is LibraryListItem.LibraryDownloadItem -> DownloadBookItem(
            index = index,
            item = item,
            onPauseResumeClick = state.onPauseResumeButtonClick,
            onStopClick = state.onStopButtonClick
          )
        }
      }
    }
    showLoadMoreProgressBar(state.isLoadingMoreItem)
  }

  LaunchedEffect(lazyListState, state.onlineLibraryList) {
    snapshotFlow { lazyListState.layoutInfo }
      .combine(
        snapshotFlow { state.onlineLibraryList.orEmpty() }
      ) { layoutInfo, libraryList ->
        val bookItems = libraryList.filterIsInstance<LibraryListItem.BookItem>()
        val totalItems = layoutInfo.totalItemsCount
        val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: ZERO
        Triple(bookItems, totalItems, lastVisibleItemIndex)
      }
      .distinctUntilChanged()
      .collect { (bookItems, totalItems, lastVisibleItemIndex) ->
        if (bookItems.isNotEmpty() && lastVisibleItemIndex >= totalItems.minus(FIVE)) {
          state.onLoadMore(totalItems)
        }
      }
  }
}

private fun LazyListScope.showLoadMoreProgressBar(isLoadingMoreItem: Boolean) {
  if (isLoadingMoreItem) {
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(SIXTEEN_DP),
        contentAlignment = Alignment.Center
      ) {
        ContentLoadingProgressBar()
      }
    }
  }
}

@Composable
private fun ShowDividerItem(dividerItem: DividerItem) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = SIXTEEN_DP)
      .padding(top = SIXTEEN_DP, bottom = EIGHT_DP)
  ) {
    Text(
      text = dividerItem.sectionTitle,
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
      modifier = Modifier.semantics { testTag = ONLINE_DIVIDER_ITEM_TEXT_TESTING_TAG }
    )
  }
}

@Composable
private fun NoContentView(noContentMessage: String) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = noContentMessage,
      textAlign = TextAlign.Center,
      modifier = Modifier
        .padding(horizontal = FOUR_DP)
        .semantics { testTag = NO_CONTENT_VIEW_TEXT_TESTING_TAG }
    )
  }
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
      .width(DOWNLOADING_LIBRARY_PROGRESS_CARD_VIEW_WIDTH)
      .padding(DOWNLOADING_LIBRARY_PROGRESS_CARD_VIEW_DEFAULT_MARGIN)
      .semantics {
        testTag = SHOW_FETCHING_LIBRARY_LAYOUT_TESTING_TAG
      },
    shape = MaterialTheme.shapes.small,
    elevation = CardDefaults.cardElevation(defaultElevation = SIX_DP),
    colors = CardDefaults.cardColors(containerColor = cardContainerColor)
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .padding(DOWNLOADING_LIBRARY_PROGRESS_CARD_VIEW_CONTENT_MARGIN)
        .fillMaxWidth()
    ) {
      ContentLoadingProgressBar(
        modifier = Modifier.size(DOWNLOADING_LIBRARY_PROGRESSBAR_SIZE),
        circularProgressBarStockWidth = THREE_DP,
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
