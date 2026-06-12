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
package org.kiwix.kiwixmobile.core.utils

import android.content.Context
import android.os.Environment.MEDIA_MOUNTED
import android.os.Environment.getExternalStorageState

object StorageUtils {
  @JvmStatic fun getFileNameFromUrl(url: String?): String =
    NetworkUtils.getFileNameFromUrl(url).replace(".meta4", "")

  // Checks if external storage is available for read and write
  fun isExternalStorageWritable(): Boolean =
    MEDIA_MOUNTED == getExternalStorageState()

  fun getNotesDirectory(context: Context) =
    context.getExternalFilesDir("").toString() + "/Kiwix/Notes/"
}
