/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.utils.files

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException

class FileLogger {

  fun writeLogFile() {
    if (isExternalStorageWritable) {
      val appDirectory =
        File(Environment.getExternalStorageDirectory().toString() + "/Kiwix")
      val logFile = File(appDirectory, "logcat.txt")
      Log.d("KIWIX", "Writing all logs into [" + logFile.path + "]")

      // create a new app folder
      if (!appDirectory.exists()) {
        appDirectory.mkdir()
      }

      if (logFile.exists() && logFile.isFile) {
        logFile.delete()
      }
      // clear the previous logcat and then write the new one to the file
      try {
        logFile.createNewFile()
        Runtime.getRuntime().exec("logcat -c")
        Runtime.getRuntime().exec("logcat -f " + logFile.path + " -s kiwix")
      } catch (e: IOException) {
        Log.e("KIWIX", "Error while writing logcat.txt", e)
      }
    }
  }

  /* Checks if external storage is available for read and write */
  val isExternalStorageWritable: Boolean
    /** private modifier is temp */
    get() = Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
}
