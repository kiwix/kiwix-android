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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R.drawable
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.BackPressActivityExtensions
import org.kiwix.kiwixmobile.core.extensions.hideKeyboardOnLazyColumnScroll
import org.kiwix.kiwixmobile.core.main.reader.OnBackPressed
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixFloatingActionButton
import org.kiwix.kiwixmobile.core.ui.components.KiwixSearchView
import org.kiwix.kiwixmobile.core.ui.components.KiwixSnackbarHost
import org.kiwix.kiwixmobile.core.ui.components.SwipeRefreshLayout
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.theme.MineShaftGray700
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DOWNLOADING_LIBRARY_MESSAGE_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DOWNLOADING_LIBRARY_PROGRESSBAR_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DOWNLOADING_LIBRARY_PROGRESS_CARD_VIEW_CONTENT_MARGIN
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DOWNLOADING_LIBRARY_PROGRESS_CARD_VIEW_DEFAULT_MARGIN
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DOWNLOADING_LIBRARY_PROGRESS_CARD_VIEW_WIDTH
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOUR_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ONE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIX_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.THREE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWENTY_FOUR_DP
import org.kiwix.kiwixmobile.core.utils.FIVE
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryUiState
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.DividerItem
import kotlin.time.Duration.Companion.milliseconds

const val ONLINE_LIBRARY_SEARCH_VIEW_TESTING_TAG = "onlineLibrarySearchViewTestingTag"
const val ONLINE_LIBRARY_SEARCH_VIEW_CLOSE_BUTTON_TESTING_TAG =
  "onlineLibrarySearchViewCloseButtonTestingTag"
