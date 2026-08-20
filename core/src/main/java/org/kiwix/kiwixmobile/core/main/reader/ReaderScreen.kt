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
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.TtsControlsItem
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.DpSize
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BackPressActivityExtensions
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.BookmarkButtonItem
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.BackToTopButtonClick
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.BookmarkClicked
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.BookmarkLongClicked
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.CloseAllTabs
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.CloseTab
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.CloseTocDrawer
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.DonateButtonClick
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.DonateLaterButtonClick
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.HomeClicked
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.NextClicked
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.NextLongClicked
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.OpenLibrary
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.OpenSearch
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.OpenTocDrawer
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.PauseTts
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.ChangeTtsSpeed
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.RewindTts10s
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.ForwardTts10s
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.SeekTts
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.ShowVoiceSelectionDialog
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.DismissVoiceSelectionDialog
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.SelectTtsVoice
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.ShowTtsControlsOverlay
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.DismissTtsControlsOverlay
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.PreviousClicked
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.PreviousLongClicked
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.SelectTab
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.StopTts
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderUiState
import org.kiwix.kiwixmobile.core.main.reader.helper.TabsManager
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.FindInPageAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixButton
import org.kiwix.kiwixmobile.core.ui.components.KiwixFloatingActionButton
import org.kiwix.kiwixmobile.core.ui.components.KiwixSnackbarHost
import org.kiwix.kiwixmobile.core.ui.components.KiwixWebViewWithAppBarScrolling
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.ui.components.ProgressBarStyle
import org.kiwix.kiwixmobile.core.ui.components.TWELVE
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem.Drawable
import org.kiwix.kiwixmobile.core.ui.models.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.DenimBlue800
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.CLOSE_ALL_TAB_BUTTON_BOTTOM_PADDING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.CLOSE_TAB_ICON_ANIMATION_TIMEOUT
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.CLOSE_TAB_ICON_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIFTY_SIX_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOURTEEN_DP
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
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIX_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.THREE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWELVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWENTY_EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWENTY_FOUR_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWENTY_TWO_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWO_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ZERO_DP
import kotlin.math.abs
import java.util.Locale
import org.kiwix.kiwixmobile.core.utils.HUNDERED
import org.kiwix.kiwixmobile.core.utils.StyleUtils.fromHtml
import org.kiwix.kiwixmobile.core.utils.ZERO

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
const val TTS_CONTROL_SPEED_BUTTON_TESTING_TAG = "ttsControlSpeedButtonTestingTag"
const val TTS_CONTROL_PLAY_PAUSE_BUTTON_TESTING_TAG = "ttsControlPlayPauseButtonTestingTag"
const val TTS_CONTROL_REWIND_10_BUTTON_TESTING_TAG = "ttsControlRewind10ButtonTestingTag"
const val TTS_CONTROL_FORWARD_10_BUTTON_TESTING_TAG = "ttsControlForward10ButtonTestingTag"
const val TTS_CONTROL_VOICE_BUTTON_TESTING_TAG = "ttsControlVoiceButtonTestingTag"
const val TTS_CONTROL_SLIDER_TESTING_TAG = "ttsControlSliderTestingTag"
const val TTS_VOICE_SELECTION_DIALOG_TESTING_TAG = "ttsVoiceSelectionDialogTestingTag"
const val TTS_FLOATING_SPEAKER_BUTTON_TESTING_TAG = "ttsFloatingSpeakerButtonTestingTag"
const val TTS_CONTROLS_OVERLAY_DISMISS_TESTING_TAG = "ttsControlsOverlayDismissTestingTag"

