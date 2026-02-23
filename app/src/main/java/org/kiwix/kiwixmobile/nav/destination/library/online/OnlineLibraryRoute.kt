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

import android.Manifest
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.R.drawable
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.extensions.closeKeyboard
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.navigateToSettings
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.core.utils.NetworkUtils
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem

const val LANGUAGE_MENU_ICON_TESTING_TAG = "languageMenuIconTestingTag"
const val CATEGORY_MENU_ICON_TESTING_TAG = "categoryMenuIconTestingTag"

@Suppress("UnusedPrivateProperty")
private const val ZERO = 0

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("MagicNumber", "LongMethod")
@Composable
fun OnlineLibraryRoute(
  viewModelFactory: ViewModelProvider.Factory,
  alertDialogShower: AlertDialogShower,
  navController: NavHostController
) {
  val context = LocalContext.current
  val activity = context as KiwixMainActivity

  val zimManageViewModel: ZimManageViewModel =
    viewModel(viewModelStoreOwner = activity, factory = viewModelFactory)
  val onlineLibraryViewModel: OnlineLibraryViewModel =
    viewModel(viewModelStoreOwner = activity, factory = viewModelFactory)

  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val lazyListState = rememberLazyListState()

  var isSearchActive by remember { mutableStateOf(false) }
  var searchText by remember { mutableStateOf("") }
  var isLoadingMoreItem by remember { mutableStateOf(false) }
  var scanningProgressItem by remember { mutableStateOf(Pair(false, "")) }
  var noContentViewItem by remember { mutableStateOf(Pair("", false)) }
  var isRefreshing by remember { mutableStateOf(false) }
  var onlineLibraryList by remember { mutableStateOf<List<LibraryListItem>?>(null) }

  val storagePermissionLauncher = rememberStoragePermissionLauncher(
    onlineLibraryViewModel,
    alertDialogShower,
    activity,
    context
  )

  val notificationPermissionLauncher = rememberNotificationPermissionLauncher(
    onlineLibraryViewModel,
    activity,
    context
  )

  val libraryItems by zimManageViewModel.libraryItems.collectAsState()
  val onlineLibraryDownloading by zimManageViewModel.onlineLibraryDownloading.collectAsState()
  val scanningProgress by onlineLibraryViewModel.scanningProgress.collectAsState()
  val noContentState by onlineLibraryViewModel.noContentState.collectAsState()

  // Collect UI events
  HandleUiEvents(
    viewModel = onlineLibraryViewModel,
    snackbarHostState = snackbarHostState,
    alertDialogShower = alertDialogShower,
    activity = activity,
    scope = scope,
    context = context
  )

  // Handle side-effects (Network, Wifi, Progress, etc)
  HandleEffects(
    zimManageViewModel = zimManageViewModel,
    onlineLibraryViewModel = onlineLibraryViewModel,
    alertDialogShower = alertDialogShower,
    isListEmpty = onlineLibraryList.isNullOrEmpty(),
    onNoContentChanged = { noContentViewItem = it },
    onRefreshingChanged = { isRefreshing = it }
  )

  LaunchedEffect(noContentState) {
    if (noContentState.first.isNotEmpty() || noContentState.second) {
      noContentViewItem = noContentState
    }
  }

  ObserveLibraryItems(
    libraryItems = libraryItems,
    context = context,
    activity = activity,
    onListChanged = { onlineLibraryList = it },
    onNoContentChanged = { noContentViewItem = it },
    onLoadingMoreChanged = { isLoadingMoreItem = it }
  )

  ObserveDownloadingState(
    onlineLibraryDownloading = onlineLibraryDownloading,
    onRefreshingChanged = { isRefreshing = it },
    onScanningProgressChanged = { scanningProgressItem = it },
    onLoadingMoreChanged = { isLoadingMoreItem = it },
    context = context
  )

  LaunchedEffect(Unit) {
    zimManageViewModel.updateOnlineLibraryFilters(
      onlineLibraryViewModel.getOnlineLibraryRequest()
    )
    scanningProgressItem = false to context.getString(string.reaching_remote_library)
    zimManageViewModel.onlineBooksSearchedQuery.value
      ?.takeIf { it.isNotEmpty() }
      ?.let { query ->
        isSearchActive = true
        searchText = query
        zimManageViewModel.requestFiltering.tryEmit(query)
      } ?: run {
      zimManageViewModel.onlineBooksSearchedQuery.value = ""
      zimManageViewModel.requestFiltering.tryEmit("")
    }
  }

  val actionMenuItems = buildActionMenuItems(
    isSearchActive = isSearchActive,
    onSearchClick = { isSearchActive = true },
    activity = activity,
    navController = navController
  )

  OnlineLibraryScreen(
    state = OnlineLibraryScreenState(
      onlineLibraryList = onlineLibraryList,
      isRefreshing = isRefreshing,
      snackBarHostState = snackbarHostState,
      onRefresh = {
        scope.launch {
          onlineLibraryViewModel.refreshFragment(
            activity,
            zimManageViewModel,
            true,
            context,
            onlineLibraryList.isNullOrEmpty(),
            { isRefreshing = it }
          )
        }
      },
      scanningProgressItem = scanningProgressItem,
      noContentViewItem = noContentViewItem,
      bookUtils = onlineLibraryViewModel.bookUtils,
      availableSpaceCalculator = onlineLibraryViewModel.availableSpaceCalculator,
      onBookItemClick = {
        onlineLibraryViewModel.onBookItemClick(
          activity,
          it,
          context,
          onRequestStoragePermission = {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
          },
          onRequestNotificationPermission = {
            @SuppressLint("InlinedApi")
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          },
          onShowStorageSelectDialog = {
            onlineLibraryViewModel.showStorageSelectDialog(activity)
          }
        )
      },
      onPauseResumeButtonClick = { item ->
        if (!NetworkUtils.isNetworkAvailable(activity)) {
          onlineLibraryViewModel.emitNoInternetSnackbar(context)
          return@OnlineLibraryScreenState
        }
        onlineLibraryViewModel.downloader.pauseResumeDownload(
          item.downloadId,
          item.downloadState == DownloadState.Paused
        )
      },
      onStopButtonClick = { item ->
        onStopButtonClick(onlineLibraryViewModel, item, activity, context)
      },
      isSearchActive = isSearchActive,
      searchText = searchText,
      searchValueChangedListener = {
        if (it.isNotEmpty()) {
          zimManageViewModel.onlineBooksSearchedQuery.value = it
        }
        searchText = it
        zimManageViewModel.requestFiltering.tryEmit(it.trim())
      },
      clearSearchButtonClickListener = {
        searchText = ""
        zimManageViewModel.onlineBooksSearchedQuery.value = null
        zimManageViewModel.requestFiltering.tryEmit("")
      },
      onLoadMore = { count ->
        onlineLibraryViewModel.handleLoadMore(zimManageViewModel, count)
      },
      isLoadingMoreItem = isLoadingMoreItem
    ),
    actionMenuItems = actionMenuItems,
    listState = lazyListState,
    bottomAppBarScrollBehaviour = activity.bottomAppBarScrollBehaviour,
    onUserBackPressed = {
      handleBackPress(activity, isSearchActive) {
        isSearchActive = false
        searchText = ""
        zimManageViewModel.onlineBooksSearchedQuery.value = null
        zimManageViewModel.requestFiltering.tryEmit("")
      }
    },
    navHostController = navController,
    navigationIcon = {
      NavigationIcon(
        iconItem = if (isSearchActive) {
          IconItem.Vector(Icons.AutoMirrored.Filled.ArrowBack)
        } else {
          IconItem.Vector(Icons.Filled.Menu)
        },
        contentDescription = string.open_drawer,
        onClick = {
          if (isSearchActive) {
            isSearchActive = false
            searchText = ""
            zimManageViewModel.onlineBooksSearchedQuery.value = null
            zimManageViewModel.requestFiltering.tryEmit("")
            activity.onBackPressedDispatcher.onBackPressed()
          } else {
            if (activity.navigationDrawerIsOpen()) {
              activity.closeNavigationDrawer()
            } else {
              activity.openNavigationDrawer()
            }
          }
        }
      )
    }
  )

  DialogHost(alertDialogShower)
}

