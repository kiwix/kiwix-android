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

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.NEARBY_WIFI_DEVICES
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.rememberPermissionState
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import javax.inject.Inject

class AndroidPermissionChecker @Inject constructor(
  val context: Application,
  val kiwixDataStore: KiwixDataStore
) : KiwixPermissionChecker {
  override suspend fun hasWriteExternalStoragePermission(): Boolean =
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU ||
      kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove()
    ) {
      true
    } else {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      ) == PackageManager.PERMISSION_GRANTED
    }

  override suspend fun hasReadExternalStoragePermission(): Boolean =
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU ||
      kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove()
    ) {
      true
    } else {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_EXTERNAL_STORAGE
      ) == PackageManager.PERMISSION_GRANTED
    }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  override suspend fun hasNearbyWifiPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.NEARBY_WIFI_DEVICES
    ) == PackageManager.PERMISSION_GRANTED
  }

  override suspend fun hasLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
  }

  override fun shouldShowRationale(activity: Activity, permission: String): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

  override suspend fun isWriteExternalStoragePermissionRequired(): Boolean =
    Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU &&
      !kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove()

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
  override fun isAndroid13orAbove(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