val CYCLIC_TTS_SPEEDS = listOf(1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 0.5f, 0.75f)
private const val TTS_SPEED_TOLERANCE = 0.01f

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ComposableLambdaParameterNaming", "LongMethod", "LongParameterList")
@Composable
fun ReaderScreen(
  state: ReaderUiState,
  snackBarHost: SnackbarHostState,
  onReaderAction: (ReaderAction) -> Unit,
  actionMenuItems: List<ActionMenuItem>,
  onUserBackPressed: suspend () -> BackPressActivityExtensions.Super,
  navHostController: NavHostController,
  mainActivityBottomAppBarScrollBehaviour: BottomAppBarScrollBehavior?,
  navigationIcon: @Composable () -> Unit
) {
  // For managing the scroll event handling of webView.
  val shouldUpdateTopAppBarAndBottomAppBarOnScrolling = rememberSaveable { mutableStateOf(true) }
  val topAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  val bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
  LaunchedEffect(bottomAppBarScrollBehavior.state.heightOffset) {
    mainActivityBottomAppBarScrollBehaviour?.state?.heightOffset =
      bottomAppBarScrollBehavior.state.heightOffset
  }
  KiwixTheme {
    Box(Modifier.fillMaxSize()) {
      Scaffold(
        snackbarHost = { KiwixSnackbarHost(snackbarHostState = snackBarHost) },
        topBar = {
          ReaderTopBar(
            state,
            actionMenuItems,
            topAppBarScrollBehavior,
            onReaderAction,
            navigationIcon
          )
        },
        bottomBar = {
          if (!state.showNoBookOpenInReader) {
            BottomAppBarOfReaderScreen(
              state.bookmarkButtonItem,
              state.isPreviousPageButtonEnable,
              state.isNextPageButtonEnable,
              state.isTocButtonEnable,
              state.showBottomBar,
              bottomAppBarScrollBehavior,
              onReaderAction
            )
          }
        },
        floatingActionButton = { BackToTopFab(state.showBackToTopButton, onReaderAction) },
        modifier = Modifier
          .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
          .nestedScroll(bottomAppBarScrollBehavior.nestedScrollConnection)
          .semantics { testTag = READER_SCREEN_TESTING_TAG }
      ) { paddingValues ->
        OnBackPressed(onUserBackPressed, navHostController)
        ReaderContentLayout(
          state,
          onReaderAction,
          Modifier.padding(paddingValues),
          bottomAppBarScrollBehavior,
          topAppBarScrollBehavior,
          shouldUpdateTopAppBarAndBottomAppBarOnScrolling
        )
      }
      LaunchedEffect(state.showTableOfContentDrawer) {
        shouldUpdateTopAppBarAndBottomAppBarOnScrolling.value = !state.showTableOfContentDrawer
      }
      if (state.showTableOfContentDrawer) {
        // Showing the background color on screen so that it look same as navigation drawer.
        val overlayContentDescription = stringResource(android.R.string.untitled)
        Box(
          Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable { onReaderAction(CloseTocDrawer) }
            .semantics { contentDescription = overlayContentDescription }
        )
      }
      AnimatedVisibility(
        visible = state.showTableOfContentDrawer,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = Modifier.align(Alignment.CenterEnd)
      ) {
        TableDrawerSheet(
          title = state.tableOfContentTitle,
          sections = state.documentSections,
          state.tabsState.currentWebView
        ) { onReaderAction(CloseTocDrawer) }
      }
    }
  }
}

