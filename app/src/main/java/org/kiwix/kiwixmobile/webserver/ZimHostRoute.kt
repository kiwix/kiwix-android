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

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isCustomApp
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.utils.REQUEST_POST_NOTIFICATION_PERMISSION
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.DismissDialog
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

const val REQUEST_STORAGE_PERMISSION = 1001

@Composable
fun ZimHostRoute(
  viewModelFactory: ViewModelProvider.Factory,
  alertDialogShower: AlertDialogShower
) {
  val activity = LocalActivity.current as KiwixMainActivity
  val lifecycleOwner = LocalLifecycleOwner.current
  val viewModel: ZimHostViewModel = viewModel(factory = viewModelFactory)
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  BindHotspotService(activity, viewModel)

  LaunchedEffect(lifecycleOwner) {
    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
      viewModel.loadBooks(activity.isCustomApp())
    }
  }
  CollectZimHostEvents(lifecycleOwner, viewModel, activity, alertDialogShower)
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
    ) {
      handlePermissionsAndStart(
        activity,
        uiState,
        alertDialogShower,
        viewModel
      )
    },
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
  activity: KiwixMainActivity,
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

@Composable
private fun CollectZimHostEvents(
  lifecycleOwner: LifecycleOwner,
  viewModel: ZimHostViewModel,
  activity: KiwixMainActivity,
  alertDialogShower: AlertDialogShower
) {
  LaunchedEffect(lifecycleOwner) {
    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
      viewModel.events.collect { event ->
        handleZimHostEvent(
          event = event,
          context = activity,
          alertDialogShower = alertDialogShower,
          viewModel = viewModel
        )
      }
    }
  }
}

private fun handleZimHostEvent(
  event: ZimHostViewModel.Event,
  context: Context,
  alertDialogShower: AlertDialogShower,
  viewModel: ZimHostViewModel
) {
  when (event) {
    is StartIpCheck ->
      handleStartIpCheck(context, alertDialogShower)

    is StartServer ->
      handleStartServer(context, event)

    is StopServer ->
      handleStopServer(context)

    is ShowWifiDialog ->
      handleWifiDialog(context, alertDialogShower, viewModel)

    is ShowManualHotspotDialog ->
      handleManualHotspotDialog(context, alertDialogShower)

    is ShowNoBooksToast ->
      showNoBooksToast(context)

    is ShowErrorToast ->
      showErrorToast(context, event.messageRes)

    is DismissDialog ->
      alertDialogShower.dismiss()
  }
}

private fun handleStartIpCheck(context: Context, alertDialogShower: AlertDialogShower) {
  alertDialogShower.show(KiwixDialog.StartServer { ContentLoadingProgressBar() })
  context.startService(createHotspotIntent(context, ACTION_CHECK_IP_ADDRESS))
}

private fun handleStartServer(context: Context, event: StartServer) {
  context.startService(
    createHotspotIntent(context, ACTION_START_SERVER)
      .putStringArrayListExtra(SELECTED_ZIM_PATHS_KEY, event.paths)
      .putExtra(RESTART_SERVER, event.restart)
  )
}

private fun handleStopServer(context: Context) {
  context.startService(createHotspotIntent(context, ACTION_STOP_SERVER))
}

private fun createHotspotIntent(context: Context, action: String): Intent =
  Intent(context, HotspotService::class.java).setAction(action)

private fun handleWifiDialog(
  context: Context,
  alertDialogShower: AlertDialogShower,
  viewModel: ZimHostViewModel
) {
  alertDialogShower.show(
    KiwixDialog.WiFiOnWhenHostingBooks,
    { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
    {},
    { viewModel.onWifiConfirmed() }
  )
}

private fun handleManualHotspotDialog(
  context: Context,
  alertDialogShower: AlertDialogShower
) {
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
        context.startActivity(
          Intent(Settings.ACTION_WIRELESS_SETTINGS)
        )
      }
    }
  )
}

private fun showNoBooksToast(context: Context) {
  context.toast(R.string.no_books_selected_toast_message, Toast.LENGTH_SHORT)
}

private fun showErrorToast(context: Context, messageRes: Int) {
  context.toast(messageRes, Toast.LENGTH_SHORT)
}

// ===== Permission Handling =====
@Suppress("ReturnCount")
private fun handlePermissionsAndStart(
  activity: KiwixMainActivity,
  uiState: ZimHostViewModel.UiState,
  alertDialogShower: AlertDialogShower,
  viewModel: ZimHostViewModel
) {
  // Handles Android 13+ Notification Permission
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    val granted = ContextCompat.checkSelfPermission(
      activity, Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED

    if (!granted) {
      ActivityCompat.requestPermissions(
        activity,
        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
        REQUEST_POST_NOTIFICATION_PERMISSION
      )
      return
    }
  }

  // Handles Play Store
  if (uiState.isPlayStoreBuildWithAndroid11OrAbove) {
    viewModel.startServerButtonClick()
    return
  }

  // Handles Android 11+ Manage External Storage (Non-Custom Apps)
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
    !activity.isCustomApp() &&
    !Environment.isExternalStorageManager()
  ) {
    alertDialogShower.show(
      KiwixDialog.ManageExternalFilesPermissionDialog,
      {
        activity.startActivity(
          Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        )
      }
    )
    return
  }

  // Handles Android 10 below Storage permission
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
    val granted = ContextCompat.checkSelfPermission(
      activity, Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED

    if (!granted) {
      ActivityCompat.requestPermissions(
        activity,
        arrayOf(
          Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.WRITE_EXTERNAL_STORAGE
        ),
        REQUEST_STORAGE_PERMISSION
      )
      return
    }
  }

  viewModel.startServerButtonClick()
}