@Composable
private fun HandleUiEvents(
  viewModel: OnlineLibraryViewModel,
  snackbarHostState: SnackbarHostState,
  alertDialogShower: AlertDialogShower,
  activity: KiwixMainActivity,
  scope: kotlinx.coroutines.CoroutineScope,
  context: android.content.Context
) {
  LaunchedEffect(Unit) {
    viewModel.uiEvents.collect { event ->
      when (event) {
        is OnlineLibraryViewModel.UiEvent.ShowSnackbar -> {
          snackbarHostState.snack(
            message = event.message,
            actionLabel = event.actionLabel,
            lifecycleScope = scope,
            actionClick = {
              event.actionIntent?.let { context.startActivity(it) }
              event.onAction?.invoke()
            }
          )
        }
        is OnlineLibraryViewModel.UiEvent.ShowNoSpaceSnackbar -> {
          snackbarHostState.snack(
            message = event.message,
            actionLabel = event.actionLabel,
            lifecycleScope = scope,
            actionClick = { event.onAction() }
          )
        }
        is OnlineLibraryViewModel.UiEvent.ShowDialog -> {
          alertDialogShower.show(
            event.dialog,
            event.positiveAction,
            event.negativeAction
          )
        }
        is OnlineLibraryViewModel.UiEvent.ShowToast -> {
          context.toast(event.message, Toast.LENGTH_SHORT)
        }
        is OnlineLibraryViewModel.UiEvent.RequestPermission -> {
          // Handled via launchers
        }
        is OnlineLibraryViewModel.UiEvent.NavigateToSettings -> {
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            (activity as androidx.fragment.app.FragmentActivity).navigateToSettings()
          }
        }
        is OnlineLibraryViewModel.UiEvent.NavigateToAppSettings -> {
          activity.navigateToAppSettings()
        }
      }
    }
  }
}

