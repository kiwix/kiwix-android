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

@file:Suppress("PackageNaming")

package org.kiwix.kiwixmobile.local_file_transfer

import android.net.Uri
import androidx.annotation.IntDef
import org.kiwix.kiwixmobile.local_file_transfer.WifiDirectManager.Companion.getFileName

/**
 * Helper class, part of the local file sharing module.
 *
 * Defines a file-item to represent the files being transferred.
 */
class FileItem private constructor(val fileUri: Uri?, val fileName: String) {

  var fileStatus = FileStatus.TO_BE_SENT

  @Retention(AnnotationRetention.SOURCE)
  @IntDef(FileStatus.TO_BE_SENT, FileStatus.SENDING, FileStatus.SENT, FileStatus.ERROR)
  annotation class FileStatus {
    companion object {
      const val TO_BE_SENT = -1 // File yet to be sent
      const val SENDING = 0 // Being sent
      const val SENT = +1 // Successfully sent
      const val ERROR = +2 // Error encountered while transferring the file
    }
  }

  constructor(fileUri: Uri) : this(fileUri, getFileName(fileUri)) { // For sender devices
  }

  constructor(fileName: String) : this(null, fileName) { // For receiver devices
  }
}
