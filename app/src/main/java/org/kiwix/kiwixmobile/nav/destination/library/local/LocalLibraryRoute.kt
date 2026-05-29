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
 *
 */

package org.kiwix.kiwixmobile.nav.destination.library.local

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity.RESULT_OK
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.flow.filterIsInstance
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.drawable
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.extensions.CollectSideEffectWithActivity
import org.kiwix.kiwixmobile.core.extensions.handlePermissionRequest
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.SHARE_MENU_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.DELETE_MENU_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.LocalLibraryUiActions.RequestReadWritePermission
import org.kiwix.kiwixmobile.ui.KiwixDestination

const val VALIDATE_ZIM_FILES_MENU_BUTTON_TESTING_TAG = "validateZimFilesMenuButtonTestingTag"

/**
 * Entry point for Local Library feature.
 *
 * Handles permissions, file selection, action mode, and navigation.
 * Complexity suppressed as this is a Route-level composable managing
 * multiple app-level concerns that shouldn't be split.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LocalLibraryRoute(
  localLibraryViewModel: LocalLibraryViewModel,
  navController: NavHostController,
  zimFileUriArg: String,
  snackBarHostState: SnackbarHostState
) {
  val mainActivity = LocalActivity.current as KiwixMainActivity
  val uiState = localLibraryViewModel.uiState.collectAsStateWithLifecycle()
  val readWritePermission =
    rememberMultiplePermissionsState(listOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE))
  val filePickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      if (it.resultCode == RESULT_OK) {
        localLibraryViewModel.processSelectedZimFiles(it.data)
      }
    }

  localLibraryViewModel.sideEffects.CollectSideEffectWithActivity { effect, activity ->
    effect.invokeWith(activity)
  }
  ObserveLocalLibraryUiActions(localLibraryViewModel, readWritePermission, mainActivity)
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { localLibraryViewModel.onResume() }
  BackHandler(enabled = uiState.value.fileSelectListState.selectionMode == SelectionMode.MULTI) {
    localLibraryViewModel.finishMultiModeFinished()
  }
  LocalLibraryScreen(
    state = uiState.value,
    actionMenuItems = actionMenuItems(
      navController = navController,
      selectionMode = uiState.value.fileSelectListState.selectionMode,
      localLibraryViewModel = localLibraryViewModel
    ) {
      localLibraryViewModel.filePickerMenuButtonClick(filePickerLauncher)
    },
    listState = rememberLazyListState(),
    snackbarHostState = snackBarHostState,
    onRefresh = localLibraryViewModel::onSwipeRefresh,
    onDownloadButtonClick = localLibraryViewModel::onDownloadButtonClick,
    onClick = localLibraryViewModel::onBookItemClick,
    onLongClick = localLibraryViewModel::onBookItemLongClick,
    onMultiSelect = localLibraryViewModel::onMultiSelect,
    bottomAppBarScrollBehaviour = mainActivity.bottomAppBarScrollBehaviour,
    onUserBackPressed = localLibraryViewModel::handleUserBackPressed,
    navHostController = navController,
    navigationIcon = {
      NavigationIcon(
        iconItem = navigationIconItem(uiState.value.fileSelectListState.selectionMode == SelectionMode.MULTI),
        contentDescription = string.open_drawer,
        onClick = localLibraryViewModel::onNavigationIconClick
      )
    }
  )
  LaunchedEffect(zimFileUriArg) {
    LanguageUtils(mainActivity).changeFont(mainActivity, localLibraryViewModel.kiwixDataStore)
    localLibraryViewModel.processZimFileArguments(zimFileUriArg)
  }
}

fun navigationIconItem(isMultiMode: Boolean): IconItem = if (isMultiMode) {
  IconItem.Vector(Icons.AutoMirrored.Filled.ArrowBack)
} else {
  IconItem.Vector(Icons.Filled.Menu)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ObserveLocalLibraryUiActions(
  viewModel: LocalLibraryViewModel,
  readWritePermission: MultiplePermissionsState,
  activity: KiwixMainActivity
) {
  LaunchedEffect(Unit) {
    // All other actions are handled in viewModel with sideEffect.
    // Asking permission is a UI responsibility so we are asking it on UI.
    viewModel.localLibraryUiActions
      .filterIsInstance<RequestReadWritePermission>()
      .collect {
        activity.toast(string.request_storage)
        readWritePermission.handlePermissionRequest(
          onGranted = { viewModel.onReadWritePermissionGranted(it.resultAction) },
          onRationale = viewModel::onReadWriteRationalPermission,
        )
      }
  }
}

fun actionMenuItems(
  navController: NavHostController,
  selectionMode: SelectionMode,
  localLibraryViewModel: LocalLibraryViewModel,
  filePickerButtonClick: () -> Unit
) = when (selectionMode) {
  SelectionMode.MULTI -> multiModeMenuItem(localLibraryViewModel)
  SelectionMode.NORMAL -> normalModeMenuItems(navController, filePickerButtonClick)
}

private fun multiModeMenuItem(localLibraryViewModel: LocalLibraryViewModel) = listOf(
  ActionMenuItem(
    IconItem.Drawable(drawable.ic_delete_white_24dp),
    string.delete,
    { localLibraryViewModel.deleteMenuIconClick() },
    isEnabled = true,
    testingTag = DELETE_MENU_ICON_TESTING_TAG
  ),
  ActionMenuItem(
    IconItem.Drawable(drawable.baseline_share_24),
    string.share,
    { localLibraryViewModel.shareMenuIconClick() },
    isEnabled = true,
    testingTag = SHARE_MENU_BUTTON_TESTING_TAG
  ),
  ActionMenuItem(
    IconItem.Drawable(R.drawable.file_validate),
    string.validate_zim_files,
    { localLibraryViewModel.validateMenuIconClick() },
    isEnabled = true,
    testingTag = VALIDATE_ZIM_FILES_MENU_BUTTON_TESTING_TAG
  )
)

private fun normalModeMenuItems(
  navController: NavHostController,
  filePickerButtonClick: () -> Unit
) =
  listOf(
    ActionMenuItem(
      IconItem.Drawable(drawable.ic_add_blue_24dp),
      R.string.select_zim_file,
      { filePickerButtonClick.invoke() },
      isEnabled = true,
      testingTag = SELECT_FILE_BUTTON_TESTING_TAG
    ),
    ActionMenuItem(
      IconItem.Drawable(R.drawable.ic_baseline_mobile_screen_share_24px),
      string.get_content_from_nearby_device,
      { navController.navigate(KiwixDestination.LocalFileTransfer.route) },
      isEnabled = true,
      testingTag = LOCAL_FILE_TRANSFER_MENU_BUTTON_TESTING_TAG
    )
  )