@Composable
fun OnBackPressed(
  onUserBackPressed: suspend () -> BackPressActivityExtensions.Super,
  navHostController: NavHostController
) {
  // Tracks whether the back press handler should be enabled.
  var shouldEnableBackPress by rememberSaveable { mutableStateOf(true) }
  val coroutineScope = rememberCoroutineScope()
  BackHandler(enabled = shouldEnableBackPress) {
    coroutineScope.launch {
      val result = onUserBackPressed()
      if (result == BackPressActivityExtensions.Super.ShouldCall) {
        // Disable the back press handler so that MainActivity's back handler can be triggered.
        shouldEnableBackPress = false
        navHostController.popBackStack()
      } else {
        // Keep the back press handler active.
        shouldEnableBackPress = true
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ComposableLambdaParameterNaming")
@Composable
private fun ReaderTopBar(
  state: ReaderUiState,
  actionMenuItems: List<ActionMenuItem>,
  topAppBarScrollBehavior: TopAppBarScrollBehavior,
  onReaderAction: (ReaderAction) -> Unit,
  navigationIcon: @Composable () -> Unit,
) {
  if (state.shouldShowFullScreen) return
  if (state.findInPageUiState.visible) {
    FindInPageAppBar(
      query = state.findInPageUiState.query,
      resultText = state.findInPageUiState.resultText,
      onQueryChange = { onReaderAction(ReaderAction.FindInPageQueryChanged(it)) },
      onPreviousClick = { onReaderAction(ReaderAction.FindInPagePreviousClicked) },
      onNextClick = { onReaderAction(ReaderAction.FindInPageNextClicked) },
      onCloseClick = { onReaderAction(ReaderAction.FindInPageCloseClicked) }
    )
  } else {
    KiwixAppBar(
      title = if (state.showTabSwitcher) "" else state.title,
      navigationIcon = navigationIcon,
      actionMenuItems = actionMenuItems,
      topAppBarScrollBehavior = topAppBarScrollBehavior,
      searchBar =
        searchPlaceHolderIfActive(state.searchPlaceHolderItemForBrandedApps, onReaderAction)
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderContentLayout(
  state: ReaderUiState,
  onReaderAction: (ReaderAction) -> Unit,
  modifier: Modifier = Modifier,
  bottomAppBarScrollBehavior: BottomAppBarScrollBehavior,
  topAppBarScrollBehavior: TopAppBarScrollBehavior,
  shouldUpdateTopAppBarAndBottomAppBarOnScrolling: MutableState<Boolean>,
) {
  Box(modifier = modifier.fillMaxSize()) {
    TabSwitcherAnimated(state, onReaderAction)
    if (!state.showTabSwitcher) {
      when {
        state.showNoBookOpenInReader -> NoBookOpenView { onReaderAction(OpenLibrary) }
        state.shouldShowFullScreen -> ShowFullScreenView(state.videoView)

        else -> {
          state.tabsState.currentWebView?.let { selectedWebView ->
            KiwixWebViewWithAppBarScrolling(
              selectedWebView,
              topAppBarScrollBehavior,
              bottomAppBarScrollBehavior,
              shouldUpdateTopAppBarAndBottomAppBarOnScrolling
            )
          }
          ShowProgressBarIfZIMFilePageIsLoading(state)
          if (state.ttsControlsItem.isTtsPlaying && state.ttsControlsItem.showTtsControlsOverlay) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable { onReaderAction(DismissTtsControlsOverlay) }
                .semantics { testTag = TTS_CONTROLS_OVERLAY_DISMISS_TESTING_TAG }
            )
          }
          Column(Modifier.align(Alignment.BottomCenter)) {
            TtsControls(state, onReaderAction)
            ShowDonationLayout(state, onReaderAction)
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
  closeTocClick: () -> Unit
) {
  ModalDrawerSheet(
    modifier = Modifier.width(NAVIGATION_DRAWER_WIDTH),
    drawerShape = RectangleShape,
    drawerContainerColor = MaterialTheme.colorScheme.surface
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
                closeTocClick
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
                closeTocClick
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
  closeTocClick: () -> Unit
) {
  selectedWebView?.scrollY = ZERO
  closeTocClick.invoke()
}

private fun onTableOfContentSectionClick(
  selectedWebView: KiwixWebView?,
  position: Int,
  sections: List<DocumentSection>,
  closeTocClick: () -> Unit
) {
  if (hasItemForPositionInDocumentSectionsList(position, sections)) {
    val targetId = sections[position].id.replace("'", "\\'")
    selectedWebView?.evaluateJavascript(
      "document.getElementById('$targetId')?.scrollIntoView();",
      null
    )
  }
  closeTocClick.invoke()
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
private fun TabSwitcherAnimated(state: ReaderUiState, onReaderAction: (ReaderAction) -> Unit) {
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
      state.tabsState,
      onReaderAction
    )
  }
}

@Composable
private fun searchPlaceHolderIfActive(
  searchPlaceHolderItemForBrandedApps: Boolean,
  onReaderAction: (ReaderAction) -> Unit
): (@Composable () -> Unit)? = if (searchPlaceHolderItemForBrandedApps) {
  {
    SearchPlaceholder(
      stringResource(R.string.search_label)
    ) { onReaderAction(OpenSearch()) }
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
      painter = IconItem.Drawable(R.drawable.action_search).toPainter(),
      contentDescription = stringResource(R.string.search_label),
      tint = White
    )
  }
}

@Composable
private fun ShowFullScreenView(videoView: FrameLayout?) {
  videoView?.let { view ->
    AndroidView(factory = { view })
  }
}

@Composable
private fun BoxScope.ShowProgressBarIfZIMFilePageIsLoading(state: ReaderUiState) {
  if (state.loading) {
    ContentLoadingProgressBar(
      modifier = Modifier.align(Alignment.TopCenter),
      progressBarStyle = ProgressBarStyle.HORIZONTAL,
      progress = state.progress
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

@Suppress("MagicNumber")
private fun formatTime(millis: Long): String {
  val totalSeconds = (millis / 1000).coerceAtLeast(0)
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "MagicNumber")
@Composable
private fun TtsControls(state: ReaderUiState, onReaderAction: (ReaderAction) -> Unit) {
  val ttsItem = state.ttsControlsItem

  if (ttsItem.isTtsPlaying) {
    if (ttsItem.showTtsControlsOverlay) {
      TtsControlsCard(ttsItem, onReaderAction)
    } else {
      TtsFloatingActionButton(onReaderAction)
    }
  }

  if (ttsItem.showVoiceSelectionDialog) {
    VoiceSelectionDialog(
      voices = ttsItem.availableVoices,
      selectedVoice = ttsItem.selectedVoiceName,
      onVoiceSelected = { onReaderAction(SelectTtsVoice(it)) },
      onDismiss = { onReaderAction(DismissVoiceSelectionDialog) }
    )
  }
}

@Composable
private fun TtsControlsCard(
  ttsItem: TtsControlsItem,
  onReaderAction: (ReaderAction) -> Unit
) {
  var isDragging by remember { mutableStateOf(false) }
  var dragProgress by remember { mutableFloatStateOf(0f) }

  val totalDurationMs = ttsItem.totalDurationMs
  val liveProgress = if (totalDurationMs > 0L) {
    (ttsItem.currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
  } else {
    0f
  }

  val displayProgress = if (isDragging) dragProgress else liveProgress
  val displayPositionMs =
    if (isDragging) (dragProgress * totalDurationMs).toLong() else ttsItem.currentPositionMs

  Card(
    shape = RoundedCornerShape(TWENTY_FOUR_DP),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = EIGHT_DP),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = SIXTEEN_DP, vertical = EIGHT_DP)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = SIXTEEN_DP, vertical = TWELVE_DP),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      TtsProgressSlider(
        displayProgress = displayProgress,
        totalDurationMs = totalDurationMs,
        onSeek = { targetMs -> onReaderAction(SeekTts(targetMs)) },
        onDraggingChanged = { dragging, progress ->
          isDragging = dragging
          dragProgress = progress
        }
      )
      TtsTimeLabelsRow(displayPositionMs = displayPositionMs, totalDurationMs = totalDurationMs)
      Spacer(modifier = Modifier.height(EIGHT_DP))
      TtsControlButtonsRow(ttsItem = ttsItem, onReaderAction = onReaderAction)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TtsProgressSlider(
  displayProgress: Float,
  totalDurationMs: Long,
  onSeek: (Long) -> Unit,
  onDraggingChanged: (Boolean, Float) -> Unit
) {
  var sliderValue by remember(displayProgress) { mutableFloatStateOf(displayProgress) }

  val sliderColors = SliderDefaults.colors(
    thumbColor = MaterialTheme.colorScheme.primary,
    activeTrackColor = MaterialTheme.colorScheme.primary,
    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
  )
  Slider(
    value = sliderValue,
    onValueChange = { newProgress ->
      sliderValue = newProgress
      onDraggingChanged(true, newProgress)
    },
    onValueChangeFinished = {
      val targetMs = (sliderValue * totalDurationMs).toLong()
      onDraggingChanged(false, sliderValue)
      onSeek(targetMs)
    },
    modifier = Modifier
      .fillMaxWidth()
      .semantics { testTag = TTS_CONTROL_SLIDER_TESTING_TAG },
    thumb = {
      SliderDefaults.Thumb(
        interactionSource = remember { MutableInteractionSource() },
        colors = sliderColors,
        thumbSize = DpSize(FOURTEEN_DP, FOURTEEN_DP)
      )
    },
    track = { sliderState ->
      SliderDefaults.Track(
        sliderState = sliderState,
        colors = sliderColors,
        drawStopIndicator = null,
        thumbTrackGapSize = ZERO_DP,
        trackInsideCornerSize = ZERO_DP,
        modifier = Modifier.height(FOUR_DP)
      )
    }
  )
}

@Composable
private fun TtsTimeLabelsRow(displayPositionMs: Long, totalDurationMs: Long) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = FOUR_DP),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = formatTime(displayPositionMs),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = if (totalDurationMs > 0L) formatTime(totalDurationMs) else "--:--",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerTooltip(
  tooltipText: String,
  content: @Composable () -> Unit
) {
  TooltipBox(
    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
    tooltip = {
      PlainTooltip(
        shape = RoundedCornerShape(EIGHT_DP),
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface
      ) {
        Text(
          text = tooltipText,
          style = MaterialTheme.typography.bodySmall
        )
      }
    },
    state = rememberTooltipState()
  ) {
    content()
  }
}

@Suppress("LongMethod")
@Composable
private fun TtsControlButtonsRow(
  ttsItem: TtsControlsItem,
  onReaderAction: (ReaderAction) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 1. Speed button (cyclic)
    PlayerTooltip(stringResource(R.string.tts_speech_speed)) {
      Surface(
        onClick = {
          val currentIndex =
            CYCLIC_TTS_SPEEDS.indexOfFirst { abs(it - ttsItem.ttsSpeed) < TTS_SPEED_TOLERANCE }
          val nextSpeed = if (currentIndex != -1) {
            CYCLIC_TTS_SPEEDS[(currentIndex + 1) % CYCLIC_TTS_SPEEDS.size]
          } else {
            1.0f
          }
          onReaderAction(ChangeTtsSpeed(nextSpeed))
        },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { testTag = TTS_CONTROL_SPEED_BUTTON_TESTING_TAG }
      ) {
        val speedText = if (ttsItem.ttsSpeed % 1.0f == 0f) {
          "${ttsItem.ttsSpeed.toInt()}x"
        } else {
          "${ttsItem.ttsSpeed}x"
        }
        Text(
          text = speedText,
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.labelMedium,
          modifier = Modifier.padding(horizontal = TWELVE_DP, vertical = SIX_DP)
        )
      }
    }

    // 2. Rewind 10s button
    PlayerTooltip(stringResource(R.string.tts_rewind_10_seconds)) {
      IconButton(
        onClick = { onReaderAction(RewindTts10s) },
        modifier = Modifier.semantics { testTag = TTS_CONTROL_REWIND_10_BUTTON_TESTING_TAG }
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_replay_10),
          contentDescription = "-10s",
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(TWENTY_FOUR_DP)
        )
      }
    }

    // 3. Center Play / Pause button
    val playPauseTooltip = stringResource(
      if (ttsItem.isTtsPaused) R.string.tts_resume else R.string.tts_pause
    )
    PlayerTooltip(playPauseTooltip) {
      Surface(
        onClick = { onReaderAction(PauseTts) },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
          .size(FIFTY_SIX_DP)
          .semantics { testTag = TTS_CONTROL_PLAY_PAUSE_BUTTON_TESTING_TAG }
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            painter = painterResource(
              id = if (ttsItem.isTtsPaused) {
                R.drawable.ic_baseline_play
              } else {
                R.drawable.ic_baseline_pause
              }
            ),
            contentDescription = ttsItem.contentDescription,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(TWENTY_EIGHT_DP)
          )
        }
      }
    }

    // 4. Forward 10s button
    PlayerTooltip(stringResource(R.string.tts_forward_10_seconds)) {
      IconButton(
        onClick = { onReaderAction(ForwardTts10s) },
        modifier = Modifier.semantics { testTag = TTS_CONTROL_FORWARD_10_BUTTON_TESTING_TAG }
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_forward_10),
          contentDescription = "+10s",
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(TWENTY_FOUR_DP)
        )
      }
    }

    // 5. Voice button (Equalizer)
    PlayerTooltip(stringResource(R.string.tts_select_voice)) {
      IconButton(
        onClick = { onReaderAction(ShowVoiceSelectionDialog) },
        modifier = Modifier.semantics { testTag = TTS_CONTROL_VOICE_BUTTON_TESTING_TAG }
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_graphic_eq),
          contentDescription = stringResource(R.string.menu_read_aloud),
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(TWENTY_FOUR_DP)
        )
      }
    }

    // 6. Stop button
    PlayerTooltip(stringResource(R.string.stop)) {
      IconButton(
        onClick = { onReaderAction(StopTts) },
        modifier = Modifier.semantics { testTag = TTS_CONTROL_STOP_BUTTON_TESTING_TAG }
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_stop_square_outline),
          contentDescription = stringResource(R.string.stop),
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(TWENTY_TWO_DP)
        )
      }
    }
  }
}

