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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.localFileTransfer.DialogEvent
import org.kiwix.kiwixmobile.localFileTransfer.LocalFileTransferViewModel

@Composable
@Suppress("LongMethod")
internal fun FileTransferDialogComponent(
  context: Context,
  alertDialogShower: AlertDialogShower,
  viewModel: LocalFileTransferViewModel,
  enableLocationServicesLauncher: () -> Unit
) {
  LaunchedEffect(Unit) {
    viewModel.dialogEvent.collect { dialogEvent ->
      when (dialogEvent) {
        DialogEvent.ShowNearbyWifiRationale -> {
          (context as? Activity)?.let { activity ->
            alertDialogShower.show(
              KiwixDialog.NearbyWifiPermissionRationale,
              {
                activity.navigateToAppSettings()
              },
              {
                Toast.makeText(context, string.discovery_needs_wifi, Toast.LENGTH_SHORT).show()
              }
            )
          }
        }

        DialogEvent.ShowLocationRationale -> {
          (context as? Activity)?.let { activity ->
            alertDialogShower.show(
              KiwixDialog.LocationPermissionRationale,
              {
                activity.navigateToAppSettings()
              },
              {
                Toast.makeText(context, string.discovery_needs_location, Toast.LENGTH_SHORT).show()
              }
            )
          }
        }

        DialogEvent.ShowStorageRationale -> {
          (context as? Activity)?.let { activity ->
            alertDialogShower.show(
              KiwixDialog.StoragePermissionRationale,
              {
                activity.navigateToAppSettings()
              },
              {
                Toast.makeText(context, string.storage_permission_denied, Toast.LENGTH_SHORT).show()
              }
            )
          }
        }

        DialogEvent.ShowEnableWifiP2p -> {
          alertDialogShower.show(
            KiwixDialog.EnableWifiP2pServices,
            {
              context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            },
            {
              Toast.makeText(context, string.discovery_needs_wifi, Toast.LENGTH_SHORT).show()
            }
          )
        }

        DialogEvent.ShowEnableLocationServices -> {
          alertDialogShower.show(
            KiwixDialog.EnableLocationServices,
            {
              enableLocationServicesLauncher()
            },
            {
              Toast.makeText(context, string.discovery_needs_location, Toast.LENGTH_SHORT).show()
            }
          )
        }
      }
    }
  }
}
