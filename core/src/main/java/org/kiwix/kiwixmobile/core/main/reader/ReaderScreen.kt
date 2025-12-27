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

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.utils.HUNDERED
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.extensions.update
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.UpdateDialog
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixButton
import org.kiwix.kiwixmobile.core.ui.components.KiwixSnackbarHost
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.ui.components.ProgressBarStyle
import org.kiwix.kiwixmobile.core.ui.components.TWELVE
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem.Drawable
import org.kiwix.kiwixmobile.core.ui.models.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.DenimBlue800
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.ui.theme.MineShaftGray700
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.BACK_TO_TOP_BUTTON_BOTTOM_MARGIN
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.CLOSE_ALL_TAB_BUTTON_BOTTOM_PADDING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.CLOSE_TAB_ICON_ANIMATION_TIMEOUT
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.CLOSE_TAB_ICON_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOUR_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.KIWIX_TOOLBAR_HEIGHT
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.LARGE_BODY_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.NAVIGATION_DRAWER_WIDTH
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ONE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.READER_BOTTOM_APP_BAR_BUTTON_ICON_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.READER_BOTTOM_APP_BAR_DISABLE_BUTTON_ALPHA
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.READER_BOTTOM_APP_BAR_LAYOUT_HEIGHT
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SEARCH_PLACEHOLDER_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.THREE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TTS_BUTTONS_CONTROL_ALPHA
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWELVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWO_DP
import org.kiwix.kiwixmobile.core.utils.StyleUtils.fromHtml

const val CONTENT_LOADING_PROGRESSBAR_TESTING_TAG = "contentLoadingProgressBarTestingTag"
const val TAB_SWITCHER_VIEW_TESTING_TAG = "tabSwitcherViewTestingTag"
const val READER_SCREEN_TESTING_TAG = "readerScreenTestingTag"
const val CLOSE_ALL_TABS_BUTTON_TESTING_TAG = "closeAllTabsButtonTestingTag"
const val TAB_TITLE_TESTING_TAG = "tabTitleTestingTag"
const val READER_BOTTOM_BAR_BOOKMARK_BUTTON_TESTING_TAG = "readerBottomBarBookmarkButtonTestingTag"
const val READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG =
  "readerBottomBarPreviousScreenButtonTestingTag"
const val READER_BOTTOM_BAR_NEXT_SCREEN_BUTTON_TESTING_TAG =
  "readerBottomBarNextScreenButtonTestingTag"
const val READER_BOTTOM_BAR_HOME_BUTTON_TESTING_TAG = "readerBottomBarHomeButtonTestingTag"
const val READER_BOTTOM_BAR_TABLE_CONTENT_BUTTON_TESTING_TAG =
  "readerBottomBarTableContentButtonTestingTag"
const val TTS_CONTROL_STOP_BUTTON_TESTING_TAG = "ttsControlStopButtonTestingTag"

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ComposableLambdaParameterNaming", "LongMethod", "LongParameterList")
@Composable
fun ReaderScreen(
  state: ReaderScreenState,
  actionMenuItems: List<ActionMenuItem>,
  showTableOfContentDrawer: MutableState<Boolean>,
  documentSections: MutableList<DocumentSection>?,
  onUserBackPressed: () -> FragmentActivityExtensions.Super,
  navHostController: NavHostController,
  mainActivityBottomAppBarScrollBehaviour: BottomAppBarScrollBehavior?,
  navigationIcon: @Composable () -> Unit
) {
  // For managing the scroll event handling of webView.
  val shouldUpdateTopAppBarAndBottomAppBarOnScrolling = remember { mutableStateOf(true) }
  val topAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  val bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
  LaunchedEffect(bottomAppBarScrollBehavior.state.heightOffset) {
    mainActivityBottomAppBarScrollBehaviour?.state?.heightOffset =
      bottomAppBarScrollBehavior.state.heightOffset
  }
  KiwixTheme {
    Box(Modifier.fillMaxSize()) {
      Scaffold(
        snackbarHost = { KiwixSnackbarHost(snackbarHostState = state.snackBarHostState) },
        topBar = {
          ReaderTopBar(
            state,
            actionMenuItems,
            topAppBarScrollBehavior,
            navigationIcon
          )
        },
        floatingActionButton = { BackToTopFab(state) },
        modifier = Modifier
          .systemBarsPadding()
          .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
          .nestedScroll(bottomAppBarScrollBehavior.nestedScrollConnection)
          .semantics { testTag = READER_SCREEN_TESTING_TAG }
      ) { paddingValues ->
        ShowUpdateDialog(
          state,
          navHostController
        )
        OnBackPressed(onUserBackPressed, navHostController)
        ReaderContentLayout(
          state,
          Modifier.padding(paddingValues),
          bottomAppBarScrollBehavior,
          topAppBarScrollBehavior,
          shouldUpdateTopAppBarAndBottomAppBarOnScrolling
        )
      }
      LaunchedEffect(showTableOfContentDrawer.value) {
        shouldUpdateTopAppBarAndBottomAppBarOnScrolling.value = !showTableOfContentDrawer.value
      }
      if (showTableOfContentDrawer.value) {
        // Showing the background color on screen so that it look same as navigation drawer.
        val overlayContentDescription = stringResource(android.R.string.untitled)
        Box(
          Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable { showTableOfContentDrawer.update { false } }
            .semantics { contentDescription = overlayContentDescription }
        )
      }
      AnimatedVisibility(
        visible = showTableOfContentDrawer.value,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = Modifier
          .systemBarsPadding()
          .align(Alignment.CenterEnd)
      ) {
        TableDrawerSheet(
          title = state.tableOfContentTitle,
          sections = documentSections.orEmpty(),
          state.selectedWebView,
          showTableOfContentDrawer
        )
      }
    }
  }
}