@Composable
@Suppress("LongMethod", "ComplexMethod")
private fun HandleEffects(
  zimManageViewModel: ZimManageViewModel,
  onlineLibraryViewModel: OnlineLibraryViewModel,
  alertDialogShower: AlertDialogShower,
  isListEmpty: Boolean,
  onNoContentChanged: (Pair<String, Boolean>) -> Unit,
  onRefreshingChanged: (Boolean) -> Unit
) {
  val context = LocalContext.current
  val activity = context as KiwixMainActivity
  val scope = rememberCoroutineScope()

  // Observe LiveData from ZimManageViewModel using observeAsState (Compose-LiveData bridge)
  val shouldShowWifiOnly by zimManageViewModel.shouldShowWifiOnlyDialog.observeAsState(false)
  val libraryListIsRefreshing by zimManageViewModel.libraryListIsRefreshing.observeAsState(false)
  val networkState by zimManageViewModel.networkStates.observeAsState()

  // Handle wifi-only dialog
  LaunchedEffect(shouldShowWifiOnly) {
    if (shouldShowWifiOnly && !NetworkUtils.isWiFi(context)) {
      alertDialogShower.show(
        KiwixDialog.YesNoDialog.WifiOnly,
        {
          onNoContentChanged("" to false)
          scope.launch {
            onlineLibraryViewModel.kiwixDataStore.setWifiOnly(false)
            zimManageViewModel.shouldShowWifiOnlyDialog.value = false
            zimManageViewModel.updateOnlineLibraryFilters(
              onlineLibraryViewModel.getOnlineLibraryRequest()
            )
          }
        },
        {
          context.toast(
            context.getString(string.denied_internet_permission_message),
            Toast.LENGTH_SHORT
          )
          onNoContentChanged(context.getString(string.swipe_down_for_library) to true)
        }
      )
      onRefreshingChanged(false)
    }
  }

  // Handle refreshing state
  LaunchedEffect(libraryListIsRefreshing) {
    onRefreshingChanged(libraryListIsRefreshing)
  }

  // Handle network state changes
  LaunchedEffect(networkState) {
    when (networkState) {
      NetworkState.CONNECTED -> {
        onlineLibraryViewModel.handleNetworkConnected(
          context,
          activity,
          zimManageViewModel,
          isListEmpty,
          onRefreshingChanged
        )
      }
      NetworkState.NOT_CONNECTED -> {
        if (!isListEmpty) {
          onlineLibraryViewModel.emitNoInternetSnackbar(context)
        } else {
          onNoContentChanged(context.getString(string.no_network_connection) to true)
        }
        onRefreshingChanged(false)
      }
      else -> {}
    }
  }
}

