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

package org.kiwix.kiwixmobile.core.utils.effects

import android.Manifest.permission.POST_NOTIFICATIONS
import androidx.appcompat.app.AppCompatActivity
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.extensions.navigateToAppSettings
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import javax.inject.Inject

sealed interface NotificationPermissionAction {
  data object RequestNotificationPermission : NotificationPermissionAction
  data object None : NotificationPermissionAction
}

class RequestNotificationPermission @Inject constructor(
  private val alertDialogShower: AlertDialogShower,
  private val kiwixPermissionChecker: KiwixPermissionChecker,
) : SideEffect<NotificationPermissionAction> {
  override fun invokeWith(activity: AppCompatActivity): NotificationPermissionAction {
    val coreActivity = activity as? CoreMainActivity
      ?: return NotificationPermissionAction.None

    return if (!kiwixPermissionChecker.shouldShowRationale(
        coreActivity,
        POST_NOTIFICATIONS
      )
    ) {
      NotificationPermissionAction.RequestNotificationPermission
    } else {
      alertDialogShower.show(
        KiwixDialog.NotificationPermissionDialog,
        coreActivity::navigateToAppSettings
      )
      NotificationPermissionAction.None
    }
  }
}
