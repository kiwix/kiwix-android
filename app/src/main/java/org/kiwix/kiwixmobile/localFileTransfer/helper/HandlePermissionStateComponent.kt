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

package org.kiwix.kiwixmobile.localFileTransfer.helper

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.NEARBY_WIFI_DEVICES
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import org.kiwix.kiwixmobile.localFileTransfer.DialogEvent
import org.kiwix.kiwixmobile.localFileTransfer.PermissionAction

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
internal fun HandlePermissionStateComponent(
  permissionState: PermissionAction?,
  isAndroid13orAbove: Boolean,
  isWriteExternalStoragePermissionRequired: Boolean,
  onPermissionGranted: () -> Unit,
  showDialog: (DialogEvent) -> Unit,
  clearPermissionAction: () -> Unit
) {
  // we are not requesting external storage permission for android 13+ and play store variants
  val externalStoragePermissionState = if (isWriteExternalStoragePermissionRequired) {
    rememberPermissionState(WRITE_EXTERNAL_STORAGE)
  } else {
    null
  }

  // On Android < 13 we request ACCESS_FINE_LOCATION,
  // while on Android 13+ we request NEARBY_WIFI_DEVICES
  val locationOrWifiPermissionState = if (isAndroid13orAbove) {
    rememberPermissionState(NEARBY_WIFI_DEVICES)
  } else {
    rememberPermissionState(ACCESS_FINE_LOCATION)
  }

  LaunchedEffect(
    locationOrWifiPermissionState.status,
    externalStoragePermissionState?.status
  ) {
    val locationGranted = locationOrWifiPermissionState.status.isGranted
    val storageGranted = externalStoragePermissionState?.status?.isGranted ?: true

    if (locationGranted && storageGranted) {
      onPermissionGranted()
    }
  }

  LaunchedEffect(
    externalStoragePermissionState?.status,
    locationOrWifiPermissionState.status
  ) {
    // check if all required permissions are granted
    val locationOrWifiGranted = locationOrWifiPermissionState.status.isGranted

    // value of param externalStoragePermissionState is null if this permission is not required
    // in that case we set it to true
    val storageGranted = externalStoragePermissionState?.status?.isGranted ?: true

    if (locationOrWifiGranted && storageGranted) {
      onPermissionGranted()
    }
  }

  LaunchedEffect(permissionState) {
    when (val action = permissionState) {
      is PermissionAction.RequestPermission -> {
        val state = when (action.permission) {
          NEARBY_WIFI_DEVICES, ACCESS_FINE_LOCATION -> locationOrWifiPermissionState
          WRITE_EXTERNAL_STORAGE -> externalStoragePermissionState
          else -> null
        }

        state?.let {
          when {
            it.status.isGranted -> {
              onPermissionGranted()
            }

            it.status.shouldShowRationale -> {
              val dialogEvent = when (action.permission) {
                NEARBY_WIFI_DEVICES -> DialogEvent.ShowNearbyWifiRationale
                ACCESS_FINE_LOCATION -> DialogEvent.ShowLocationRationale
                WRITE_EXTERNAL_STORAGE -> DialogEvent.ShowStorageRationale
                else -> null
              }
              dialogEvent?.let { event -> showDialog(event) }
            }

            else -> {
              // this gets called first time, when permission is not granted
              it.launchPermissionRequest()
            }
          }
        }

        clearPermissionAction()
      }

      null -> {}
    }
  }
}