@Composable
private fun rememberStoragePermissionLauncher(
  onlineLibraryViewModel: OnlineLibraryViewModel,
  alertDialogShower: AlertDialogShower,
  activity: KiwixMainActivity,
  context: android.content.Context
): ActivityResultLauncher<String> =
  rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
    if (isGranted) {
      onlineLibraryViewModel.checkSpaceAndDownload(context, activity) {
        onlineLibraryViewModel.showStorageSelectDialog(activity)
      }
    } else {
      val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      )
      if (showRationale) {
        alertDialogShower.show(
          KiwixDialog.WriteStoragePermissionRationale,
          {
            ActivityCompat.requestPermissions(
              activity,
              arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
              org.kiwix.kiwixmobile.core.utils.REQUEST_STORAGE_PERMISSION
            )
          }
        )
      } else {
        alertDialogShower.show(
          KiwixDialog.WriteStoragePermissionRationale,
          activity::navigateToAppSettings
        )
      }
    }
  }

@Composable
private fun rememberNotificationPermissionLauncher(
  onlineLibraryViewModel: OnlineLibraryViewModel,
  activity: KiwixMainActivity,
  context: android.content.Context
): ActivityResultLauncher<String> =
  rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
    if (isGranted) {
      onlineLibraryViewModel.downloadBookItem?.let {
        kotlinx.coroutines.MainScope().launch {
          if (activity.getStorageDeviceList().isNotEmpty()) {
            onlineLibraryViewModel.checkSpaceAndDownload(context, activity) {
              onlineLibraryViewModel.showStorageSelectDialog(activity)
            }
          }
        }
      }
    }
  }

@Composable
private fun ObserveLibraryItems(
  libraryItems: ZimManageViewModel.LibraryListItemWrapper,
  context: android.content.Context,
  activity: KiwixMainActivity,
  onListChanged: (List<LibraryListItem>?) -> Unit,
  onNoContentChanged: (Pair<String, Boolean>) -> Unit,
  onLoadingMoreChanged: (Boolean) -> Unit
) {
  LaunchedEffect(libraryItems) {
    onListChanged(libraryItems.items)
    if (libraryItems.items.isEmpty()) {
      onNoContentChanged(
        if (!NetworkUtils.isNetworkAvailable(activity)) {
          context.getString(string.no_network_connection)
        } else {
          context.getString(string.no_items_msg)
        } to true
      )
    } else {
      onNoContentChanged("" to false)
    }
    onLoadingMoreChanged(false)
  }
}

@Composable
private fun ObserveDownloadingState(
  onlineLibraryDownloading: Pair<Boolean, Boolean>,
  onRefreshingChanged: (Boolean) -> Unit,
  onScanningProgressChanged: (Pair<Boolean, String>) -> Unit,
  onLoadingMoreChanged: (Boolean) -> Unit,
  context: android.content.Context
) {
  LaunchedEffect(onlineLibraryDownloading) {
    val (initialLibraryDownloading, loadingMoreItem) = onlineLibraryDownloading
    if (initialLibraryDownloading) {
      onRefreshingChanged(false)
      onScanningProgressChanged(true to context.getString(string.reaching_remote_library))
    } else {
      onRefreshingChanged(false)
      onScanningProgressChanged(false to context.getString(string.reaching_remote_library))
    }
    onLoadingMoreChanged(loadingMoreItem)
  }
}

