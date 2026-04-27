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

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.R.drawable
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.extensions.closeKeyboard
import org.kiwix.kiwixmobile.core.extensions.navigateToAppSettings
import org.kiwix.kiwixmobile.core.extensions.navigateToSettings
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.utils.NetworkUtils
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.UiEvent.ScrollToTop
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem

const val LANGUAGE_MENU_ICON_TESTING_TAG = "languageMenuIconTestingTag"
const val CATEGORY_MENU_ICON_TESTING_TAG = "categoryMenuIconTestingTag"

@Suppress("UnusedPrivateProperty")
private const val ZERO = 0

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Suppress("MagicNumber", "LongMethod")
@Composable
fun OnlineLibraryRoute(
  viewModelFactory: ViewModelProvider.Factory,
  alertDialogShower: AlertDialogShower,
  navController: NavHostController
) {
  val context = LocalContext.current
  val activity = context as KiwixMainActivity
  val onlineLibraryViewModel: OnlineLibraryViewModel =
    viewModel(viewModelStoreOwner = activity, factory = viewModelFactory)
  val uiState by onlineLibraryViewModel.uiState.collectAsState()
  val notificationPermission = if (onlineLibraryViewModel.isAndroid13OrAbove) {
    rememberPermissionState(POST_NOTIFICATIONS) {
      onlineLibraryViewModel.onNotificationPermissionResult(it, activity)
    }
  } else {
    null
  }
  val writePermissionState = rememberPermissionState(WRITE_EXTERNAL_STORAGE) {
    onlineLibraryViewModel.onStoragePermissionResult(it, activity)
  }
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val lazyListState = rememberLazyListState()

  var isSearchActive by remember { mutableStateOf(false) }
  var onlineLibraryList by remember { mutableStateOf<List<LibraryListItem>?>(null) }

  val libraryItems by onlineLibraryViewModel.libraryItems.collectAsState()

  // Collect UI events
  HandleUiEvents(
    viewModel = onlineLibraryViewModel,
    snackbarHostState = snackbarHostState,
    alertDialogShower = alertDialogShower,
    activity = activity,
    scope = scope,
    context = context,
    lazyListState = lazyListState,
    notificationPermission = notificationPermission,
    writePermissionState = writePermissionState
  )

  // Handle side-effects (Network, Wifi, Progress, etc)
  HandleEffects(
    onlineLibraryViewModel = onlineLibraryViewModel,
    isListEmpty = onlineLibraryList.isNullOrEmpty(),
    onNoContentChanged = { noContentViewItem = it },
    onRefreshingChanged = { isRefreshing = it }
  )

  ObserveLibraryItems(
    libraryItems = libraryItems,
    onListChanged = { onlineLibraryList = it },
    onLoadingMoreChanged = { isLoadingMoreItem = it }
  )

  // ObserveDownloadingState(
  //   onlineLibraryDownloading = onlineLibraryDownloading,
  //   onRefreshingChanged = { isRefreshing = it },
  //   onScanningProgressChanged = { scanningProgressItem = it },
  //   onLoadingMoreChanged = { isLoadingMoreItem = it },
  //   context = context
  // )

  LaunchedEffect(Unit) {
    onlineLibraryViewModel.updateOnlineLibraryFilters(
      onlineLibraryViewModel.getOnlineLibraryRequest()
    )
    scanningProgressItem = false to context.getString(string.reaching_remote_library)
  }

  val actionMenuItems = buildActionMenuItems(
    isSearchActive = isSearchActive,
    onSearchClick = { isSearchActive = true },
    activity = activity,
    navController = navController
  )

  OnlineLibraryScreen(
    uiState = uiState,
    onlineLibraryViewModel = onlineLibraryViewModel,
    actionMenuItems = actionMenuItems,
    listState = lazyListState,
    bottomAppBarScrollBehaviour = activity.bottomAppBarScrollBehaviour,
    onUserBackPressed = {
      handleBackPress(activity, uiState.isSearchActive) {
        onlineLibraryViewModel.closeSearchView()
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
          if (uiState.isSearchActive) {
            onlineLibraryViewModel.closeSearchView()
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
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun HandleUiEvents(
  viewModel: OnlineLibraryViewModel,
  snackbarHostState: SnackbarHostState,
  alertDialogShower: AlertDialogShower,
  activity: KiwixMainActivity,
  scope: CoroutineScope,
  context: Context,
  lazyListState: LazyListState,
  notificationPermission: PermissionState?,
  writePermissionState: PermissionState
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
              event.actionIntent?.let { intent: Intent ->
                context.startActivity(intent)
              }
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
          handlePermissionEvents(notificationPermission, event.permission, writePermissionState)
        }

        is OnlineLibraryViewModel.UiEvent.NavigateToSettings -> {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.toast(context.getString(string.all_files_permission_needed), Toast.LENGTH_SHORT)
            (activity as FragmentActivity).navigateToSettings()
          }
        }

        is OnlineLibraryViewModel.UiEvent.NavigateToAppSettings -> {
          activity.navigateToAppSettings()
        }

        ScrollToTop -> lazyListState.scrollToItem(ZERO)
      }
    }
  }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun handlePermissionEvents(
  notificationPermission: PermissionState?,
  permission: String,
  writePermissionState: PermissionState
) {
  when (permission) {
    POST_NOTIFICATIONS -> notificationPermission?.launchPermissionRequest()
    WRITE_EXTERNAL_STORAGE -> writePermissionState.launchPermissionRequest()
  }
}

@Composable
@Suppress("LongMethod", "ComplexMethod")
private fun HandleEffects(
  onlineLibraryViewModel: OnlineLibraryViewModel,
  isListEmpty: Boolean,
  onNoContentChanged: (Pair<String, Boolean>) -> Unit,
  onRefreshingChanged: (Boolean) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  // Observe LiveData from ZimManageViewModel using observeAsState (Compose-LiveData bridge)
  val shouldShowWifiOnly by onlineLibraryViewModel.shouldShowWifiOnlyDialog.observeAsState(false)
  val networkState by onlineLibraryViewModel.networkStates.observeAsState()

  // Handle wifi-only dialog
  LaunchedEffect(shouldShowWifiOnly) {
    if (shouldShowWifiOnly && !NetworkUtils.isWiFi(context)) {
      onlineLibraryViewModel.emitDialog(
        KiwixDialog.YesNoDialog.WifiOnly,
        positiveAction = {
          onNoContentChanged("" to false)
          scope.launch {
            onlineLibraryViewModel.kiwixDataStore.setWifiOnly(false)
            onlineLibraryViewModel.shouldShowWifiOnlyDialog.value = false
            onlineLibraryViewModel.updateOnlineLibraryFilters(
              onlineLibraryViewModel.getOnlineLibraryRequest()
            )
          }
        },
        negativeAction = {
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

  // Handle network state changes
  LaunchedEffect(networkState) {
    when (networkState) {
      NetworkState.CONNECTED -> {
        onlineLibraryViewModel.handleNetworkConnected()
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
private fun ObserveLibraryItems(
  libraryItems: ZimManageViewModel.LibraryListItemWrapper,
  onListChanged: (List<LibraryListItem>?) -> Unit,
  onLoadingMoreChanged: (Boolean) -> Unit
) {
  LaunchedEffect(libraryItems) {
    onListChanged(libraryItems.items)
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

private fun buildActionMenuItems(
  isSearchActive: Boolean,
  onSearchClick: () -> Unit,
  activity: KiwixMainActivity,
  navController: NavHostController
): List<ActionMenuItem> = listOfNotNull(
  if (!isSearchActive) {
    ActionMenuItem(
      icon = IconItem.Drawable(R.drawable.action_search),
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
    val insets = ViewCompat.getRootWindowInsets(decorView)
    val isKeyboardVisible =
      insets?.isVisible(WindowInsetsCompat.Type.ime()) == true
    if (isKeyboardVisible || isSearchActive) {
      activity.currentFocus?.closeKeyboard()
      closeSearch()
      FragmentActivityExtensions.Super.ShouldNotCall
    } else {
      FragmentActivityExtensions.Super.ShouldCall
    }
  }
}
