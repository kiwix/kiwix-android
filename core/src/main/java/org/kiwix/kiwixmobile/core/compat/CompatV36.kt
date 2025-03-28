/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.compat

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.ConnectivityManager
import android.os.Build.VERSION_CODES.BAKLAVA
import androidx.annotation.RequiresApi
import java.util.Locale

@RequiresApi(BAKLAVA)
open class CompatV36 : Compat {
  private val compatV33 = CompatV33()
  override fun queryIntentActivities(
    packageManager: PackageManager,
    intent: Intent,
    flags: ResolveInfoFlagsCompat
  ): List<ResolveInfo> = compatV33.queryIntentActivities(packageManager, intent, flags)

  override fun getPackageInformation(
    packageName: String,
    packageManager: PackageManager,
    flag: Int
  ): PackageInfo =
    compatV33.getPackageInformation(packageName, packageManager, flag)

  override fun isNetworkAvailable(connectivity: ConnectivityManager): Boolean =
    compatV33.isNetworkAvailable(connectivity)

  override fun isWifi(connectivity: ConnectivityManager): Boolean =
    compatV33.isWifi(connectivity)

  override fun convertToLocal(language: String): Locale = Locale.of(language)
}
