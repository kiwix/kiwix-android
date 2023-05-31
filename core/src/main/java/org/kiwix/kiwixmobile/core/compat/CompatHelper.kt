/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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
import android.os.Build

class CompatHelper private constructor() {
  // Note: Needs ": Compat" or the type system assumes `Compat21`
  private val compatValue: Compat = when {
    sdkVersion >= Build.VERSION_CODES.TIRAMISU -> CompatV33()
    else -> CompatV21()
  }

  companion object {
    /** Singleton instance of [CompatHelper] */
    private val instance by lazy(::CompatHelper)

    /** Get the current Android API level.  */
    val sdkVersion: Int
      get() = Build.VERSION.SDK_INT

    val compat get() = instance.compatValue

    /**
     * Retrieve all activities that can be performed for the given intent.
     *
     * @param intent The desired intent as per resolveActivity().
     * @param flags Additional option flags to modify the data returned. The
     *            most important is [MATCH_DEFAULT_ONLY], to limit the
     *            resolution to only those activities that support the
     *            [CATEGORY_DEFAULT]. Or, set
     *            [MATCH_ALL] to prevent any filtering of the results.
     * @return Returns a List of ResolveInfo objects containing one entry for
     *         each matching activity, ordered from best to worst. In other
     *         words, the first item is what would be returned by
     *         {@link #resolveActivity}. If there are no matching activities, an
     *         empty list is returned.
     */
    fun PackageManager.queryIntentActivitiesCompat(
      intent: Intent,
      flags: ResolveInfoFlagsCompat
    ): List<ResolveInfo> = compat.queryIntentActivities(this, intent, flags)

    fun PackageManager.getPackageInformation(
      packageName: String,
      flag: Int
    ): PackageInfo = compat.getPackageInformation(packageName, this, flag)

    fun PackageInfo.getVersionCode() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      longVersionCode.toInt()
    } else {
      versionCode
    }
  }
}