@Composable
fun OnBackPressed(
  onUserBackPressed: () -> FragmentActivityExtensions.Super,
  navHostController: NavHostController
) {
  // Tracks whether the fragment's BackHandler should be enabled.
  var shouldEnableBackPress by remember { mutableStateOf(true) }
  BackHandler(enabled = shouldEnableBackPress) {
    val result = onUserBackPressed()
    if (result == FragmentActivityExtensions.Super.ShouldCall) {
      // Disable the fragment's BackHandler so that MainActivity's back handler can be triggered.
      shouldEnableBackPress = false
      navHostController.popBackStack()
    } else {
      // Keep the fragment's BackHandler active.
      shouldEnableBackPress = true
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ComposableLambdaParameterNaming")
@Composable
private fun ReaderTopBar(
  state: ReaderScreenState,
  actionMenuItems: List<ActionMenuItem>,
  topAppBarScrollBehavior: TopAppBarScrollBehavior,
  navigationIcon: @Composable () -> Unit,
) {
  if (!state.fullScreenItem.first) {
    KiwixAppBar(
      title = if (state.showTabSwitcher) "" else state.readerScreenTitle,
      navigationIcon = navigationIcon,
      actionMenuItems = actionMenuItems,
      topAppBarScrollBehavior = topAppBarScrollBehavior,
      searchBar =
        searchPlaceHolderIfActive(state.searchPlaceHolderItemForCustomApps)
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderContentLayout(
  state: ReaderScreenState,
  modifier: Modifier = Modifier,
  bottomAppBarScrollBehavior: BottomAppBarScrollBehavior,
  topAppBarScrollBehavior: TopAppBarScrollBehavior,
  shouldUpdateTopAppBarAndBottomAppBarOnScrolling: MutableState<Boolean>,
) {
  Box(modifier = modifier.fillMaxSize()) {
    TabSwitcherAnimated(state)
    if (!state.showTabSwitcher) {
      when {
        state.isNoBookOpenInReader -> NoBookOpenView(state.onOpenLibraryButtonClicked)
        state.fullScreenItem.first -> ShowFullScreenView(state)

        else -> {
          ShowZIMFileContent(
            state,
            bottomAppBarScrollBehavior,
            topAppBarScrollBehavior,
            shouldUpdateTopAppBarAndBottomAppBarOnScrolling
          )
          ShowProgressBarIfZIMFilePageIsLoading(state)
          Column(Modifier.align(Alignment.BottomCenter)) {
            TtsControls(state)
            ShowDonationLayout(state)
            BottomAppBarOfReaderScreen(
              state.bookmarkButtonItem,
              state.previousPageButtonItem,
              state.onHomeButtonClick,
              state.nextPageButtonItem,
              state.tocButtonItem,
              state.shouldShowBottomAppBar,
              bottomAppBarScrollBehavior
            )
          }
        }
      }
    }
  }
}

@Suppress("LongMethod")
@Composable
fun TableDrawerSheet(
  title: String,
  sections: List<DocumentSection>,
  selectedWebView: KiwixWebView?,
  showTableOfContentDrawer: MutableState<Boolean>
) {
  val drawerBackgroundColor = if (isSystemInDarkTheme()) {
    MineShaftGray700
  } else {
    White
  }
  ModalDrawerSheet(
    modifier = Modifier.width(NAVIGATION_DRAWER_WIDTH),
    drawerShape = RectangleShape,
    drawerContainerColor = drawerBackgroundColor
  ) {
    LazyColumn(modifier = Modifier.fillMaxHeight()) {
      item {
        Text(
          text = title.ifEmpty { stringResource(id = R.string.no_section_info) },
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable {
              onTableOfContentHeaderClick(
                selectedWebView,
                showTableOfContentDrawer
              )
            }
            .padding(horizontal = SIXTEEN_DP, vertical = TWELVE_DP)
            .semantics { contentDescription = "${title}${title.hashCode()}" }
        )
      }
      itemsIndexed(sections) { index, section ->
        val paddingStart = (section.level - ONE) * TWELVE
        Text(
          text = section.title,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Light,
            fontSize = LARGE_BODY_TEXT_SIZE
          ),
          modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable {
              onTableOfContentSectionClick(
                selectedWebView,
                index,
                sections,
                showTableOfContentDrawer
              )
            }
            .padding(start = paddingStart.dp, top = EIGHT_DP, bottom = EIGHT_DP, end = SIXTEEN_DP)
            .semantics { contentDescription = "${section.title}$index" }
        )
      }
    }
  }
}

private fun onTableOfContentHeaderClick(
  selectedWebView: KiwixWebView?,
  showTableOfContentDrawer: MutableState<Boolean>
) {
  selectedWebView?.scrollY = ZERO
  showTableOfContentDrawer.update { false }
}

private fun onTableOfContentSectionClick(
  selectedWebView: KiwixWebView?,
  position: Int,
  sections: List<DocumentSection>,
  showTableOfContentDrawer: MutableState<Boolean>
) {
  if (hasItemForPositionInDocumentSectionsList(position, sections)) {
    val targetId = sections[position].id.replace("'", "\\'")
    selectedWebView?.evaluateJavascript(
      "document.getElementById('$targetId')?.scrollIntoView();",
      null
    )
  }
  showTableOfContentDrawer.update { false }
}

private fun hasItemForPositionInDocumentSectionsList(
  position: Int,
  sections: List<DocumentSection>
): Boolean {
  val documentListSize = sections.size
  return when {
    position < 0 -> false
    position >= documentListSize -> false
    else -> true
  }
}

@Composable
private fun TabSwitcherAnimated(state: ReaderScreenState) {
  val transitionSpec = remember {
    slideInVertically(
      initialOffsetY = { -it },
      animationSpec = tween(durationMillis = HIDE_TAB_SWITCHER_DELAY.toInt())
    ) + fadeIn() togetherWith
      slideOutVertically(
        targetOffsetY = { -it },
        animationSpec = tween(durationMillis = HIDE_TAB_SWITCHER_DELAY.toInt())
      ) + fadeOut()
  }

  AnimatedVisibility(
    visible = state.showTabSwitcher,
    enter = transitionSpec.targetContentEnter,
    exit = transitionSpec.initialContentExit,
    modifier = Modifier
      .zIndex(1f)
      .semantics { testTag = TAB_SWITCHER_VIEW_TESTING_TAG },
  ) {
    TabSwitcherView(
      state.kiwixWebViewList,
      state.currentWebViewPosition,
      state.onTabClickListener,
      state.onCloseAllTabs
    )
  }
}

@Composable
private fun searchPlaceHolderIfActive(
  searchPlaceHolderItemForCustomApps: Pair<Boolean, () -> Unit>
): (@Composable () -> Unit)? = if (searchPlaceHolderItemForCustomApps.first) {
  {
    SearchPlaceholder(
      stringResource(R.string.search_label),
      searchPlaceHolderItemForCustomApps.second
    )
  }
} else {
  null
}

@Composable
fun SearchPlaceholder(hint: String, searchPlaceHolderClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        color = Color.Transparent,
        shape = RoundedCornerShape(THREE_DP)
      )
      .border(
        width = 1.5.dp,
        color = colorResource(id = R.color.alabaster_white),
        shape = RoundedCornerShape(THREE_DP)
      )
      .padding(horizontal = FIVE_DP, vertical = FIVE_DP)
      .clickable(onClick = searchPlaceHolderClick),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = hint,
      color = Color.Gray,
      modifier = Modifier.weight(1f),
      fontSize = SEARCH_PLACEHOLDER_TEXT_SIZE
    )
    Spacer(modifier = Modifier.width(TEN_DP))
    Icon(
      painter = Drawable(R.drawable.action_search).toPainter(),
      contentDescription = stringResource(R.string.search_label),
      tint = White
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowZIMFileContent(
  state: ReaderScreenState,
  bottomAppBarScrollBehavior: BottomAppBarScrollBehavior,
  topAppBarScrollBehavior: TopAppBarScrollBehavior,
  shouldUpdateTopAppBarAndBottomAppBarOnScrolling: MutableState<Boolean>
) {
  state.selectedWebView?.let { selectedWebView ->
    key(selectedWebView) {
      DisposableEffect(Unit) {
        val listener = View.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
          val deltaY = (scrollY - oldScrollY).toFloat()
          if (deltaY == 0f || !shouldUpdateTopAppBarAndBottomAppBarOnScrolling.value) return@OnScrollChangeListener
          val topLimit = topAppBarScrollBehavior.state.heightOffsetLimit
          val bottomLimit = bottomAppBarScrollBehavior.state.heightOffsetLimit

          topAppBarScrollBehavior.state.heightOffset =
            (topAppBarScrollBehavior.state.heightOffset - deltaY)
              .coerceIn(topLimit, 0f)

          bottomAppBarScrollBehavior.state.heightOffset =
            (bottomAppBarScrollBehavior.state.heightOffset - deltaY)
              .coerceIn(bottomLimit, 0f)
        }

        selectedWebView.setOnScrollChangeListener(listener)

        onDispose {
          selectedWebView.setOnScrollChangeListener(null)
        }
      }
      AndroidView(
        factory = { context ->
          FrameLayout(context).apply {
            (selectedWebView.parent as? ViewGroup)?.removeView(selectedWebView)
            selectedWebView.layoutParams = FrameLayout.LayoutParams(
              MATCH_PARENT,
              MATCH_PARENT
            )
            addView(selectedWebView)
          }
        },
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@Composable
private fun ShowFullScreenView(state: ReaderScreenState) {
  state.fullScreenItem.second?.let { videoView ->
    AndroidView(factory = { videoView })
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
private fun TtsControls(state: ReaderScreenState) {
  if (state.showTtsControls) {
    Row {
      Button(
        onClick = state.onPauseTtsClick,
        modifier = Modifier
          .weight(1f)
          .alpha(TTS_BUTTONS_CONTROL_ALPHA)
      ) {
        Text(
          text = state.pauseTtsButtonText.uppercase(),
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.width(FOUR_DP))
      Button(
        onClick = state.onStopTtsClick,
        modifier = Modifier
          .weight(1f)
          .alpha(TTS_BUTTONS_CONTROL_ALPHA)
          .semantics { testTag = TTS_CONTROL_STOP_BUTTON_TESTING_TAG }
      ) {
        Text(
          text = stringResource(R.string.stop).uppercase(),
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
private fun BackToTopFab(state: ReaderScreenState) {
  if (state.showBackToTopButton) {
    SmallFloatingActionButton(
      onClick = state.backToTopButtonClick,
      modifier = Modifier.padding(bottom = BACK_TO_TOP_BUTTON_BOTTOM_MARGIN),
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      shape = CircleShape
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
private fun BottomAppBarOfReaderScreen(
  bookmarkButtonItem: Triple<() -> Unit, () -> Unit, Drawable>,
  previousPageButtonItem: Triple<() -> Unit, () -> Unit, Boolean>,
  onHomeButtonClick: () -> Unit,
  nextPageButtonItem: Triple<() -> Unit, () -> Unit, Boolean>,
  tocButtonItem: Pair<Boolean, () -> Unit>,
  shouldShowBottomAppBar: Boolean,
  bottomAppBarScrollBehavior: BottomAppBarScrollBehavior
) {
  if (!shouldShowBottomAppBar) return
  BottomAppBar(
    containerColor = MaterialTheme.colorScheme.onPrimary,
    contentColor = MaterialTheme.colorScheme.onBackground,
    scrollBehavior = bottomAppBarScrollBehavior,
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
        contentDescription = stringResource(R.string.bookmarks),
        testingTag = READER_BOTTOM_BAR_BOOKMARK_BUTTON_TESTING_TAG
      )
      // Back Icon(for going to previous page)
      BottomAppBarButtonIcon(
        onClick = previousPageButtonItem.first,
        onLongClick = previousPageButtonItem.second,
        buttonIcon = Drawable(R.drawable.ic_keyboard_arrow_left_24dp),
        shouldEnable = previousPageButtonItem.third,
        contentDescription = stringResource(R.string.go_to_previous_page),
        testingTag = READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG
      )
      // Home Icon(to open the home page of ZIM file)
      BottomAppBarButtonIcon(
        onClick = onHomeButtonClick,
        buttonIcon = Drawable(R.drawable.action_home),
        contentDescription = stringResource(R.string.menu_home),
        testingTag = READER_BOTTOM_BAR_HOME_BUTTON_TESTING_TAG
      )
      // Forward Icon(for going to next page)
      BottomAppBarButtonIcon(
        onClick = nextPageButtonItem.first,
        onLongClick = nextPageButtonItem.second,
        buttonIcon = Drawable(R.drawable.ic_keyboard_arrow_right_24dp),
        shouldEnable = nextPageButtonItem.third,
        contentDescription = stringResource(R.string.go_to_next_page),
        testingTag = READER_BOTTOM_BAR_NEXT_SCREEN_BUTTON_TESTING_TAG
      )
      // Toggle Icon(to open the table of content in right side bar)
      BottomAppBarButtonIcon(
        shouldEnable = tocButtonItem.first,
        onClick = tocButtonItem.second,
        buttonIcon = Drawable(R.drawable.ic_toc_24dp),
        contentDescription = stringResource(R.string.table_of_contents),
        testingTag = READER_BOTTOM_BAR_TABLE_CONTENT_BUTTON_TESTING_TAG
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
  contentDescription: String,
  testingTag: String
) {
  Box(
    modifier = Modifier
      .size(READER_BOTTOM_APP_BAR_BUTTON_ICON_SIZE + TEN_DP)
      .clip(CircleShape)
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
        enabled = shouldEnable
      )
      .testTag(testingTag),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      buttonIcon.toPainter(),
      contentDescription,
      modifier = Modifier.size(READER_BOTTOM_APP_BAR_BUTTON_ICON_SIZE),
      tint = if (shouldEnable) {
        LocalContentColor.current
      } else {
        LocalContentColor.current.copy(alpha = READER_BOTTOM_APP_BAR_DISABLE_BUTTON_ALPHA)
      }
    )
  }
}

@Composable
private fun ShowDonationLayout(state: ReaderScreenState) {
  if (state.shouldShowDonationPopup) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
      DonationLayout(
        state.appName,
        state.donateButtonClick,
        state.laterButtonClick
      )
    }
  }
}

@Composable
private fun ShowUpdateDialog(
  state: ReaderScreenState,
  navHostController: NavHostController
) {
  if (state.shouldShowUpdatePopup) {
    UpdateDialog(
      onConfirm = { navHostController.navigate("updateScreen") },
      onDismiss = {}
    )
  }
}

@Composable
fun TabSwitcherView(
  webViews: List<KiwixWebView>,
  selectedIndex: Int,
  onTabClickListener: TabClickListener,
  onCloseAllTabs: () -> Unit
) {
  val state = rememberLazyListState()
  Box(modifier = Modifier.fillMaxSize()) {
    LazyRow(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .padding(top = SIXTEEN_DP),
      contentPadding = PaddingValues(horizontal = SIXTEEN_DP, vertical = EIGHT_DP),
      horizontalArrangement = Arrangement.spacedBy(EIGHT_DP),
      state = state
    ) {
      itemsIndexed(webViews, key = { _, item -> item.hashCode() }) { index, webView ->
        val context = LocalContext.current
        val title = remember(webView) {
          webView.title?.fromHtml()?.toString()
            ?: context.getString(R.string.menu_home)
        }

        TabItemView(
          index = index,
          title = title,
          isSelected = index == selectedIndex,
          webView = webView,
          onTabClickListener = onTabClickListener,
        )
      }
    }
    LaunchedEffect(Unit) {
      state.animateScrollToItem(selectedIndex)
    }
    CloseAllTabButton(onCloseAllTabs)
  }
}

@Composable
private fun BoxScope.CloseAllTabButton(onCloseAllTabs: () -> Unit) {
  var isAnimating by remember { mutableStateOf(false) }
  var isDone by remember { mutableStateOf(false) }

  // Animate rotation from 0f to 360f
  val rotation by animateFloatAsState(
    targetValue = if (isAnimating) 360f else 0f,
    animationSpec = tween(durationMillis = 600),
    finishedListener = {
      isDone = true
      isAnimating = false
    }
  )

  // ⏳ Auto-reset to close icon after delay
  LaunchedEffect(isDone) {
    if (isDone) {
      delay(CLOSE_TAB_ICON_ANIMATION_TIMEOUT)
      isDone = false
    }
  }

  FloatingActionButton(
    onClick = {
      isAnimating = true
      onCloseAllTabs()
    },
    modifier = Modifier
      .align(Alignment.BottomCenter)
      .padding(bottom = CLOSE_ALL_TAB_BUTTON_BOTTOM_PADDING)
      .graphicsLayer {
        rotationZ = rotation
      }
      .semantics { testTag = CLOSE_ALL_TABS_BUTTON_TESTING_TAG }
      .clickable(
        enabled = !isAnimating,
        onClick = {
          isAnimating = true
          onCloseAllTabs()
        }
      ),
    containerColor = DenimBlue800,
    contentColor = White
  ) {
    Icon(
      painter = painterResource(
        id = if (isDone) {
          R.drawable.ic_done_white_24dp
        } else {
          R.drawable.ic_close_black_24dp
        }
      ),
      contentDescription = stringResource(R.string.close_all_tabs)
    )
  }
}

@Composable
fun TabItemView(
  index: Int,
  title: String,
  isSelected: Boolean,
  webView: KiwixWebView,
  modifier: Modifier = Modifier,
  onTabClickListener: TabClickListener
) {
  val cardElevation = if (isSelected) EIGHT_DP else TWO_DP
  val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
  val (cardWidth, cardHeight) = getTabCardSize(toolbarHeightDp = KIWIX_TOOLBAR_HEIGHT)
  Box(modifier = modifier) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .padding(horizontal = EIGHT_DP, vertical = FOUR_DP)
        .width(cardWidth)
    ) {
      TabItemHeader(title, index, onTabClickListener)
      TabItemCard(
        webView,
        cardWidth,
        cardHeight,
        onTabClickListener,
        borderColor,
        cardElevation,
        index
      )
    }
  }
}

@Composable
private fun TabItemHeader(
  title: String,
  index: Int,
  onTabClickListener: TabClickListener
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
        .padding(end = FOUR_DP)
        .weight(1f)
        .semantics { testTag = TAB_TITLE_TESTING_TAG },
      style = MaterialTheme.typography.labelSmall
    )
    IconButton(
      onClick = { onTabClickListener.onCloseTab(index) },
      modifier = Modifier.size(CLOSE_TAB_ICON_SIZE)
    ) {
      Icon(
        painter = painterResource(id = R.drawable.ic_clear_white_24dp),
        contentDescription = stringResource(R.string.close_tab) + index
      )
    }
  }
}

@Composable
private fun TabItemCard(
  webView: KiwixWebView,
  cardWidth: Dp,
  cardHeight: Dp,
  onTabClickListener: TabClickListener,
  borderColor: Color,
  elevation: Dp,
  index: Int
) {
  Card(
    elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    border = BorderStroke(ONE_DP, borderColor),
    shape = MaterialTheme.shapes.extraSmall,
    modifier = Modifier
      .width(cardWidth)
      .height(cardHeight)
      .semantics { hideFromAccessibility() }
  ) {
    AndroidView(
      factory = { context ->
        FrameLayout(context).apply {
          (webView.parent as? ViewGroup)?.removeView(webView)
          addView(webView)
          val clickableView = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            // Prevent clicking inside the webView when tabs are active.
            setOnClickListener { onTabClickListener.onSelectTab(index) }
            contentDescription = "${webView.contentDescription}${webView.hashCode()}"
          }
          addView(clickableView)
        }
      },
      modifier = Modifier
        .fillMaxSize()
        .semantics { hideFromAccessibility() }
    )
  }
}

@Composable
fun getTabCardSize(toolbarHeightDp: Dp): Pair<Dp, Dp> {
  val windowSize = LocalWindowInfo.current.containerSize
  val density = LocalDensity.current

  val screenWidth = with(density) { windowSize.width.toDp() }
  val screenHeight = with(density) { windowSize.height.toDp() }

  val cardWidth = screenWidth / 2
  val cardHeight = ((screenHeight - toolbarHeightDp) / 2).coerceAtLeast(HUNDERED.dp)

  return cardWidth to cardHeight
}

interface TabClickListener {
  fun onSelectTab(position: Int)
  fun onCloseTab(position: Int)
}

data class DocumentSection(var title: String, var id: String, var level: Int)
