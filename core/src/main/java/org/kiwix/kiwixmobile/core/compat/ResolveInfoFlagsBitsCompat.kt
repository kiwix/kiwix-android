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

import androidx.annotation.LongDef

@LongDef(
  flag = true,
  // prefix = ["GET_", "MATCH_"],
  value = [
    GET_META_DATA.toLong(),
    GET_RESOLVED_FILTER.toLong(),
    GET_SHARED_LIBRARY_FILES.toLong(),
    MATCH_ALL.toLong(),
    MATCH_DISABLED_COMPONENTS.toLong(),
    MATCH_DISABLED_UNTIL_USED_COMPONENTS.toLong(),
    MATCH_DEFAULT_ONLY.toLong(),
    MATCH_DIRECT_BOOT_AUTO.toLong(),
    MATCH_DIRECT_BOOT_AWARE.toLong(),
    MATCH_DIRECT_BOOT_UNAWARE.toLong(),
    MATCH_SYSTEM_ONLY.toLong(),
    MATCH_UNINSTALLED_PACKAGES.toLong()
    // PackageManager.MATCH_INSTANT, // @SystemApi
    // PackageManager.MATCH_DEBUG_TRIAGED_MISSING, // deprecated
    // PackageManager.GET_DISABLED_COMPONENTS, // deprecated
    // PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS, // deprecated
    // PackageManager.GET_UNINSTALLED_PACKAGES.toLong() // deprecated
  ]
)
@Retention(AnnotationRetention.SOURCE)
annotation class ResolveInfoFlagsBitsCompat
