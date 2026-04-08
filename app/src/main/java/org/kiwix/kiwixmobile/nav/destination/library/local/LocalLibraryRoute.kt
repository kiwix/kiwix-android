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

import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.R.drawable
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isManageExternalStoragePermissionGranted
import org.kiwix.kiwixmobile.core.extensions.CollectSideEffectWithActivity
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.navigateToSettings
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.ui.KiwixDestination
import java.util.Locale

private const val SHOW_SCAN_DIALOG_DELAY = 2000L

/**
 * Entry point for Local Library feature.
 *
 * Handles permissions, file selection, action mode, and navigation.
 * Complexity suppressed as this is a Route-level composable managing
 * multiple app-level concerns that shouldn't be split.
 */
@Suppress(
  "LongMethod",
  "ComplexMethod",
  "ComplexCondition",
  "TooGenericExceptionCaught",
  "InjectDispatcher"
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalLibraryRoute(
  localLibraryViewModel: LocalLibraryViewModel,
  navController: NavHostController,
  zimFileUriArg: String,
  snackBarHostState: SnackbarHostState
) {
  val context = LocalContext.current
  val activity = context as KiwixMainActivity

  val kiwixDataStore = activity.kiwixDataStore
  val dialogShower = remember { activity.alertDialogShower }
  val uiState = localLibraryViewModel.uiState.collectAsStateWithLifecycle()

  val coroutineScope = rememberCoroutineScope()
  var actionMode by remember { mutableStateOf<ActionMode?>(null) }
  var permissionDeniedLayoutShowing by remember { mutableStateOf(false) }
  var shouldScanFileSystem by remember { mutableStateOf(false) }

  val requestFileSystemCheck = {
    coroutineScope.launch {
      localLibraryViewModel.requestFileSystemCheck.emit(Unit)
    }
    Unit
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

  LaunchedEffect(Unit) {
    localLibraryViewModel.processZimFileUri(zimFileUriArg)

    // it should not everytime scan the storage.
    // It should only when user want to scan the storage.
    // if (activity.isManageExternalStoragePermissionGranted(kiwixDataStore)) {
    //   requestFileSystemCheck()
    // }
  }

  // Port of LocalLibraryFragment.onResume() lifecycle logic.
  // Shows the scan storage dialog on first visit, handles permission checks,
  // and triggers file system scans when returning from settings.
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        coroutineScope.launch {
          when {
            shouldShowFileSystemDialog(
              kiwixDataStore,
              fileSelectListState
            ) -> {
              Handler(Looper.getMainLooper()).postDelayed({
                coroutineScope.launch {
                  showFileSystemScanDialog(
                    dialogShower,
                    kiwixDataStore,
                    onScanRequested = {
                      shouldScanFileSystem = true
                      coroutineScope.launch {
                        scanFileSystem(
                          activity,
                          kiwixDataStore,
                          dialogShower,
                          onShouldScanChanged = { shouldScanFileSystem = it },
                          requestFileSystemCheck = requestFileSystemCheck
                        )
                      }
                    }
                  )
                }
              }, SHOW_SCAN_DIALOG_DELAY)
            }

            shouldScanFileSystem -> {
              // When user goes to settings for granting the MANAGE_EXTERNAL_STORAGE
              // permission, and comes back to the application then initiate
              // the scanning of file system.
              scanFileSystem(
                activity,
                kiwixDataStore,
                dialogShower,
                onShouldScanChanged = { shouldScanFileSystem = it },
                requestFileSystemCheck = requestFileSystemCheck
              )
            }

            !kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove() &&
              !kiwixDataStore.prefIsTest.first() &&
              !permissionDeniedLayoutShowing -> {
              // Check manage external storage permission for non-PlayStore builds.
              if (!activity.isManageExternalStoragePermissionGranted(kiwixDataStore)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                  val shouldShowDialog =
                    kiwixDataStore.showManageExternalFilesPermissionDialog.first()
                  if (shouldShowDialog) {
                    kiwixDataStore.setShowManageExternalFilesPermissionDialog(false)
                    dialogShower.show(
                      KiwixDialog.ManageExternalFilesPermissionDialog,
                      { activity.navigateToSettings() }
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  LocalLibraryScreen(
    state = uiState.value,
    actionMenuItems =
      actionMenuItems(navController, localLibraryViewModel::filePickerMenuButtonClick),
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
}

private fun setActionModeTitle(actionMode: ActionMode?, selectedBookCount: Int) {
  actionMode?.title = String.format(Locale.getDefault(), "%d", selectedBookCount)
}

/**
 * Shows the FileSystemScan dialog.
 */
private fun showFileSystemScanDialog(
  dialogShower: AlertDialogShower,
  kiwixDataStore: KiwixDataStore,
  onScanRequested: () -> Unit
) {
  dialogShower.show(
    KiwixDialog.YesNoDialog.FileSystemScan,
    {
      CoroutineScope(Dispatchers.Main).launch {
        // Sets true so that it cannot show again.
        kiwixDataStore.setIsScanFileSystemDialogShown(true)
        onScanRequested()
      }
    },
    {
      CoroutineScope(Dispatchers.Main).launch {
        // User clicks on the "No" button so do not show again.
        kiwixDataStore.setIsScanFileSystemDialogShown(true)
      }
    }
  )
}

/**
 * Scans the file system for ZIM files.
 * Checks:
 * 1. If the app has manage external storage permission (on Android R+).
 * 2. Then finally it scans the storage for ZIM files.
 */
private suspend fun scanFileSystem(
  activity: KiwixMainActivity,
  kiwixDataStore: KiwixDataStore,
  dialogShower: AlertDialogShower,
  onShouldScanChanged: (Boolean) -> Unit,
  requestFileSystemCheck: () -> Unit
) {
  when {
    !activity.isManageExternalStoragePermissionGranted(kiwixDataStore) -> {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        dialogShower.show(
          KiwixDialog.ManageExternalFilesPermissionDialog,
          { activity.navigateToSettings() }
        )
      }
    }

    else -> {
      onShouldScanChanged(false)
      requestFileSystemCheck()
    }
  }
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
