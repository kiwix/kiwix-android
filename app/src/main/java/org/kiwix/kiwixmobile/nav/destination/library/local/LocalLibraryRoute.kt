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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isManageExternalStoragePermissionGranted
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.navigateToSettings
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.BuildConfig
import org.kiwix.kiwixmobile.kiwixActivityComponent
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.local.FileSelectActions.RequestMultiSelection
import org.kiwix.kiwixmobile.nav.destination.library.local.FileSelectActions.RequestNavigateTo
import org.kiwix.kiwixmobile.nav.destination.library.local.FileSelectActions.RequestSelect
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.zimManager.MAX_PROGRESS
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState
import java.util.Locale

private const val SHOW_SCAN_DIALOG_DELAY = 2000L

/**
 * Entry point for Local Library feature.
 *
 * Handles permissions, file selection, action mode, and navigation.
 * Complexity suppressed as this is a Route-level composable managing
 * multiple app-level concerns that shouldn't be split.
 */
@Suppress("LongMethod", "ComplexMethod", "ComplexCondition", "TooGenericExceptionCaught", "InjectDispatcher")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalLibraryRoute(
  viewModelFactory: ViewModelProvider.Factory,
  navController: NavHostController,
  zimFileUriArg: String
) {
  val context = LocalContext.current
  val activity = context as KiwixMainActivity
  val component = activity.kiwixActivityComponent

  val kiwixDataStore = activity.kiwixDataStore
  val dialogShower = remember { activity.alertDialogShower }
  val mainRepositoryActions = remember { component.mainRepositoryActions() }

  val localLibraryViewModel: LocalLibraryViewModel = viewModel(factory = viewModelFactory)
  val validateZimViewModel: ValidateZimViewModel = viewModel(factory = viewModelFactory)

  val fileSelectListState by localLibraryViewModel.fileSelectListStates
    .observeAsState(FileSelectListState(emptyList()))
  val deviceListScanningProgress by localLibraryViewModel.deviceListScanningProgress.observeAsState()
  val snackBarHostState = remember { SnackbarHostState() }

  val processSelectedZimFilesForPlayStore = remember { component.processSelectedZimFilesForPlayStore() }
  val processSelectedZimFilesForStandalone = remember { component.processSelectedZimFilesForStandalone() }

  val coroutineScope = rememberCoroutineScope()
  var actionMode by remember { mutableStateOf<ActionMode?>(null) }
  var permissionDeniedLayoutShowing by remember { mutableStateOf(false) }
  var shouldScanFileSystem by remember { mutableStateOf(false) }

  val selectedZimFileCallback = rememberSelectedZimFileCallback(
    activity,
    navController,
    dialogShower,
    coroutineScope,
    mainRepositoryActions = mainRepositoryActions
  )

  LaunchedEffect(Unit) {
    processSelectedZimFilesForStandalone.setSelectedZimFileCallback(selectedZimFileCallback)
    processSelectedZimFilesForPlayStore.init(
      lifecycleScope = coroutineScope,
      alertDialogShower = dialogShower,
      snackBarHostState = snackBarHostState,
      fragmentManager = activity.supportFragmentManager,
      selectedZimFileCallback = selectedZimFileCallback
    )
    localLibraryViewModel.setAlertDialogShower(dialogShower)
    localLibraryViewModel.setValidateZimViewModel(validateZimViewModel)
  }

  val fileSelectLauncher = rememberFileSelectLauncher(
    activity,
    coroutineScope,
    processSelectedZimFilesForStandalone,
    processSelectedZimFilesForPlayStore
  )

  val filePickerButtonClick = rememberFilePickerAction(
    activity,
    coroutineScope,
    dialogShower,
    kiwixDataStore,
    fileSelectLauncher
  )

  val requestFileSystemCheck = {
    coroutineScope.launch {
      localLibraryViewModel.requestFileSystemCheck.emit(Unit)
    }
    Unit
  }

  LaunchedEffect(Unit) {
    localLibraryViewModel.sideEffects.collect {
      val effectResult = it.invokeWith(activity)
      if (effectResult is ActionMode) {
        actionMode = effectResult
        actionMode?.title = String.format(Locale.getDefault(), "%d", fileSelectListState.selectedBooks.size)
      }
    }
  }

  LaunchedEffect(fileSelectListState) {
    if (fileSelectListState.bookOnDiskListItems.isNotEmpty()) {
      if (fileSelectListState.bookOnDiskListItems.none { it.isSelected }) {
        actionMode?.finish()
        actionMode = null
      } else {
        actionMode?.title = String.format(Locale.getDefault(), "%d", fileSelectListState.selectedBooks.size)
      }
    }
  }

  LaunchedEffect(Unit) {
    if (zimFileUriArg.isNotEmpty()) {
      val uri = zimFileUriArg.toUri()
      // Manual handling for start-up argument, similar to logic inside launcher
      coroutineScope.launch {
        when {
          processSelectedZimFilesForStandalone.canHandleUris() ->
            processSelectedZimFilesForStandalone.processSelectedFiles(listOf(uri))

          activity.kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove() ->
            processSelectedZimFilesForPlayStore.processSelectedFiles(listOf(uri))
        }
      }
    }

    if (activity.isManageExternalStoragePermissionGranted(kiwixDataStore)) {
      requestFileSystemCheck()
    }
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
                          shouldScanFileSystem = true,
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
                shouldScanFileSystem = shouldScanFileSystem,
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

  val screenState = rememberLocalLibraryScreenState(
    fileSelectListState = fileSelectListState,
    deviceListScanningProgress = deviceListScanningProgress,
    snackBarHostState = snackBarHostState,
    permissionDeniedLayoutShowing = permissionDeniedLayoutShowing,
    navController = navController
  )

  LocalLibraryScreen(
    state = screenState,
    listState = rememberLazyListState(),
    onRefresh = {
      coroutineScope.launchWithPermissionCheck(activity, kiwixDataStore, dialogShower) {
        requestFileSystemCheck()
      }
    },
    onDownloadButtonClick = {
      coroutineScope.launch {
        if (permissionDeniedLayoutShowing) {
          permissionDeniedLayoutShowing = false
          activity.navigateToAppSettings()
        } else {
          localLibraryViewModel.fileSelectActions.tryEmit(FileSelectActions.UserClickedDownloadBooksButton)
        }
      }
    },
    fabButtonClick = filePickerButtonClick,
    onClick = { bookOnDisk ->
      coroutineScope.launchWithPermissionCheck(activity, kiwixDataStore, dialogShower) {
        localLibraryViewModel.fileSelectActions.tryEmit(RequestNavigateTo(bookOnDisk))
      }
    },
    onLongClick = { bookOnDisk ->
      coroutineScope.launchWithPermissionCheck(activity, kiwixDataStore, dialogShower) {
        localLibraryViewModel.fileSelectActions.tryEmit(RequestMultiSelection(bookOnDisk))
      }
    },
    onMultiSelect = { bookOnDisk ->
      localLibraryViewModel.fileSelectActions.tryEmit(RequestSelect(bookOnDisk))
    },
    bottomAppBarScrollBehaviour = activity.bottomAppBarScrollBehaviour,
    onUserBackPressed = {
      handleUserBackPressed(activity)
    },
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

  DisposableEffect(Unit) {
    onDispose {
      processSelectedZimFilesForPlayStore.dispose()
      selectedZimFileCallback.hashCode() // keep reference
    }
  }
}

/**
 * Determines whether the file system scan dialog should be shown.
 * Conditions:
 *  1. The scan dialog has not already been shown.
 *  2. This is not the Play Store build.
 *  3. There are no ZIM files showing on the library screen.
 */
private suspend fun shouldShowFileSystemDialog(
  kiwixDataStore: KiwixDataStore,
  fileSelectListState: FileSelectListState
): Boolean =
  !kiwixDataStore.isScanFileSystemDialogShown.first() &&
    !BuildConfig.IS_PLAYSTORE &&
    fileSelectListState.bookOnDiskListItems.isEmpty()

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
  shouldScanFileSystem: Boolean,
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

@Composable
private fun rememberLocalLibraryScreenState(
  fileSelectListState: FileSelectListState,
  deviceListScanningProgress: Int?,
  snackBarHostState: SnackbarHostState,
  permissionDeniedLayoutShowing: Boolean,
  navController: NavHostController
): LocalLibraryScreenState {
  val context = LocalContext.current
  return remember(
    fileSelectListState,
    deviceListScanningProgress,
    snackBarHostState,
    permissionDeniedLayoutShowing,
    navController,
    context
  ) {
    val noFilesViewItem = Triple(
      if (permissionDeniedLayoutShowing) {
        context.getString(string.grant_read_storage_permission)
      } else {
        context.getString(string.no_files_here)
      },
      if (permissionDeniedLayoutShowing) {
        context.getString(string.go_to_settings_label)
      } else {
        context.getString(string.download_books)
      },
      permissionDeniedLayoutShowing || fileSelectListState.bookOnDiskListItems.isEmpty()
    )

    val actionMenuItems = listOf(
      ActionMenuItem(
        IconItem.Drawable(R.drawable.ic_baseline_mobile_screen_share_24px),
        string.get_content_from_nearby_device,
        { navController.navigate(KiwixDestination.LocalFileTransfer.route) },
        isEnabled = true,
        testingTag = LOCAL_FILE_TRANSFER_MENU_BUTTON_TESTING_TAG
      )
    )

    LocalLibraryScreenState(
      fileSelectListState = fileSelectListState,
      snackBarHostState = snackBarHostState,
      swipeRefreshItem = false to (deviceListScanningProgress != MAX_PROGRESS && deviceListScanningProgress != null),
      scanningProgressItem = (deviceListScanningProgress != null && deviceListScanningProgress != MAX_PROGRESS) to
        (deviceListScanningProgress ?: 0),
      noFilesViewItem = noFilesViewItem,
      actionMenuItems = actionMenuItems
    )
  }
}

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

private fun kotlinx.coroutines.CoroutineScope.launchWithPermissionCheck(
  activity: KiwixMainActivity,
  kiwixDataStore: KiwixDataStore,
  dialogShower: AlertDialogShower,
  block: suspend () -> Unit
) {
  launch {
    if (!activity.isManageExternalStoragePermissionGranted(kiwixDataStore)) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        dialogShower.show(
          KiwixDialog.ManageExternalFilesPermissionDialog,
          {
            activity.navigateToSettings()
          }
        )
      }
    } else {
      block()
    }
  }
}