private fun onStopButtonClick(
  onlineLibraryViewModel: OnlineLibraryViewModel,
  item: LibraryListItem.LibraryDownloadItem,
  activity: KiwixMainActivity,
  context: android.content.Context
) {
  if (item.currentDownloadState == com.tonyodev.fetch2.Status.FAILED) {
    when (item.downloadError) {
      com.tonyodev.fetch2.Error.UNKNOWN_IO_ERROR,
      com.tonyodev.fetch2.Error.CONNECTION_TIMED_OUT,
      com.tonyodev.fetch2.Error.UNKNOWN -> {
        if (!NetworkUtils.isNetworkAvailable(activity)) {
          onlineLibraryViewModel.emitNoInternetSnackbar(context)
        } else {
          onlineLibraryViewModel.downloader.retryDownload(item.downloadId)
        }
      }
      else -> {
        onlineLibraryViewModel.emitDialog(KiwixDialog.YesNoDialog.StopDownload) {
          onlineLibraryViewModel.downloader.cancelDownload(item.downloadId)
        }
      }
    }
  } else {
    onlineLibraryViewModel.emitDialog(KiwixDialog.YesNoDialog.StopDownload) {
      onlineLibraryViewModel.downloader.cancelDownload(item.downloadId)
    }
  }
}

private fun buildActionMenuItems(
  isSearchActive: Boolean,
  onSearchClick: () -> Unit,
  activity: KiwixMainActivity,
  navController: NavHostController
): List<ActionMenuItem> = listOfNotNull(
  if (!isSearchActive) {
    ActionMenuItem(
      icon = IconItem.Drawable(org.kiwix.kiwixmobile.core.R.drawable.action_search),
      contentDescription = string.search_label,
      onClick = onSearchClick,
      testingTag = SEARCH_ICON_TESTING_TAG
    )
  } else {
    null
  },
  ActionMenuItem(
    IconItem.Drawable(drawable.ic_category),
    org.kiwix.kiwixmobile.R.string.select_category,
    {
      val fragmentTransaction = activity.supportFragmentManager.beginTransaction()
      if (activity.supportFragmentManager
          .findFragmentByTag(ONLINE_CATEGORY_DIALOG_TAG) == null
      ) {
        OnlineCategoryDialog()
          .show(fragmentTransaction, ONLINE_CATEGORY_DIALOG_TAG)
      }
    },
    isEnabled = true,
    testingTag = CATEGORY_MENU_ICON_TESTING_TAG
  ),
  ActionMenuItem(
    IconItem.Drawable(drawable.ic_language_white_24dp),
    string.pref_language_chooser,
    {
      navController.navigate(KiwixDestination.Language.route)
      activity.currentFocus?.closeKeyboard()
    },
    isEnabled = true,
    testingTag = LANGUAGE_MENU_ICON_TESTING_TAG
  )
)

private fun handleBackPress(
  activity: KiwixMainActivity,
  isSearchActive: Boolean,
  closeSearch: () -> Unit
): FragmentActivityExtensions.Super {
  return if (activity.navigationDrawerIsOpen()) {
    activity.closeNavigationDrawer()
    FragmentActivityExtensions.Super.ShouldNotCall
  } else {
    val decorView = activity.window.decorView
    val insets = androidx.core.view.ViewCompat.getRootWindowInsets(decorView)
    val isKeyboardVisible =
      insets?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
    if (isKeyboardVisible || isSearchActive) {
      activity.currentFocus?.closeKeyboard()
      closeSearch()
      FragmentActivityExtensions.Super.ShouldNotCall
    } else {
      FragmentActivityExtensions.Super.ShouldCall
    }
  }
}
