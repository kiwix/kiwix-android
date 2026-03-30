/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.webserver

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isCustomApp
import org.kiwix.kiwixmobile.core.extensions.handlePermissionRequest
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.navigateToSettings
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.AllFilesPermissionDialog
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.AskNotificationPermission
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.AskReadWritePermission
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.DismissDialog
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.NotificationPermissionRationaleDialog
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.ReadPermissionRationaleDialog
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.ShowErrorToast
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.ShowManualHotspotDialog
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.ShowNoBooksToast
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.ShowWifiDialog
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.StartIpCheck
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.StartServer
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.StopServer
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.HotspotService
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.HotspotService.Companion.ACTION_CHECK_IP_ADDRESS
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.HotspotService.Companion.ACTION_START_SERVER
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.HotspotService.Companion.ACTION_STOP_SERVER

const val SELECTED_ZIM_PATHS_KEY = "selected_zim_paths"
const val RESTART_SERVER = "restart_server"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ZimHostRoute(
  viewModel: ZimHostViewModel,
  alertDialogShower: AlertDialogShower,
  activity: CoreMainActivity
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val notificationPermission = if (viewModel.isAndroid13OrAbove) {
    rememberPermissionState(POST_NOTIFICATIONS)
  } else {
    null
  }
  val readWritePermission =
    rememberMultiplePermissionsState(listOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE))
  BindHotspotService(activity, viewModel)
  LaunchedEffect(lifecycleOwner) {
    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
      viewModel.loadBooks(activity.isCustomApp())
    }
  }
  LaunchedEffect(lifecycleOwner) {
    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
      viewModel.events.collect { event ->
        handleUiEvent(
          event,
          activity,
          alertDialogShower,
          viewModel,
          notificationPermission,
          readWritePermission
        )
      }
    }
  }
  ZimHostScreen(
    serverIpText = uiState.serverIpDisplayText,
    showShareIcon = uiState.showShareIcon,
    shareIconClick = {
      activity.startActivity(
        Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_TEXT, uiState.serverIpAddress)
        }
      )
    },
    qrImageItem = uiState.qrVisible to uiState.qrIcon,
    booksList = uiState.books,
    startServerButtonItem = Triple(
      stringResource(uiState.startServerButtonTextRes),
      uiState.startServerButtonColor
    ) { viewModel.startServerButtonClick() },
    selectionMode = SelectionMode.MULTI,
    onMultiSelect = { viewModel.onBookSelected(it) },
    navigationIcon = {
      NavigationIcon(onClick = {
        activity.onBackPressedDispatcher.onBackPressed()
      })
    }
  )
}

