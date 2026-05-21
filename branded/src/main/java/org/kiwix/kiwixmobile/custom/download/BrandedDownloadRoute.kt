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

package org.kiwix.kiwixmobile.custom.download

import android.Manifest.permission.POST_NOTIFICATIONS
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.requestNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.navigateToAppSettings
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.custom.download.Action.ClickedDownload
import org.kiwix.kiwixmobile.custom.download.Action.ClickedRetry

@Composable
fun BrandedDownloadRoute(
  brandedDownloadViewModel: BrandedDownloadViewModel,
  alertDialogShower: AlertDialogShower
) {
  val activity = LocalActivity.current as CoreMainActivity
  val state by brandedDownloadViewModel.state.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) {
    brandedDownloadViewModel.effects.collect { effect ->
      effect.invokeWith(activity)
    }
  }

  BrandedDownloadScreen(
    state = state,
    onDownloadClick = {
      handleActionWithPermission(
        activity = activity,
        alertDialogShower = alertDialogShower,
        downloadViewModel = brandedDownloadViewModel,
        action = ClickedDownload
      )
    },
    onRetryClick = {
      handleActionWithPermission(
        activity = activity,
        alertDialogShower = alertDialogShower,
        downloadViewModel = brandedDownloadViewModel,
        action = ClickedRetry
      )
    }
  )
  DialogHost(alertDialogShower)
}

private fun handleActionWithPermission(
  activity: CoreMainActivity,
  alertDialogShower: AlertDialogShower,
  downloadViewModel: BrandedDownloadViewModel,
  action: Action
) {
  activity.lifecycleScope.launch {
    if (downloadViewModel.hasNotificationPermission(activity)) {
      downloadViewModel.actions.emit(action)
    } else {
      requestNotificationPermissionOrShowRationale(activity, alertDialogShower)
    }
  }
}

private fun requestNotificationPermissionOrShowRationale(
  activity: CoreMainActivity,
  alertDialogShower: AlertDialogShower
) {
  if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, POST_NOTIFICATIONS)) {
    activity.requestNotificationPermission()
  } else {
    alertDialogShower.show(
      KiwixDialog.NotificationPermissionDialog,
      activity::navigateToAppSettings
    )
  }
}
