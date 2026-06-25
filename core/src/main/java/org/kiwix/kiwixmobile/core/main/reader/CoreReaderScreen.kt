package org.kiwix.kiwixmobile.core.main.reader

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderEffect
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SearchItemToOpen
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.utils.TAG_FILE_SEARCHED

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoreReaderScreen(
  viewModel: CoreReaderViewModel,
  activity: CoreMainActivity,
  navHostController: NavHostController
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackBarHostState = remember { SnackbarHostState() }
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
    state = uiState,
    snackBarHost = snackBarHostState,
    actionMenuItems = viewModel.readerMenuState?.menuItems.orEmpty(),
    onReaderAction = viewModel::onAction,
    onUserBackPressed = { viewModel.onUserBackPressed(activity) },
    navHostController = navHostController,
    mainActivityBottomAppBarScrollBehaviour = activity.bottomAppBarScrollBehaviour,
    navigationIcon = { NavigationItem(viewModel, activity) }
  )
}

@Composable
private fun NavigationItem(viewModel: CoreReaderViewModel, activity: CoreMainActivity) {
  NavigationIcon(
    iconItem = viewModel.navigationIcon(),
    contentDescription = viewModel.navigationIconContentDescription(),
    onClick = { viewModel.navigationIconClick(activity.navigationDrawerIsOpen()) },
    iconTint = viewModel.navigationIconTint()
  )
}