@Composable
private fun TtsFloatingActionButton(onReaderAction: (ReaderAction) -> Unit) {
  var offsetX by remember { mutableFloatStateOf(0f) }
  var offsetY by remember { mutableFloatStateOf(0f) }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = SIXTEEN_DP, vertical = EIGHT_DP),
    contentAlignment = Alignment.BottomStart
  ) {
    PlayerTooltip(stringResource(R.string.tts_controls)) {
      FloatingActionButton(
        onClick = { onReaderAction(ShowTtsControlsOverlay) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
          .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
          .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
              change.consume()
              offsetX += dragAmount.x
              offsetY += dragAmount.y
            }
          }
          .semantics { testTag = TTS_FLOATING_SPEAKER_BUTTON_TESTING_TAG }
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_volume_up),
          contentDescription = stringResource(R.string.menu_read_aloud),
          modifier = Modifier.size(TWENTY_FOUR_DP)
        )
      }
    }
  }
}

@Suppress("LongMethod")
@Composable
private fun VoiceSelectionDialog(
  voices: List<String>,
  selectedVoice: String?,
  onVoiceSelected: (String) -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    textContentColor = MaterialTheme.colorScheme.onSurface,
    title = {
      Text(
        text = stringResource(R.string.menu_read_aloud),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      if (voices.isEmpty()) {
        Text(
          text = stringResource(R.string.tts_not_enabled),
          style = MaterialTheme.typography.bodyMedium
        )
      } else {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
          itemsIndexed(voices) { index, voiceName ->
            val isSelected = if (!selectedVoice.isNullOrBlank()) {
              voiceName.equals(selectedVoice, ignoreCase = true) ||
                voiceName.substringBefore("-local").substringBefore("-network")
                  .equals(
                    selectedVoice.substringBefore("-local").substringBefore("-network"),
                    ignoreCase = true
                  )
            } else {
              index == 0
            }
            val displayName = "Voice ${index + 1}"
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onVoiceSelected(voiceName) }
                .padding(vertical = EIGHT_DP, horizontal = FOUR_DP),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = isSelected,
                onClick = { onVoiceSelected(voiceName) },
                colors = RadioButtonDefaults.colors(
                  selectedColor = MaterialTheme.colorScheme.primary
                )
              )
              Spacer(modifier = Modifier.width(EIGHT_DP))
              Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(text = stringResource(R.string.cancel))
      }
    },
    modifier = Modifier.semantics { testTag = TTS_VOICE_SELECTION_DIALOG_TESTING_TAG }
  )
}

