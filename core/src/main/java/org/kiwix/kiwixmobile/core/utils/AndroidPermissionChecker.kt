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

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.NEARBY_WIFI_DEVICES
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Environment
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.first
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import javax.inject.Inject

class AndroidPermissionChecker @Inject constructor(
  private val context: Application,
  private val kiwixDataStore: KiwixDataStore
) : KiwixPermissionChecker {
  override suspend fun hasWriteExternalStoragePermission(): Boolean =
    if (isAndroid13orAbove() || kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove()) {
      true
    } else {
      ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED
    }

  override suspend fun hasReadExternalStoragePermission(): Boolean =
    if (isAndroid13orAbove() || kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove()) {
      true
    } else {
      ContextCompat.checkSelfPermission(context, READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED
    }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  override suspend fun hasNearbyWifiPermission(): Boolean =
    ContextCompat.checkSelfPermission(context, NEARBY_WIFI_DEVICES) == PERMISSION_GRANTED

  /**
   * Checks ACCESS_FINE_LOCATION permission.
   *
   * Note: This should only be called on devices below Android 13 (API 33).
   * For Android 13 and above, use hasNearbyWifiPermission().
   */
  override suspend fun hasFineLocationPermission(): Boolean {
    require(!isAndroid13orAbove()) {
      "hasFineLocationPermission should not be called on API 33+. Use hasNearbyWifiPermission() instead."
    }
    return ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
  }

  override fun shouldShowRationale(activity: Activity, permission: String): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

  override suspend fun isWriteExternalStoragePermissionRequired(): Boolean =
    !isAndroid13orAbove() &&
      !kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove()

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
  override fun isAndroid13orAbove(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
  override fun isAndroid8OrAbove(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

  override suspend fun isManageExternalStoragePermissionGranted(): Boolean =
    if (kiwixDataStore.isNotPlayStoreBuildWithAndroid11OrAbove() &&
      !kiwixDataStore.prefIsTest.first() &&
      kiwixDataStore.showManageExternalFilesPermissionDialogOnRefresh.first()
    ) {
      Environment.isExternalStorageManager()
    } else {
      true
    }
}
