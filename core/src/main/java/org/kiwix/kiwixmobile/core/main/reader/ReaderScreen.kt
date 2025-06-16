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

package org.kiwix.kiwixmobile.core.main.reader

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.main.DarkModeViewPainter
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixButton
import org.kiwix.kiwixmobile.core.ui.components.KiwixSnackbarHost
import org.kiwix.kiwixmobile.core.ui.components.ProgressBarStyle
import org.kiwix.kiwixmobile.core.ui.components.ScrollDirection
import org.kiwix.kiwixmobile.core.ui.components.rememberLazyListScrollListener
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem.Drawable
import org.kiwix.kiwixmobile.core.ui.models.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.Black
import org.kiwix.kiwixmobile.core.ui.theme.KiwixDialogTheme
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.CLOSE_ALL_TAB_BUTTON_BOTTOM_PADDING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOUR_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ONE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.READER_BOTTOM_APP_BAR_BUTTON_ICON_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.READER_BOTTOM_APP_BAR_LAYOUT_HEIGHT
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TTS_BUTTONS_CONTROL_ALPHA
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWO_DP
import org.kiwix.kiwixmobile.core.utils.StyleUtils.fromHtml

const val CONTENT_LOADING_PROGRESSBAR_TESTING_TAG = "contentLoadingProgressBarTestingTag"

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ComposableLambdaParameterNaming")
@Composable
fun ReaderScreen(
  state: ReaderScreenState,
  listState: LazyListState,
  actionMenuItems: List<ActionMenuItem>,
  navigationIcon: @Composable () -> Unit
) {
  val (bottomNavHeight, lazyListState) =
    rememberScrollBehavior(state.bottomNavigationHeight, listState)
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  KiwixDialogTheme {
    Scaffold(
      snackbarHost = { KiwixSnackbarHost(snackbarHostState = state.snackBarHostState) },
      topBar = {
        KiwixAppBar(
          state.readerScreenTitle,
          navigationIcon,
          actionMenuItems,
          scrollBehavior
        )
      },
      floatingActionButton = { BackToTopFab(state) },
      modifier = Modifier
        .systemBarsPadding()
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .padding(bottom = bottomNavHeight.value)
    ) { paddingValues ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
      ) {
        if (state.isNoBookOpenInReader) {
          NoBookOpenView(state.onOpenLibraryButtonClicked)
        } else {
          ShowZIMFileContent(state)
          ShowProgressBarIfZIMFilePageIsLoading(state)
          TtsControls(state)
          BottomAppBarOfReaderScreen(
            state.bookmarkButtonItem,
            state.previousPageButtonItem,
            state.onHomeButtonClick,
            state.nextPageButtonItem,
            state.onTocClick,
            state.shouldShowBottomAppBar
          )
          ShowFullScreenView(state)
        }
        ShowDonationLayout(state)
      }
    }
  }
}

@Composable
private fun ShowZIMFileContent(state: ReaderScreenState) {
  state.selectedWebView?.let { selectedWebView ->
    key(selectedWebView) {
      AndroidView(
        factory = { selectedWebView },
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}

@Composable
private fun ShowFullScreenView(state: ReaderScreenState) {
  if (state.fullScreenItem.first) {
    state.fullScreenItem.second
  }
}

@Composable
private fun BoxScope.ShowProgressBarIfZIMFilePageIsLoading(state: ReaderScreenState) {
  if (state.pageLoadingItem.first) {
    ContentLoadingProgressBar(
      modifier = Modifier
        .testTag(CONTENT_LOADING_PROGRESSBAR_TESTING_TAG)
        .align(Alignment.TopCenter),
      progressBarStyle = ProgressBarStyle.HORIZONTAL,
      progress = state.pageLoadingItem.second
    )
  }
}

@Composable
private fun NoBookOpenView(
  onOpenLibraryButtonClicked: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = FOUR_DP)
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = stringResource(R.string.no_open_book),
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(EIGHT_DP))
    KiwixButton(
      buttonText = stringResource(R.string.open_library),
      clickListener = onOpenLibraryButtonClicked
    )
  }
}

