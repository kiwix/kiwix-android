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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
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
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TEN_DP
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixBasicDialogFrame
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryViewModel
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.ScrollToTop
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.utils.effects.ShowStorageSelectionDialog

const val LANGUAGE_MENU_ICON_TESTING_TAG = "languageMenuIconTestingTag"
const val CATEGORY_MENU_ICON_TESTING_TAG = "categoryMenuIconTestingTag"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Suppress("LongMethod")
@Composable
fun OnlineLibraryRoute(
  onlineLibraryViewModel: OnlineLibraryViewModel,
  categoryViewModel: CategoryViewModel,
  alertDialogShower: AlertDialogShower,
  navController: NavHostController,
  activity: KiwixMainActivity
) {
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

  // Collect UI events
  HandleUiEvents(
    viewModel = onlineLibraryViewModel,
    snackbarHostState = snackbarHostState,
    alertDialogShower = alertDialogShower,
    activity = activity,
    scope = scope,
    lazyListState = lazyListState,
    notificationPermission = notificationPermission,
    writePermissionState = writePermissionState
  )

  val actionMenuItems = buildActionMenuItems(
    isSearchActive = uiState.isSearchActive,
    onSearchClick = onlineLibraryViewModel::openSearchView,
    onCategoryClick = { onlineLibraryViewModel.setShowCategoryDialog(true) },
    activity = activity,
    navController = navController
  )

  LaunchedEffect(Unit) {
    onlineLibraryViewModel.loadInitialLibrary()
  }

  OnlineLibraryScreen(
    uiState = uiState,
    onlineLibraryViewModel = onlineLibraryViewModel,
    actionMenuItems = actionMenuItems,
    listState = lazyListState,
    snackBarHostState = snackbarHostState,
    bottomAppBarScrollBehaviour = activity.bottomAppBarScrollBehaviour,
    onUserBackPressed = {
      handleBackPress(activity, uiState.isSearchActive) {
        onlineLibraryViewModel.closeSearchView()
      }
    },
    navHostController = navController,
    activity = activity,
    navigationIcon = {
      NavigationIcon(
        iconItem = if (uiState.isSearchActive) {
          IconItem.Vector(Icons.AutoMirrored.Filled.ArrowBack)
        } else {
          IconItem.Vector(Icons.Filled.Menu)
        },
        contentDescription = string.open_drawer,
        onClick = {
          if (uiState.isSearchActive) {
            onlineLibraryViewModel.closeSearchView()
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

  ShowCategoryDialog(categoryViewModel, uiState.showCategoryDialog) {
    onlineLibraryViewModel.setShowCategoryDialog(false)
  }
}

@OptIn(ExperimentalPermissionsApi::class)
@Suppress("LongParameterList", "LongMethod")
@Composable
private fun HandleUiEvents(
  viewModel: OnlineLibraryViewModel,
  snackbarHostState: SnackbarHostState,
  alertDialogShower: AlertDialogShower,
  activity: KiwixMainActivity,
  scope: CoroutineScope,
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
                activity.startActivity(intent)
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

        is OnlineLibraryViewModel.UiEvent.SideEffects -> handleUISideEffects(
          event.uISideEffects,
          alertDialogShower = alertDialogShower,
          kiwixMainActivity = activity
        )

        is OnlineLibraryViewModel.UiEvent.ShowToast -> {
          activity.toast(event.message, Toast.LENGTH_SHORT)
        }

        is OnlineLibraryViewModel.UiEvent.RequestPermission -> {
          handlePermissionEvents(notificationPermission, event.permission, writePermissionState)
        }

        is OnlineLibraryViewModel.UiEvent.NavigateToSettings -> {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.toast(
              activity.getString(string.all_files_permission_needed),
              Toast.LENGTH_SHORT
            )
            activity.navigateToSettings()
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

private fun handleUISideEffects(
  event: OnlineLibraryViewModel.UISideEffects,
  alertDialogShower: AlertDialogShower,
  kiwixMainActivity: KiwixMainActivity
) {
  when (event) {
    is OnlineLibraryViewModel.UISideEffects.StorageSelectionDialog -> {
      ShowStorageSelectionDialog(alertDialogShower, event.dialogConfig)
        .invokeWith(kiwixMainActivity)
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

private fun buildActionMenuItems(
  isSearchActive: Boolean,
  onSearchClick: () -> Unit,
  onCategoryClick: () -> Unit,
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
    onCategoryClick,
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

@Composable
private fun ShowCategoryDialog(
  categoryViewModel: CategoryViewModel,
  showCategoryDialog: Boolean,
  onDismiss: () -> Unit
) {
  if (!showCategoryDialog) return
  categoryViewModel.apply {
    setOnDismissCallback(onDismiss)
    onDialogOpened()
  }
  KiwixBasicDialogFrame(
    onDismissRequest = onDismiss,
    dialogPadding = TEN_DP,
    topPaddingForContent = ZERO.dp
  ) {
    OnlineCategoryDialogScreen(
      categoryViewModel = categoryViewModel,
      navigationIcon = {
        NavigationIcon(
          iconItem = IconItem.Vector(Icons.AutoMirrored.Filled.ArrowBack),
          contentDescription = string.close_drawer,
          onClick = onDismiss
        )
      }
    )
  }
}