@Composable
private fun BackToTopFab(showBackToTop: Boolean, onReaderAction: (ReaderAction) -> Unit) {
  if (!showBackToTop) return
  KiwixFloatingActionButton(
    icon = Drawable(R.drawable.ic_arrow_upward_24dp).toPainter(),
    onClick = { onReaderAction(BackToTopButtonClick) },
    contentDescription = stringResource(R.string.pref_back_to_top),
    shouldPulse = true
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomAppBarOfReaderScreen(
  bookmarkButtonItem: BookmarkButtonItem,
  isPreviousPageButtonEnable: Boolean,
  isNextPageButtonEnable: Boolean,
  isTocButtonEnable: Boolean,
  shouldShowBottomAppBar: Boolean,
  bottomAppBarScrollBehavior: BottomAppBarScrollBehavior,
  onReaderAction: (ReaderAction) -> Unit
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
        onClick = { onReaderAction(BookmarkClicked) },
        onLongClick = { onReaderAction(BookmarkLongClicked) },
        buttonIcon = bookmarkButtonItem.icon,
        contentDescription = stringResource(R.string.bookmarks),
        testingTag = READER_BOTTOM_BAR_BOOKMARK_BUTTON_TESTING_TAG,
        modifier = Modifier.semantics {
          selected = bookmarkButtonItem.isBookmarked
        }
      )
      // Back Icon(for going to previous page)
      BottomAppBarButtonIcon(
        onClick = { onReaderAction(PreviousClicked) },
        onLongClick = { onReaderAction(PreviousLongClicked) },
        buttonIcon = Drawable(R.drawable.ic_keyboard_arrow_left_24dp),
        shouldEnable = isPreviousPageButtonEnable,
        contentDescription = stringResource(R.string.go_to_previous_page),
        testingTag = READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG
      )
      // Home Icon(to open the home page of ZIM file)
      BottomAppBarButtonIcon(
        onClick = { onReaderAction(HomeClicked) },
        buttonIcon = Drawable(R.drawable.action_home),
        contentDescription = stringResource(R.string.menu_home),
        testingTag = READER_BOTTOM_BAR_HOME_BUTTON_TESTING_TAG
      )
      // Forward Icon(for going to next page)
      BottomAppBarButtonIcon(
        onClick = { onReaderAction(NextClicked) },
        onLongClick = { onReaderAction(NextLongClicked) },
        buttonIcon = Drawable(R.drawable.ic_keyboard_arrow_right_24dp),
        shouldEnable = isNextPageButtonEnable,
        contentDescription = stringResource(R.string.go_to_next_page),
        testingTag = READER_BOTTOM_BAR_NEXT_SCREEN_BUTTON_TESTING_TAG
      )
      // Toggle Icon(to open the table of content in right side bar)
      BottomAppBarButtonIcon(
        shouldEnable = isTocButtonEnable,
        onClick = { onReaderAction(OpenTocDrawer) },
        buttonIcon = Drawable(R.drawable.ic_toc_24dp),
        contentDescription = stringResource(R.string.table_of_contents),
        testingTag = READER_BOTTOM_BAR_TABLE_CONTENT_BUTTON_TESTING_TAG
      )
    }
  }
}