@Composable
private fun BoxScope.TtsControls(state: ReaderScreenState) {
  if (state.showTtsControls) {
    Row(modifier = Modifier.align(Alignment.BottomCenter)) {
      Button(
        onClick = state.onPauseTtsClick,
        modifier = Modifier
          .weight(1f)
          .alpha(TTS_BUTTONS_CONTROL_ALPHA)
      ) {
        Text(
          text = state.pauseTtsButtonText,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.width(FOUR_DP))
      Button(
        onClick = state.onStopTtsClick,
        modifier = Modifier
          .weight(1f)
          .alpha(TTS_BUTTONS_CONTROL_ALPHA)
      ) {
        Text(
          text = stringResource(R.string.stop),
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
private fun BackToTopFab(state: ReaderScreenState) {
  if (state.showBackToTopButton) {
    FloatingActionButton(
      onClick = state.backToTopButtonClick,
      modifier = Modifier,
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      shape = FloatingActionButtonDefaults.smallShape
    ) {
      Icon(
        painter = Drawable(R.drawable.action_find_previous).toPainter(),
        contentDescription = stringResource(R.string.pref_back_to_top),
        tint = White
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.BottomAppBarOfReaderScreen(
  bookmarkButtonItem: Triple<() -> Unit, () -> Unit, Drawable>,
  previousPageButtonItem: Triple<() -> Unit, () -> Unit, Boolean>,
  onHomeButtonClick: () -> Unit,
  nextPageButtonItem: Triple<() -> Unit, () -> Unit, Boolean>,
  onTocClick: () -> Unit,
  shouldShowBottomAppBar: Boolean
) {
  if (!shouldShowBottomAppBar) return
  BottomAppBar(
    containerColor = Black,
    contentColor = White,
    modifier = Modifier.align(Alignment.BottomCenter),
    scrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(READER_BOTTOM_APP_BAR_LAYOUT_HEIGHT),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceEvenly
    ) {
      // Bookmark Icon
      BottomAppBarButtonIcon(
        onClick = bookmarkButtonItem.first,
        onLongClick = bookmarkButtonItem.second,
        buttonIcon = bookmarkButtonItem.third,
        contentDescription = stringResource(R.string.bookmarks)
      )
      // Back Icon(for going to previous page)
      BottomAppBarButtonIcon(
        onClick = previousPageButtonItem.first,
        onLongClick = previousPageButtonItem.second,
        buttonIcon = Drawable(R.drawable.ic_keyboard_arrow_left_24dp),
        shouldEnable = previousPageButtonItem.third,
        contentDescription = stringResource(R.string.go_to_previous_page)
      )
      // Home Icon(to open the home page of ZIM file)
      BottomAppBarButtonIcon(
        onClick = onHomeButtonClick,
        buttonIcon = Drawable(R.drawable.action_home),
        contentDescription = stringResource(R.string.menu_home)
      )
      // Forward Icon(for going to next page)
      BottomAppBarButtonIcon(
        onClick = nextPageButtonItem.first,
        onLongClick = nextPageButtonItem.second,
        buttonIcon = Drawable(R.drawable.ic_keyboard_arrow_right_24dp),
        shouldEnable = nextPageButtonItem.third,
        contentDescription = stringResource(R.string.go_to_next_page)
      )
      // Toggle Icon(to open the table of content in right side bar)
      BottomAppBarButtonIcon(
        onClick = onTocClick,
        buttonIcon = Drawable(R.drawable.ic_toc_24dp),
        contentDescription = stringResource(R.string.table_of_contents)
      )
    }
  }
}

@Composable
private fun BottomAppBarButtonIcon(
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
  buttonIcon: IconItem,
  shouldEnable: Boolean = true,
  contentDescription: String
) {
  IconButton(
    onClick = onClick,
    modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    enabled = shouldEnable
  ) {
    Icon(
      buttonIcon.toPainter(),
      contentDescription,
      modifier = Modifier.size(READER_BOTTOM_APP_BAR_BUTTON_ICON_SIZE)
    )
  }
}

@Composable
private fun BoxScope.ShowDonationLayout(state: ReaderScreenState) {
  if (state.shouldShowDonationPopup) {
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
    ) {
      // TODO create donation popup layout.
    }
  }
}

@Composable
fun TabSwitcherView(
  webViews: List<KiwixWebView>,
  selectedIndex: Int,
  onSelectTab: (Int) -> Unit,
  onCloseTab: (Int) -> Unit,
  onCloseAllTabs: () -> Unit,
  painter: DarkModeViewPainter
) {
  Box(modifier = Modifier.fillMaxSize()) {
    LazyRow(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .padding(top = SIXTEEN_DP),
      contentPadding = PaddingValues(horizontal = SIXTEEN_DP, vertical = EIGHT_DP),
      horizontalArrangement = Arrangement.spacedBy(EIGHT_DP)
    ) {
      itemsIndexed(webViews, key = { _, item -> item.hashCode() }) { index, webView ->
        val context = LocalContext.current
        val title = remember(webView) {
          webView.title?.fromHtml()?.toString()
            ?: context.getString(R.string.menu_home)
        }

        LaunchedEffect(webView) {
          if (title != context.getString(R.string.menu_home)) {
            painter.update(webView)
          }
        }

        TabItemView(
          title = title,
          isSelected = index == selectedIndex,
          webView = webView,
          onSelectTab = { onSelectTab(index) },
          onCloseTab = { onCloseTab(index) }
        )
      }
    }
    CloseAllTabButton(onCloseAllTabs)
  }
}

@Composable
private fun BoxScope.CloseAllTabButton(onCloseAllTabs: () -> Unit) {
  FloatingActionButton(
    onClick = onCloseAllTabs,
    modifier = Modifier
      .align(Alignment.BottomCenter)
      .padding(bottom = CLOSE_ALL_TAB_BUTTON_BOTTOM_PADDING)
  ) {
    Icon(
      painter = painterResource(R.drawable.ic_close_black_24dp),
      contentDescription = stringResource(R.string.close_all_tabs)
    )
  }
}

@Suppress("MagicNumber")
@Composable
fun TabItemView(
  title: String,
  isSelected: Boolean,
  webView: KiwixWebView,
  modifier: Modifier = Modifier,
  onSelectTab: () -> Unit,
  onCloseTab: () -> Unit
) {
  val cardElevation = if (isSelected) EIGHT_DP else TWO_DP
  val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

  Box(modifier = modifier) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .padding(horizontal = EIGHT_DP, vertical = FOUR_DP)
        .widthIn(min = 200.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = FOUR_DP),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .weight(1f)
            .padding(end = EIGHT_DP),
          style = MaterialTheme.typography.labelLarge
        )
        IconButton(onClick = onCloseTab) {
          Icon(
            painter = painterResource(id = R.drawable.ic_clear_white_24dp),
            contentDescription = stringResource(R.string.close_tab)
          )
        }
      }

      // Card with WebView (non-interactive with overlay)
      Card(
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        border = BorderStroke(ONE_DP, borderColor),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1.6f) // approximate height logic
          .clickable { onSelectTab() }
      ) {
        AndroidView(
          factory = { context ->
            // Detach if needed to avoid WebView already has a parent issue
            (webView.parent as? ViewGroup)?.removeView(webView)
            FrameLayout(context).apply {
              addView(webView)
            }
          },
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}

@Composable
fun rememberScrollBehavior(
  bottomNavigationHeight: Int,
  listState: LazyListState,
): Pair<MutableState<Dp>, LazyListState> {
  val bottomNavHeightInDp = with(LocalDensity.current) { bottomNavigationHeight.toDp() }
  val bottomNavHeight = remember { mutableStateOf(bottomNavHeightInDp) }
  val lazyListState = rememberLazyListScrollListener(
    lazyListState = listState,
    onScrollChanged = { direction ->
      when (direction) {
        ScrollDirection.SCROLL_UP -> {
          bottomNavHeight.value = bottomNavHeightInDp
        }

        ScrollDirection.SCROLL_DOWN -> {
          bottomNavHeight.value = ZERO.dp
        }

        ScrollDirection.IDLE -> {}
      }
    }
  )

  return bottomNavHeight to lazyListState
}
