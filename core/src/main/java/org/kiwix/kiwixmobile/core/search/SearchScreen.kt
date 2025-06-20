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

package org.kiwix.kiwixmobile.core.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixSearchView
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOUR_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.LOAD_MORE_PROGRESS_BAR_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MINIMUM_HEIGHT_OF_SEARCH_ITEM
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SEARCH_ITEM_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.THREE_DP

const val SEARCH_FIELD_TESTING_TAG = "searchFieldTestingTag"
const val NO_SEARCH_RESULT_TESTING_TAG = "noSearchResultTestingTag"
const val FIND_IN_PAGE_TESTING_TAG = "findInPageTestingTag"
const val SEARCH_ITEM_TESTING_TAG = "searchItemTestingTag"
const val LOADING_ITEMS_BEFORE = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
  searchScreenState: SearchScreenState,
  actionMenuItemList: List<ActionMenuItem>,
  isLoadingMoreResult: Boolean
) {
  val lazyListState = rememberLazyListState()
  KiwixTheme {
    Scaffold(
      topBar = {
        KiwixAppBar(
          title = stringResource(R.string.empty_string),
          navigationIcon = searchScreenState.navigationIcon,
          actionMenuItems = actionMenuItemList,
          searchBar = {
            KiwixSearchView(
              value = searchScreenState.searchText,
              searchViewTextFiledTestTag = SEARCH_FIELD_TESTING_TAG,
              onValueChange = searchScreenState.onSearchViewValueChange,
              onClearClick = searchScreenState.onSearchViewClearClick,
              modifier = Modifier,
              onKeyboardSubmitButtonClick = searchScreenState.onKeyboardSubmitButtonClick
            )
          }
        )
      }
    ) { innerPadding ->
      SearchScreenContent(searchScreenState, innerPadding, lazyListState)
    }
  }
  InfiniteListHandler(
    listState = lazyListState,
    isLoadingMoreResult = isLoadingMoreResult,
    onLoadMore = searchScreenState.onLoadMore
  )
}

@Composable
private fun SearchScreenContent(
  searchScreenState: SearchScreenState,
  innerPadding: PaddingValues,
  lazyListState: LazyListState
) {
  val progressBarTrackColor = MaterialTheme.colorScheme.background
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(
        top = innerPadding.calculateTopPadding(),
        start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
        end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
      ),
    contentAlignment = Alignment.Center
  ) {
    if (searchScreenState.searchList.isEmpty()) {
      NoSearchResultView()
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize(),
        state = lazyListState
      ) {
        items(searchScreenState.searchList) { item ->
          SearchListItem(
            searchListItem = item,
            onItemClick = { searchScreenState.onItemClick(item) },
            onNewTabIconClick = { searchScreenState.onNewTabIconClick(item) },
            onItemLongClick = if (item is SearchListItem.RecentSearchListItem) {
              { searchScreenState.onItemLongClick(item) }
            } else {
              null
            }
          )
        }
        showLoadMoreProgressBar(searchScreenState, progressBarTrackColor)
      }
    }
    ShowLoadingProgressBar(searchScreenState.isLoading, progressBarTrackColor)
  }
}

private fun LazyListScope.showLoadMoreProgressBar(
  searchScreenState: SearchScreenState,
  progressBarTrackColor: Color
) {
  if (searchScreenState.shouldShowLoadingMoreProgressBar) {
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(SIXTEEN_DP),
        contentAlignment = Alignment.Center
      ) {
        ContentLoadingProgressBar(
          modifier = Modifier.size(LOAD_MORE_PROGRESS_BAR_SIZE),
          circularProgressBarStockWidth = THREE_DP,
          progressBarTrackColor = progressBarTrackColor
        )
      }
    }
  }
}

@Composable
private fun ShowLoadingProgressBar(isLoading: Boolean, progressBarTrackColor: Color) {
  if (isLoading) {
    Box(
      modifier = Modifier
        .fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      ContentLoadingProgressBar(progressBarTrackColor = progressBarTrackColor)
    }
  }
}

@Composable
private fun NoSearchResultView() {
  Text(
    text = stringResource(R.string.no_results),
    textAlign = TextAlign.Center,
    modifier = Modifier
      .padding(horizontal = FOUR_DP)
      .semantics { testTag = NO_SEARCH_RESULT_TESTING_TAG }
  )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchListItem(
  searchListItem: SearchListItem,
  onNewTabIconClick: (SearchListItem) -> Unit,
  onItemClick: (SearchListItem) -> Unit,
  onItemLongClick: ((SearchListItem) -> Unit)? = null
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = MINIMUM_HEIGHT_OF_SEARCH_ITEM),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = searchListItem.value,
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = EIGHT_DP)
        .combinedClickable(
          onClick = { onItemClick(searchListItem) },
          onLongClick = { onItemLongClick?.invoke(searchListItem) }
        )
        .semantics { testTag = SEARCH_ITEM_TESTING_TAG },
      fontSize = SEARCH_ITEM_TEXT_SIZE,
    )

    IconButton(
      onClick = { onNewTabIconClick(searchListItem) },
    ) {
      Icon(
        painter = painterResource(id = R.drawable.ic_open_in_new_24dp),
        contentDescription = stringResource(id = R.string.search_open_in_new_tab),
      )
    }
  }
}

@Composable
fun InfiniteListHandler(
  listState: LazyListState,
  buffer: Int = LOADING_ITEMS_BEFORE,
  isLoadingMoreResult: Boolean,
  onLoadMore: () -> Unit
) {
  val shouldLoadMore = remember {
    derivedStateOf {
      val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
      val totalItemCount = listState.layoutInfo.totalItemsCount
      !isLoadingMoreResult && lastVisibleItemIndex >= totalItemCount - buffer
    }
  }

  LaunchedEffect(shouldLoadMore) {
    snapshotFlow { shouldLoadMore.value }.collect { load ->
      if (load) onLoadMore()
    }
  }
}
