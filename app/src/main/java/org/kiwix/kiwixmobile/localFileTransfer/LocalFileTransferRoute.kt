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
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.localFileTransfer.UiEvent.NavigateBack
import org.kiwix.kiwixmobile.localFileTransfer.UiEvent.RequestPermission
import org.kiwix.kiwixmobile.localFileTransfer.UiEvent.ShowDialog
import org.kiwix.kiwixmobile.localFileTransfer.helper.FileTransferDialogComponent

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("InlinedApi")
@Suppress("LongMethod")
@Composable
internal fun LocalFileTransferScreenRoute(
  viewModel: LocalFileTransferViewModel,
  alertDialogShower: AlertDialogShower,
  navigateBack: () -> Unit
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  val locationPermission = if (viewModel.android13OrAbove) {
    rememberPermissionState(NEARBY_WIFI_DEVICES)
  } else {
    rememberPermissionState(ACCESS_FINE_LOCATION)
  }

  val storagePermission = if (uiState.isWritePermissionRequired) {
    rememberPermissionState(WRITE_EXTERNAL_STORAGE)
  } else {
    null
  }

  val context = LocalContext.current

  val enableLocationServicesLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode != Activity.RESULT_OK) {
      if (!viewModel.isLocationServiceEnabled) {
        Toast.makeText(context, string.permission_refused_location, Toast.LENGTH_SHORT).show()
      }
    }
  }
  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        NavigateBack -> navigateBack()
        is ShowDialog -> viewModel.showDialog(event.dialog)
        is RequestPermission -> handlePermissionRequest(
          permission = event.permission,
          locationPermission = locationPermission,
          storagePermission = storagePermission,
          onPermissionGranted = viewModel::onPermissionGranted,
          showDialog = viewModel::showDialog
        )
      }
    }
  }

  LaunchedEffect(locationPermission.status, storagePermission?.status) {
    if (arePermissionsGranted(locationPermission, storagePermission)) {
      viewModel.onPermissionGranted()
    }
  }

  FileTransferDialogComponent(
    context = context,
    viewModel = viewModel,
    alertDialogShower = alertDialogShower,
    enableLocationServicesLauncher = {
      enableLocationServicesLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }
  )

  val actionMenuItems = remember {
    listOf(
      ActionMenuItem(
        IconItem.Vector(Icons.Default.Search),
        string.search_label,
        { viewModel.onSearchMenuClicked() },
        testingTag = SEARCH_ICON_TESTING_TAG
      )
    )
  }

  KiwixTheme {
    LocalFileTransferScreen(
      state = uiState,
      actionMenuItems = actionMenuItems,
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

@OptIn(ExperimentalPermissionsApi::class)
private fun handlePermissionRequest(
  permission: String,
  locationPermission: PermissionState,
  storagePermission: PermissionState?,
  onPermissionGranted: () -> Unit,
  showDialog: (DialogEvent) -> Unit
) {
  val state = when (permission) {
    NEARBY_WIFI_DEVICES, ACCESS_FINE_LOCATION -> locationPermission
    WRITE_EXTERNAL_STORAGE -> storagePermission
    else -> null
  } ?: return

  when {
    state.status.isGranted -> {
      onPermissionGranted()
    }

    state.status.shouldShowRationale -> {
      resolveDialog(permission)?.let(showDialog)
    }

    else -> {
      state.launchPermissionRequest()
    }
  }
}

private fun resolveDialog(permission: String): DialogEvent? {
  return when (permission) {
    NEARBY_WIFI_DEVICES -> DialogEvent.ShowNearbyWifiRationale
    ACCESS_FINE_LOCATION -> DialogEvent.ShowLocationRationale
    WRITE_EXTERNAL_STORAGE -> DialogEvent.ShowStorageRationale
    else -> null
  }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun arePermissionsGranted(
  locationPermission: PermissionState,
  storagePermission: PermissionState?
): Boolean {
  val locationGranted = locationPermission.status.isGranted
  val storageGranted = storagePermission?.status?.isGranted ?: true
  return locationGranted && storageGranted
}
