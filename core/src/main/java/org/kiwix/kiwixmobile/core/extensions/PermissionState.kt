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

package org.kiwix.kiwixmobile.core.extensions

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
fun PermissionState.handlePermissionRequest(
  onGranted: () -> Unit,
  onRationale: () -> Unit
) {
  when {
    status.isGranted -> onGranted()
    status.shouldShowRationale -> onRationale()
    else -> launchPermissionRequest()
  }
}

@OptIn(ExperimentalPermissionsApi::class)
fun List<PermissionState?>.allGranted(): Boolean = all { it?.status?.isGranted ?: true }

@OptIn(ExperimentalPermissionsApi::class)
val PermissionState?.isGranted: Boolean get() = listOf(this).allGranted()
