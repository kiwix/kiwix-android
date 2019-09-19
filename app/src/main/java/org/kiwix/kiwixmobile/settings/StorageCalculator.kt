package org.kiwix.kiwixmobile.settings

import android.annotation.SuppressLint
import android.os.Build.VERSION_CODES
import android.os.storage.StorageManager
import eu.mhutti1.utils.storage.Bytes
import org.kiwix.kiwixmobile.KiwixBuildConfig
import java.io.File
import javax.inject.Inject

/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
class StorageCalculator @Inject constructor(private val storageManager: StorageManager) {

  fun calculateAvailableSpace(file: File): String =
    Bytes(availableBytes(file)).humanReadable

  fun calculateTotalSpace(file: File): String =
    Bytes(totalBytes(file)).humanReadable

  @SuppressLint("NewApi")
  fun availableBytes(file: File) =
    if (KiwixBuildConfig.SDK_INT >= VERSION_CODES.O)
      storageManager.getAllocatableBytes(storageManager.getUuidForPath(file))
    else
      file.freeSpace

  private fun totalBytes(file: File) = file.totalSpace
}
