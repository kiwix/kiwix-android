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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import org.kiwix.kiwixmobile.core.extensions.CollectSideEffectWithActivity
import org.kiwix.kiwixmobile.core.utils.effects.NotificationPermissionAction

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BrandedDownloadRoute(brandedDownloadViewModel: BrandedDownloadViewModel) {
  val state by brandedDownloadViewModel.state.collectAsStateWithLifecycle()
  val permissionState = if (brandedDownloadViewModel.isAndroid13OrAbove) {
    rememberPermissionState(POST_NOTIFICATIONS) { granted ->
      brandedDownloadViewModel.onNotificationPermissionResult(granted)
    }
  } else {
    null
  }

  brandedDownloadViewModel.effects.CollectSideEffectWithActivity { effect, activity ->
    when (effect.invokeWith(activity)) {
      NotificationPermissionAction.RequestNotificationPermission ->
        permissionState?.launchPermissionRequest()

      else -> Unit
    }
  }

  BrandedDownloadScreen(
    state = state,
    onDownloadClick = { brandedDownloadViewModel.onDownloadButtonClick() },
    onRetryClick = { brandedDownloadViewModel.onRetryButtonClick() },
  )
}
