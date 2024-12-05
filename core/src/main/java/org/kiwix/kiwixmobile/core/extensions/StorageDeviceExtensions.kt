/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

import android.content.Context
import eu.mhutti1.utils.storage.StorageDevice
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

fun StorageDevice.getFreeSpace(context: Context, storageCalculator: StorageCalculator): String {
  val freeSpace = storageCalculator.calculateAvailableSpace(file)
  return context.getString(R.string.pref_free_storage, freeSpace)
}

suspend fun StorageDevice.getUsedSpace(
  context: Context,
  storageCalculator: StorageCalculator
): String {
  val usedSpace = storageCalculator.calculateUsedSpace(file)
  return context.getString(R.string.pref_storage_used, usedSpace)
}

@Suppress("MagicNumber")
suspend fun StorageDevice.usedPercentage(storageCalculator: StorageCalculator): Int {
  val totalSpace = storageCalculator.totalBytes(file)
  val availableSpace = storageCalculator.availableBytes(file)
  val usedSpace = totalSpace - availableSpace
  return (usedSpace.toDouble() / totalSpace * 100).toInt()
}

suspend fun StorageDevice.storagePathAndTitle(
  context: Context,
  index: Int,
  sharedPreferenceUtil: SharedPreferenceUtil,
  storageCalculator: StorageCalculator
): String {
  val storageName = if (isInternal) {
    context.getString(R.string.internal_storage)
  } else {
    context.getString(R.string.external_storage)
  }
  val storagePath = if (index == sharedPreferenceUtil.storagePosition) {
    sharedPreferenceUtil.prefStorage
  } else {
    getStoragePathForNonSelectedStorage(this, sharedPreferenceUtil)
  }
  val totalSpace = storageCalculator.calculateTotalSpace(file)
  return "$storageName - $totalSpace\n$storagePath/Kiwix"
}

private fun getStoragePathForNonSelectedStorage(
  storageDevice: StorageDevice,
  sharedPreferenceUtil: SharedPreferenceUtil
): String =
  if (storageDevice.isInternal) {
    sharedPreferenceUtil.getPublicDirectoryPath(storageDevice.name)
  } else {
    storageDevice.name
  }