const val NO_CONTENT_VIEW_TEXT_TESTING_TAG = "noContentViewTextTestingTag"
const val SHOW_FETCHING_LIBRARY_LAYOUT_TESTING_TAG = "showFetchingLibraryLayoutTestingTag"
const val ONLINE_DIVIDER_ITEM_TEXT_TESTING_TAG = "onlineDividerItemTextTag"
const val LANGUAGE_TABS_ROW_TESTING_TAG = "languageTabsRowTestingTag"
const val LANGUAGE_TAB_TESTING_TAG_PREFIX = "languageTabTestingTag_"
const val CATEGORY_CHIPS_ROW_TESTING_TAG = "categoryChipsRowTestingTag"
const val CATEGORY_CHIP_TESTING_TAG_PREFIX = "categoryChipTestingTag_"
const val LOAD_MORE_DELAY = 150L
private const val BACK_TO_TOP_ITEM_THRESHOLD = 5
private val TAB_HEIGHT = 40.dp

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ComposableLambdaParameterNaming", "LongParameterList")
@Composable
fun OnlineLibraryScreen(
  uiState: OnlineLibraryUiState,
  onlineLibraryViewModel: OnlineLibraryViewModel,
  actionMenuItems: List<ActionMenuItem>,
  listState: LazyListState,
  snackBarHostState: SnackbarHostState,
  bottomAppBarScrollBehaviour: BottomAppBarScrollBehavior?,
  onUserBackPressed: () -> BackPressActivityExtensions.Super,
  navHostController: NavHostController,
  navigationIcon: @Composable () -> Unit
) {
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  val listStates = remember(uiState.tabs) { mutableMapOf<Int, LazyListState>() }
  val activeListState = if (uiState.tabs.size <= 1) {
    listState
  } else {
    listStates.getOrPut(uiState.selectedTabIndex) {
      if (uiState.selectedTabIndex == 0) listState else LazyListState()
    }
  }
  Scaffold(
    snackbarHost = { KiwixSnackbarHost(snackbarHostState = snackBarHostState) },
    topBar = {
      KiwixAppBar(
        title = stringResource(string.download),
        navigationIcon = navigationIcon,
        actionMenuItems = actionMenuItems,
        topAppBarScrollBehavior = scrollBehavior,
        searchBar = searchBarIfActive(uiState, onlineLibraryViewModel)
      )
    },
    floatingActionButton = {
      OnlineLibraryBackToTopButton(
        listState = activeListState,
        scrollBehavior = scrollBehavior,
        bottomAppBarScrollBehaviour = bottomAppBarScrollBehaviour
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
    OnlineLibraryMainContent(
      uiState,
      onlineLibraryViewModel,
      paddingValues,
      onUserBackPressed,
      navHostController,
      activeListState
    )
  }
}

@Composable
fun LanguageTabsRow(
  tabs: List<OnlineLibraryViewModel.LanguageTab>,
  selectedTabIndex: Int,
  onTabSelected: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val safeIndex = selectedTabIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
  Box(modifier = modifier.fillMaxWidth()) {
    HorizontalDivider(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter),
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
      thickness = ONE_DP
    )
    ScrollableTabRow(
      selectedTabIndex = safeIndex,
      edgePadding = SIXTEEN_DP,
      containerColor = Color.Transparent,
      divider = {},
      indicator = { tabPositions ->
        if (safeIndex < tabPositions.size) {
          TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(tabPositions[safeIndex]),
            color = MaterialTheme.colorScheme.primary
          )
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .semantics { testTag = LANGUAGE_TABS_ROW_TESTING_TAG }
    ) {
      tabs.forEachIndexed { index, tab ->
        val isSelected = index == safeIndex
        Tab(
          selected = isSelected,
          onClick = { onTabSelected(index) },
          text = {
            Text(
              text = tab.displayName,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              }
            )
          },
          modifier = Modifier
            .height(TAB_HEIGHT)
            .semantics {
              testTag = "$LANGUAGE_TAB_TESTING_TAG_PREFIX${tab.displayName}"
            }
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterChipsRow(
  categoryChips: List<String>,
  selectedCategories: Set<String>,
  onChipClick: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  CompositionLocalProvider(
    LocalMinimumInteractiveComponentSize provides Dp.Unspecified
  ) {
    LazyRow(
      modifier = modifier
        .fillMaxWidth()
        .semantics { testTag = CATEGORY_CHIPS_ROW_TESTING_TAG },
      horizontalArrangement = Arrangement.spacedBy(EIGHT_DP),
      contentPadding = PaddingValues(
        start = SIXTEEN_DP,
        end = SIXTEEN_DP,
        top = FOUR_DP,
        bottom = FOUR_DP
      )
    ) {
      items(categoryChips) { category ->
        val isSelected = selectedCategories.any { it.equals(category, ignoreCase = true) }
        FilterChip(
          selected = isSelected,
          onClick = { onChipClick(category) },
          label = { Text(category.toSentenceCaseCategory()) },
          leadingIcon = if (isSelected) {
            {
              Icon(
                imageVector = Icons.Default.Done,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
              )
            }
          } else {
            null
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
          ),
          modifier = Modifier.semantics {
            testTag = "$CATEGORY_CHIP_TESTING_TAG_PREFIX$category"
          }
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnlineLibraryMainContent(
  uiState: OnlineLibraryUiState,
  onlineLibraryViewModel: OnlineLibraryViewModel,
  paddingValues: PaddingValues,
  onUserBackPressed: () -> BackPressActivityExtensions.Super,
  navHostController: NavHostController,
  listState: LazyListState
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(
        top = paddingValues.calculateTopPadding(),
        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
      )
  ) {
    OnBackPressed(onUserBackPressed, navHostController)

    if (uiState.tabs.size > 1) {
      LanguageTabsRow(
        tabs = uiState.tabs,
        selectedTabIndex = uiState.selectedTabIndex,
        onTabSelected = onlineLibraryViewModel::selectTab
      )
    }

    if (uiState.categoryChips.isNotEmpty()) {
      CategoryFilterChipsRow(
        categoryChips = uiState.categoryChips,
        selectedCategories = uiState.selectedCategories,
        onChipClick = onlineLibraryViewModel::onCategoryChipClicked
      )
    }

    SwipeRefreshLayout(
      isRefreshing = uiState.isRefreshing && !uiState.showScanningProgressBar,
      isEnabled = !uiState.showScanningProgressBar,
      onRefresh = { onlineLibraryViewModel.refreshScreen(true) },
      modifier = Modifier.fillMaxSize()
    ) {
      OnlineLibraryScreenContent(uiState, listState, onlineLibraryViewModel)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnlineLibraryBackToTopButton(
  listState: LazyListState,
  scrollBehavior: TopAppBarScrollBehavior,
  bottomAppBarScrollBehaviour: BottomAppBarScrollBehavior?
) {
  val coroutineScope = rememberCoroutineScope()
  val shouldShowBackToTopButton by remember {
    derivedStateOf { listState.firstVisibleItemIndex >= BACK_TO_TOP_ITEM_THRESHOLD }
  }

  AnimatedVisibility(
    visible = shouldShowBackToTopButton,
    enter = slideInVertically { it },
    exit = slideOutVertically { it }
  ) {
    KiwixFloatingActionButton(
      icon = painterResource(id = drawable.ic_arrow_upward_24dp),
      onClick = {
        coroutineScope.launch {
          // Manually reset the topAppBar and bottomAppBar scroll offsets
          // so they become visible when scrolling to top programmatically.
          // animateScrollToItem alone does not update the nestedScrollConnection.
          scrollBehavior.state.heightOffset = 0f
          scrollBehavior.state.contentOffset = 0f
          bottomAppBarScrollBehaviour?.state?.heightOffset = 0f
          bottomAppBarScrollBehaviour?.state?.contentOffset = 0f
          listState.animateScrollToItem(ZERO)
        }
      },
      contentDescription = stringResource(string.pref_back_to_top),
      shouldPulse = true
    )
  }
}

@Composable
private fun searchBarIfActive(
  state: OnlineLibraryUiState,
  onlineLibraryViewModel: OnlineLibraryViewModel
): (@Composable () -> Unit)? = if (state.isSearchActive) {
  {
    KiwixSearchView(
      value = state.searchQuery,
      searchViewTextFiledTestTag = ONLINE_LIBRARY_SEARCH_VIEW_TESTING_TAG,
      clearButtonTestTag = ONLINE_LIBRARY_SEARCH_VIEW_CLOSE_BUTTON_TESTING_TAG,
      onValueChange = onlineLibraryViewModel::onSearchQueryChanged,
      onClearClick = onlineLibraryViewModel::clearSearch
    )
  }
} else {
  null
}

@Composable
private fun OnlineLibraryScreenContent(
  uiState: OnlineLibraryUiState,
  lazyListState: LazyListState,
  onlineLibraryViewModel: OnlineLibraryViewModel
) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    if (uiState.showNoContent) {
      NoContentView(uiState.noContentMessage)
    } else {
      OnlineLibraryList(uiState, lazyListState, onlineLibraryViewModel)
    }
    if (uiState.showScanningProgressBar) {
      ShowFetchingLibraryLayout(uiState.scanningProgressBarMessage)
    }
  }
}

@OptIn(FlowPreview::class)
@Composable
private fun OnlineLibraryList(
  state: OnlineLibraryUiState,
  lazyListState: LazyListState,
  onlineLibraryViewModel: OnlineLibraryViewModel
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      // hides keyboard when scrolled
      .hideKeyboardOnLazyColumnScroll(lazyListState),
    state = lazyListState
  ) {
    itemsIndexed(state.items) { index, item ->
      when (item) {
        is DividerItem -> {
          if (item.id != Long.MIN_VALUE) {
            ShowDividerItem(item)
          }
        }

        is LibraryListItem.BookItem -> OnlineBookItem(
          index = index,
          item = item,
          onlineLibraryViewModel.bookUtils,
          onlineLibraryViewModel.availableSpaceCalculator
        ) { onlineLibraryViewModel.onBookItemClick(it) }

        is LibraryListItem.LibraryDownloadItem -> DownloadBookItem(
          index = index,
          item = item,
          onPauseResumeClick = onlineLibraryViewModel::onPauseResumeButtonClick,
          onStopClick = onlineLibraryViewModel::onStopButtonClick
        )
      }
    }
    showLoadMoreProgressBar(state.isLoadingMore)
  }
  LaunchedEffect(lazyListState, state.items) {
    snapshotFlow {
      val layoutInfo = lazyListState.layoutInfo
      val visibleIndexes = layoutInfo.visibleItemsInfo.map { it.index }
      val list = state.items
      val visibleBookIndexes = visibleIndexes.filter { index ->
        list.getOrNull(index) is LibraryListItem.BookItem
      }
      val lastVisibleBookIndex = visibleBookIndexes.maxOrNull() ?: -1
      val totalBookCount = list.count { it is LibraryListItem.BookItem }

      lastVisibleBookIndex to totalBookCount
    }.distinctUntilChanged()
      .debounce(LOAD_MORE_DELAY.milliseconds)
      .collect { (lastVisibleBookIndex, totalBookCount) ->
        if (lastVisibleBookIndex >= totalBookCount.minus(FIVE) && !state.isLoadingMore) {
          onlineLibraryViewModel.handleLoadMore(totalBookCount)
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
  var isExpanded by remember { mutableStateOf(false) }
  var isTruncated by remember { mutableStateOf(false) }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = SIXTEEN_DP)
      .padding(top = SIXTEEN_DP, bottom = EIGHT_DP)
      .animateContentSize()
      .then(
        if (isTruncated || isExpanded) {
          Modifier.clickable { isExpanded = !isExpanded }
        } else {
          Modifier
        }
      )
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = dividerItem.sectionTitle,
        textAlign = TextAlign.Start,
        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { textLayoutResult ->
          isTruncated = textLayoutResult.hasVisualOverflow
        },
        style = MaterialTheme.typography.titleSmall.copy(
          fontWeight = FontWeight.Normal,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier
          .weight(1f)
          .semantics { testTag = ONLINE_DIVIDER_ITEM_TEXT_TESTING_TAG }
      )
      if (isTruncated || isExpanded) {
        Icon(
          imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(TWENTY_FOUR_DP)
        )
      }
    }
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
