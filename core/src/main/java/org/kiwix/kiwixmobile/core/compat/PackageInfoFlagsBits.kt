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

import android.content.pm.PackageManager
import androidx.annotation.LongDef

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
