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

package org.kiwix.kiwixmobile.localFileTransfer

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.NEARBY_WIFI_DEVICES
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.extensions.handlePermissionRequest
import org.kiwix.kiwixmobile.core.extensions.navigateToAppSettings
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.localFileTransfer.UiEvent.NavigateBack
import org.kiwix.kiwixmobile.localFileTransfer.UiEvent.RequestPermission
import org.kiwix.kiwixmobile.localFileTransfer.UiEvent.ShowDialog

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun LocalFileTransferScreenRoute(
  viewModel: LocalFileTransferViewModel,
  alertDialogShower: AlertDialogShower,
  navigateBack: () -> Unit
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val locationPermission = rememberPermissionState(viewModel.locationPermission)
  val storagePermission = if (uiState.isWritePermissionRequired) {
    rememberPermissionState(WRITE_EXTERNAL_STORAGE)
  } else {
    null
  }
  val enableLocationServicesLauncher =
    rememberLauncherForActivityResult(StartActivityForResult()) {
      if (it.resultCode != Activity.RESULT_OK && !viewModel.isLocationServiceEnabled) {
        context.toast(string.permission_refused_location)
      }
    }
  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        NavigateBack -> navigateBack()
        is ShowDialog -> handleDialog(
          dialog = event.dialog,
          context = context,
          alertDialogShower = alertDialogShower,
          enableLocationServicesLauncher = enableLocationServicesLauncher
        )

        is RequestPermission -> handlePermissionRequest(
          permission = event.permission,
          locationPermission = locationPermission,
          storagePermission = storagePermission,
          onPermissionGranted = viewModel::onPermissionGranted,
          onPermissionRationaleRequired = viewModel::onPermissionRationaleRequired
        )
      }
    }
  }

  KiwixTheme {
    LocalFileTransferScreen(
      state = uiState,
      actionMenuItems = actionMenuItems(viewModel::onSearchMenuClicked),
      onDeviceItemClick = viewModel::onDeviceSelected,
      onShowCaseDisplayed = viewModel::onShowCaseDisplayed,
      navigationIcon = {
        NavigationIcon(
          iconItem = IconItem.Drawable(R.drawable.ic_close_white_24dp),
          onClick = navigateBack
        )
      }
    )
  }
}

@Composable
private fun actionMenuItems(onSearchMenuClicked: () -> Unit) = remember {
  listOf(
    ActionMenuItem(
      IconItem.Vector(Icons.Default.Search),
      string.search_label,
      onSearchMenuClicked,
      testingTag = SEARCH_ICON_TESTING_TAG
    )
  )
}

private fun handleDialog(
  dialog: DialogEvent,
  context: Context,
  alertDialogShower: AlertDialogShower,
  enableLocationServicesLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
  when (dialog) {
    DialogEvent.ShowNearbyWifiRationale ->
      alertDialogShower.show(
        KiwixDialog.NearbyWifiPermissionRationale,
        { context.navigateToAppSettings() },
        { context.toast(string.discovery_needs_wifi) }
      )

    DialogEvent.ShowLocationRationale ->
      alertDialogShower.show(
        KiwixDialog.LocationPermissionRationale,
        { context.navigateToAppSettings() },
        { context.toast(string.discovery_needs_location) }
      )

    DialogEvent.ShowStorageRationale ->
      alertDialogShower.show(
        KiwixDialog.StoragePermissionRationale,
        { context.navigateToAppSettings() },
        { context.toast(string.storage_permission_denied) }
      )

    DialogEvent.ShowEnableWifiP2p -> {
      alertDialogShower.show(
        KiwixDialog.EnableWifiP2pServices,
        { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
        { context.toast(string.discovery_needs_wifi, Toast.LENGTH_SHORT) }
      )
    }

    DialogEvent.ShowEnableLocationServices -> {
      alertDialogShower.show(
        KiwixDialog.EnableLocationServices,
        { enableLocationServicesLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
        { context.toast(string.discovery_needs_location) }
      )
    }
  }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun handlePermissionRequest(
  permission: String,
  locationPermission: PermissionState,
  storagePermission: PermissionState?,
  onPermissionGranted: () -> Unit,
  onPermissionRationaleRequired: (String) -> Unit
) {
  val state = when (permission) {
    NEARBY_WIFI_DEVICES, ACCESS_FINE_LOCATION -> locationPermission
    WRITE_EXTERNAL_STORAGE -> storagePermission
    else -> null
  } ?: return

  state.handlePermissionRequest(onPermissionGranted) {
    onPermissionRationaleRequired(state.permission)
  }
}
