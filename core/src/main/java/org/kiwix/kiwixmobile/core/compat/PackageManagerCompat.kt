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

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.annotation.LongDef

/*
 * Provides [PackageInfoFlagsCompat] to Android versions before SDK 33,
 * keeping a near consistent API for [Compat.getPackageInfo],
 * constraining the flags to the correct values (or types when available)
 *
 * Allows for either long flags to be provided (new API), or int flags (old API).
 * The old API is currently better as it means .toLong() isn't needed on constants provided
 * to the API.
 * For future: Can Kotlin can accept either the int or long in a `@LongDef`
 */

/**
 * Flags class that wraps around the bitmask flags used in methods that retrieve package or
 * application info.
 */
open class Flags protected constructor(val value: Long)

/**
 * Flag parameter to retrieve some information about all applications (even
 * uninstalled ones) which have data directories. This state could have
 * resulted if applications have been deleted with flag
 * `DELETE_KEEP_DATA` with a possibility of being replaced or
 * reinstalled in future.
 *
 *
 * Note: this flag may cause less information about currently installed
 * applications to be returned.
 *
 *
 * Note: use of this flag requires the android.permission.QUERY_ALL_PACKAGES
 * permission to see uninstalled packages.
 */
const val MATCH_UNINSTALLED_PACKAGES = 0x00002000 // API 24

/**
 * [PackageInfo] flag: return the signing certificates associated with
 * this package.  Each entry is a signing certificate that the package
 * has proven it is authorized to use, usually a past signing certificate from
 * which it has rotated.
 */
const val GET_SIGNING_CERTIFICATES = 0x08000000 // API 28

/**
 * [PackageInfo] flag: include disabled components in the returned info.
 */
const val MATCH_DISABLED_COMPONENTS = 0x00000200 // API 24

/**
 * [PackageInfo] flag: include disabled components which are in
 * that state only because of [.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED]
 * in the returned info.  Note that if you set this flag, applications
 * that are in this disabled state will be reported as enabled.
 */
const val MATCH_DISABLED_UNTIL_USED_COMPONENTS = 0x00008000 // API 24

/**
 * Querying flag: include only components from applications that are marked
 * with [ApplicationInfo.FLAG_SYSTEM].
 */
const val MATCH_SYSTEM_ONLY = 0x00100000 // API 24

/**
 * [PackageInfo] flag: include APEX packages that are currently
 * installed. In APEX terminology, this corresponds to packages that are
 * currently active, i.e. mounted and available to other processes of the OS.
 * In particular, this flag alone will not match APEX files that are staged
 * for activation at next reboot.
 */
const val MATCH_APEX = 0x40000000 // API 29

/**
 * [PackageInfo] flag: return all attributions declared in the package manifest
 */
const val GET_ATTRIBUTIONS = -0x80000000 // API 31

@LongDef(
  flag = true,
  // prefix = ["GET_", "MATCH_"],
  value = [
    PackageManager.GET_ACTIVITIES.toLong(),
    PackageManager.GET_CONFIGURATIONS.toLong(),
    PackageManager.GET_GIDS.toLong(),
    PackageManager.GET_INSTRUMENTATION.toLong(),
    PackageManager.GET_META_DATA.toLong(),
    PackageManager.GET_PERMISSIONS.toLong(),
    PackageManager.GET_PROVIDERS.toLong(),
    PackageManager.GET_RECEIVERS.toLong(),
    PackageManager.GET_SERVICES.toLong(),
    PackageManager.GET_SHARED_LIBRARY_FILES.toLong(),
    GET_SIGNING_CERTIFICATES.toLong(),
    PackageManager.GET_URI_PERMISSION_PATTERNS.toLong(),
    MATCH_UNINSTALLED_PACKAGES.toLong(),
    MATCH_DISABLED_COMPONENTS.toLong(),
    MATCH_DISABLED_UNTIL_USED_COMPONENTS.toLong(),
    MATCH_SYSTEM_ONLY.toLong(),
    MATCH_APEX.toLong(),
    GET_ATTRIBUTIONS.toLong()

    // not handled: Deprecated & unused in our code
    // PackageManager.GET_INTENT_FILTERS.toLong(),
    // PackageManager.GET_SIGNATURES.toLong(),
    // PackageManager.GET_DISABLED_COMPONENTS.toLong(),
    // PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS.toLong(),
    // PackageManager.GET_UNINSTALLED_PACKAGES.toLong(),

    // not handled: values with @SystemApi
    // PackageManager.MATCH_FACTORY_ONLY,
    // PackageManager.MATCH_DEBUG_TRIAGED_MISSING,
    // PackageManager.MATCH_INSTANT,
    // PackageManager.MATCH_HIDDEN_UNTIL_INSTALLED_COMPONENTS,
  ]
)
@Retention(AnnotationRetention.SOURCE)
annotation class PackageInfoFlagsBits