@Composable
private fun BottomAppBarButtonIcon(
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
  buttonIcon: IconItem,
  shouldEnable: Boolean = true,
  contentDescription: String,
  testingTag: String
) {
  Box(
    modifier = modifier
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
private fun ShowDonationLayout(state: ReaderUiState, onReaderAction: (ReaderAction) -> Unit) {
  if (state.showDonationPopup) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
      DonationLayout(
        state.appName,
        { onReaderAction(DonateButtonClick) },
        { onReaderAction(DonateLaterButtonClick) }
      )
    }
  }
}

@Composable
fun TabSwitcherView(
  tabsState: TabsManager.TabsState,
  onReaderAction: (ReaderAction) -> Unit
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
      itemsIndexed(tabsState.webViews, key = { _, item -> item.hashCode() }) { index, webView ->
        val context = LocalContext.current
        val title = remember(webView) {
          webView.title?.fromHtml()?.toString()
            ?: context.getString(R.string.menu_home)
        }

        TabItemView(
          index = index,
          title = title,
          isSelected = index == tabsState.selectedIndex,
          webView = webView,
          onReaderAction = onReaderAction,
        )
      }
    }
    LaunchedEffect(Unit) {
      state.animateScrollToItem(tabsState.selectedIndex)
    }
    CloseAllTabButton { onReaderAction(CloseAllTabs) }
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
  onReaderAction: (ReaderAction) -> Unit
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
      TabItemHeader(title, index, onReaderAction)
      TabItemCard(
        webView,
        cardWidth,
        cardHeight,
        onReaderAction,
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
  onReaderAction: (ReaderAction) -> Unit
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
      onClick = { onReaderAction(CloseTab(index)) },
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
  onReaderAction: (ReaderAction) -> Unit,
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
            setOnClickListener { onReaderAction(SelectTab(index)) }
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

data class DocumentSection(var title: String, var id: String, var level: Int)
