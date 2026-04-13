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

package org.kiwix.kiwixmobile.core.utils

import android.app.Activity
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi

interface KiwixPermissionChecker {
  suspend fun hasWriteExternalStoragePermission(): Boolean
  suspend fun hasReadExternalStoragePermission(): Boolean

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  suspend fun hasNearbyWifiPermission(): Boolean
  suspend fun hasFineLocationPermission(): Boolean
  suspend fun isWriteExternalStoragePermissionRequired(): Boolean

  fun shouldShowRationale(activity: Activity, permission: String): Boolean

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
  fun isAndroid13orAbove(): Boolean

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
  fun isAndroid8OrAbove(): Boolean
}
