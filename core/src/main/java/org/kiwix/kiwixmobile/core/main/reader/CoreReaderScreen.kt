package org.kiwix.kiwixmobile.core.main.reader

import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderEffect
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SearchItemToOpen
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.utils.TAG_FILE_SEARCHED

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoreReaderScreen(
  viewModel: CoreReaderViewModel,
  actionMenuItems: List<ActionMenuItem>,
  showTableOfContentDrawer: MutableState<Boolean>,
  documentSections: MutableList<DocumentSection>?,
  onUserBackPressed: () -> FragmentActivityExtensions.Super,
  navHostController: NavHostController,
  mainActivityBottomAppBarScrollBehaviour: BottomAppBarScrollBehavior?,
  navigationIcon: @Composable () -> Unit
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val backStackEntry = navHostController.currentBackStackEntry
  LaunchedEffect(backStackEntry) {
    backStackEntry
      ?.savedStateHandle
      ?.getStateFlow<SearchItemToOpen?>(
        TAG_FILE_SEARCHED,
        null
      )
      ?.collect { item ->
        item ?: return@collect

        viewModel.pendingSearchItemManager.store(item)
        viewModel.emitEffect(ReaderEffect.ConsumeSavedStateHandle(listOf(TAG_FILE_SEARCHED to SearchItemToOpen::class.java)))
      }
  }

  val readerScreenState = remember(uiState) {
    ReaderScreenState(
      snackBarHostState = snackbarHostState,
      isNoBookOpenInReader = false,
      onOpenLibraryButtonClicked = { viewModel.onAction(CoreReaderViewModel.ReaderAction.OpenLibrary) },
      pageLoadingItem = uiState.loading to uiState.progress,
      shouldShowDonationPopup = false,
      fullScreenItem = uiState.shouldShowFullScreen to uiState.videoView,
      showBackToTopButton = uiState.showBackToTopButton,
      backToTopButtonClick = {},
      showTtsControls = uiState.showTtsControls,
      onPauseTtsClick = {},
      pauseTtsButtonText = "",
      onStopTtsClick = {},
      kiwixWebViewList = uiState.kiwixWebViews,
      bookmarkButtonItem = Triple(
        { viewModel.onAction(CoreReaderViewModel.ReaderAction.BookmarkClicked) },
        { viewModel.onAction(CoreReaderViewModel.ReaderAction.BookmarkLongClicked) },
        org.kiwix.kiwixmobile.core.ui.models.IconItem.Drawable(0)
      ),
      previousPageButtonItem = Triple(
        { viewModel.onAction(CoreReaderViewModel.ReaderAction.PreviousClicked) },
        { /* show backward history */ },
        false
      ),
      onHomeButtonClick = { viewModel.onAction(CoreReaderViewModel.ReaderAction.HomeClicked) },
      nextPageButtonItem = Triple(
        { viewModel.onAction(CoreReaderViewModel.ReaderAction.NextClicked) },
        { /* show forward history */ },
        false
      ),
      tocButtonItem = uiState.showBottomBar to { /* open TOC */ },
      onCloseAllTabs = { viewModel.onAction(CoreReaderViewModel.ReaderAction.CloseAllTabs) },
      shouldShowBottomAppBar = uiState.showBottomBar,
      selectedWebView = uiState.kiwixWebViews.getOrNull(uiState.selectedWebViewIndex),
      readerScreenTitle = uiState.title,
      showTabSwitcher = uiState.showTabSwitcher,
      currentWebViewPosition = uiState.selectedWebViewIndex,
      onTabClickListener = object : TabClickListener {
        override fun onSelectTab(position: Int) {
          viewModel.onAction(CoreReaderViewModel.ReaderAction.SelectTab(position))
        }

        override fun onCloseTab(position: Int) {
          viewModel.onAction(CoreReaderViewModel.ReaderAction.CloseTab(position))
        }
      },
      searchPlaceHolderItemForBrandedApps = false to {
        viewModel.openSearch(searchString = "", isOpenedFromTabView = false, false)
      },
      appName = "",
      donateButtonClick = {},
      laterButtonClick = {},
      tableOfContentTitle = ""
    )
  }

  // Collect one-shot effects from ViewModel
  // LaunchedEffect(viewModel.effects) {
  //   viewModel.effects.collect { effect ->
  //     when (effect) {
  //       is CoreReaderViewModel.ReaderEffect.ShowSnackbar -> {
  //         // val message = stringResource(effect.message)
  //         // val actionLabel = stringResource(org.kiwix.kiwixmobile.core.R.string.open)
  //         // val result = snackbarHostState.snack(message = message, actionLabel = actionLabel, duration = SnackbarDuration.Short)
  //         // if (result == SnackbarResult.ActionPerformed) {
  //         //   effect.actionClick()
  //         // }
  //       }
  //
  //       else -> {
  //         // Other effects (OpenLibrary, Enable/Disable sidebar) handled later
  //       }
  //     }
  //   }
  // }
  ReaderScreen(
    state = readerScreenState,
    actionMenuItems = actionMenuItems,
    showTableOfContentDrawer = showTableOfContentDrawer,
    documentSections = documentSections,
    onUserBackPressed = onUserBackPressed,
    navHostController = navHostController,
    mainActivityBottomAppBarScrollBehaviour = mainActivityBottomAppBarScrollBehaviour,
    navigationIcon = navigationIcon
  )
}
