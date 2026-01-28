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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import org.kiwix.kiwixmobile.localFileTransfer.DialogEvent
import org.kiwix.kiwixmobile.localFileTransfer.PermissionAction

@OptIn(ExperimentalPermissionsApi::class)
@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
internal fun HandlePermissionStateComponent(
  permissionState: PermissionAction?,
  onPermissionGranted: () -> Unit,
  showDialog: (DialogEvent) -> Unit,
  clearPermissionAction: () -> Unit
) {
  val locationPermissionState = rememberPermissionState(ACCESS_FINE_LOCATION)
  val externalStoragePermissionState = rememberPermissionState(WRITE_EXTERNAL_STORAGE)
  val nearbyWifiPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    rememberPermissionState(NEARBY_WIFI_DEVICES)
  } else {
    null
  }

  LaunchedEffect(
    locationPermissionState.status,
    externalStoragePermissionState.status,
    nearbyWifiPermissionState?.status
  ) {
    // chheck if all required permissions are granted
    val locationGranted = locationPermissionState.status.isGranted ||
      (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && nearbyWifiPermissionState?.status?.isGranted == true)

    val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      true // not needed on android 13+
    } else {
      externalStoragePermissionState.status.isGranted
    }

    if (locationGranted && storageGranted) {
      onPermissionGranted()
    }
  }

  LaunchedEffect(permissionState) {
    when (val action = permissionState) {
      is PermissionAction.RequestPermission -> {
        val state = when (action.permission) {
          NEARBY_WIFI_DEVICES -> nearbyWifiPermissionState
          ACCESS_FINE_LOCATION -> locationPermissionState
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