@Composable
private fun BindHotspotService(
  activity: CoreMainActivity,
  viewModel: ZimHostViewModel
) {
  var boundService by remember { mutableStateOf<HotspotService?>(null) }

  val connection = remember {
    object : ServiceConnection {
      override fun onServiceConnected(name: ComponentName?, binder: IBinder) {
        val service = (binder as HotspotService.HotspotBinder).service.get()
        boundService = service
        service?.registerCallBack(viewModel)
      }

      override fun onServiceDisconnected(name: ComponentName?) {
        // Do Nothing
      }
    }
  }

  DisposableEffect(Unit) {
    activity.bindService(
      Intent(activity, HotspotService::class.java),
      connection,
      Context.BIND_AUTO_CREATE
    )
    onDispose {
      boundService?.registerCallBack(null)
      runCatching { activity.unbindService(connection) }
    }
  }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun handleUiEvent(
  event: ZimHostViewModel.Event,
  context: Context,
  alertDialogShower: AlertDialogShower,
  viewModel: ZimHostViewModel,
  notificationPermission: PermissionState?,
  readWritePermission: MultiplePermissionsState
) {
  when (event) {
    is StartIpCheck,
    is StartServer,
    is StopServer -> handleServerEvents(event, context, alertDialogShower)

    is ShowWifiDialog,
    is ShowManualHotspotDialog -> handleDialogEvents(event, context, alertDialogShower, viewModel)

    is ShowNoBooksToast,
    is ShowErrorToast -> handleToastEvents(event, context)

    AskNotificationPermission,
    NotificationPermissionRationaleDialog,
    AskReadWritePermission,
    ReadPermissionRationaleDialog,
    AllFilesPermissionDialog ->
      handlePermissionEvents(
        event,
        context,
        alertDialogShower,
        viewModel,
        notificationPermission,
        readWritePermission
      )

    DismissDialog -> alertDialogShower.dismiss()
  }
}

private fun handleServerEvents(
  event: ZimHostViewModel.Event,
  context: Context,
  alertDialogShower: AlertDialogShower
) {
  when (event) {
    is StartIpCheck -> {
      alertDialogShower.show(KiwixDialog.StartServer { ContentLoadingProgressBar() })
      startHotspotService(context, ACTION_CHECK_IP_ADDRESS)
    }

    is StartServer -> {
      startHotspotService(context, ACTION_START_SERVER) {
        putStringArrayListExtra(SELECTED_ZIM_PATHS_KEY, event.paths)
        putExtra(RESTART_SERVER, event.restart)
      }
    }

    is StopServer -> {
      startHotspotService(context, ACTION_STOP_SERVER)
    }

    else -> Unit
  }
}

fun startHotspotService(context: Context, action: String, block: Intent.() -> Unit = {}) {
  context.startService(createHotspotIntent(context, action).apply(block))
}

private fun handleDialogEvents(
  event: Event,
  context: Context,
  alertDialogShower: AlertDialogShower,
  viewModel: ZimHostViewModel
) {
  when (event) {
    is ShowWifiDialog -> {
      alertDialogShower.show(
        KiwixDialog.WiFiOnWhenHostingBooks,
        { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
        {},
        { viewModel.onWifiConfirmed() }
      )
    }

    is ShowManualHotspotDialog -> {
      alertDialogShower.show(
        KiwixDialog.StartHotspotManually,
        {
          runCatching {
            // Try to open the device's dedicated hotspot/tethering screen.
            // Most AOSP-based devices support this explicit Settings component.
            context.startActivity(
              Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(
                  "com.android.settings",
                  "com.android.settings.TetherSettings"
                )
              }
            )
          }.onFailure {
            // Some OEMs remove or rename the tethering activity, so the direct intent may fail.
            // As a fallback, open the Wireless settings screen—this reliably contains the Hotspot option.
            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
          }
        }
      )
    }

    else -> Unit
  }
}

private fun handleToastEvents(event: Event, context: Context) {
  when (event) {
    is ShowNoBooksToast ->
      context.toast(R.string.no_books_selected_toast_message, Toast.LENGTH_SHORT)

    is ShowErrorToast ->
      context.toast(event.messageRes, Toast.LENGTH_SHORT)

    else -> Unit
  }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun handlePermissionEvents(
  event: ZimHostViewModel.Event,
  context: Context,
  alertDialogShower: AlertDialogShower,
  viewModel: ZimHostViewModel,
  notificationPermission: PermissionState?,
  readWritePermission: MultiplePermissionsState
) {
  when (event) {
    AskNotificationPermission -> {
      notificationPermission?.handlePermissionRequest(viewModel::startServerButtonClick) {
        viewModel.showNotificationPermissionRationaleDialog()
      }
    }

    NotificationPermissionRationaleDialog -> {
      alertDialogShower.show(
        KiwixDialog.NotificationPermissionDialog,
        { context.navigateToAppSettings() }
      )
    }

    AskReadWritePermission -> {
      readWritePermission.handlePermissionRequest(viewModel::startServerButtonClick) {
        viewModel.showReadPermissionRationalDialog()
      }
    }

    ReadPermissionRationaleDialog -> {
      alertDialogShower.show(
        KiwixDialog.ReadPermissionRequired,
        { context.navigateToAppSettings() }
      )
    }

    AllFilesPermissionDialog -> {
      if (viewModel.isAndroid13OrAbove) {
        alertDialogShower.show(
          KiwixDialog.ManageExternalFilesPermissionDialog,
          { context.navigateToSettings() }
        )
      }
    }

    else -> Unit
  }
}

private fun createHotspotIntent(context: Context, action: String): Intent =
  Intent(context, HotspotService::class.java).setAction(action)
