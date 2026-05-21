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
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.extensions.CollectSideEffectWithActivity
import org.kiwix.kiwixmobile.core.extensions.handlePermissionRequest
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.LocalLibraryUiActions.RequestReadWritePermission
import org.kiwix.kiwixmobile.ui.KiwixDestination
import java.util.Locale

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
  val activity = LocalActivity.current as KiwixMainActivity
  val uiState = localLibraryViewModel.uiState.collectAsStateWithLifecycle()
  var actionMode by remember { mutableStateOf<ActionMode?>(null) }
  val readWritePermission =
    rememberMultiplePermissionsState(listOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE))
  val filePickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      if (it.resultCode == RESULT_OK) {
        localLibraryViewModel.processSelectedZimFiles(it.data)
      }
    }

  localLibraryViewModel.sideEffects.CollectSideEffectWithActivity { effect, activity ->
    val effectResult = effect.invokeWith(activity)
    if (effectResult is ActionMode) {
      actionMode = effectResult
      uiState.value.fileSelectListState.selectedBooks.size.let {
        setActionModeTitle(actionMode, it)
      }
    }
  }
  LaunchedEffect(uiState.value.fileSelectListState) {
    if (uiState.value.fileSelectListState.bookOnDiskListItems.none { it.isSelected }) {
      actionMode?.finish()
      actionMode = null
    } else {
      setActionModeTitle(actionMode, uiState.value.fileSelectListState.selectedBooks.size)
    }
  }
  ObserveLocalLibraryUiActions(localLibraryViewModel, readWritePermission, activity)
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { localLibraryViewModel.onResume() }

  LocalLibraryScreen(
    state = uiState.value,
    actionMenuItems =
      actionMenuItems(navController) {
        localLibraryViewModel.filePickerMenuButtonClick(filePickerLauncher)
      },
    listState = rememberLazyListState(),
    snackbarHostState = snackBarHostState,
    onRefresh = localLibraryViewModel::onSwipeRefresh,
    onDownloadButtonClick = localLibraryViewModel::onDownloadButtonClick,
    onClick = { localLibraryViewModel.onBookItemClick(it) },
    onLongClick = { localLibraryViewModel.onBookItemLongClick(it) },
    onMultiSelect = { localLibraryViewModel.onMultiSelect(it) },
    bottomAppBarScrollBehaviour = activity.bottomAppBarScrollBehaviour,
    onUserBackPressed = { handleUserBackPressed(activity) },
    navHostController = navController,
    navigationIcon = {
      NavigationIcon(
        iconItem = IconItem.Vector(Icons.Filled.Menu),
        contentDescription = string.open_drawer,
        onClick = {
          handleNavigationIconClick(activity)
        }
      )
    }
  )
  LaunchedEffect(Unit) {
    LanguageUtils(activity).changeFont(activity, localLibraryViewModel.kiwixDataStore)
    localLibraryViewModel.processZimFileArguments(zimFileUriArg)
  }
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

private fun setActionModeTitle(actionMode: ActionMode?, selectedBookCount: Int) {
  actionMode?.title = String.format(Locale.getDefault(), "%d", selectedBookCount)
}

fun actionMenuItems(navController: NavHostController, filePickerButtonClick: () -> Unit) = listOf(
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

private fun handleUserBackPressed(activity: KiwixMainActivity): FragmentActivityExtensions.Super {
  val coreMainActivity = activity as? CoreMainActivity
  return if (coreMainActivity?.navigationDrawerIsOpen() == true) {
    coreMainActivity.closeNavigationDrawer()
    FragmentActivityExtensions.Super.ShouldNotCall
  } else {
    FragmentActivityExtensions.Super.ShouldCall
  }
}

private fun handleNavigationIconClick(activity: KiwixMainActivity) {
  if (activity.navigationDrawerIsOpen()) {
    activity.closeNavigationDrawer()
  } else {
    activity.openNavigationDrawer()
  }
}
