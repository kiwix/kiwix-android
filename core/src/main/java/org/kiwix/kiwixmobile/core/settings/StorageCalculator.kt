/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.settings

import eu.mhutti1.utils.storage.Bytes
import org.kiwix.kiwixmobile.core.extensions.freeSpace
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import java.io.File
import javax.inject.Inject

class StorageCalculator @Inject constructor(
  private val sharedPreferenceUtil: SharedPreferenceUtil
) {

  fun calculateAvailableSpace(file: File = File(sharedPreferenceUtil.prefStorage)): String =
    Bytes(availableBytes(file)).humanReadable

  fun calculateTotalSpace(file: File = File(sharedPreferenceUtil.prefStorage)): String =
    Bytes(totalBytes(file)).humanReadable

  fun availableBytes(file: File = File(sharedPreferenceUtil.prefStorage)) =
    if (file.isFileExist()) file.freeSpace()
    else 0L

  private fun totalBytes(file: File) = if (file.exists()) file.totalSpace else 0L
}
