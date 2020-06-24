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

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException
import java.lang.System.currentTimeMillis
import javax.inject.Inject
import javax.inject.Singleton

/** Authored by s-ayush2903 on 19 June 2020
 *
 * A class for writing logs to the file in the device */
@Singleton
class FileLogger @Inject constructor() {

  /* Checks if external storage is available for read and write */
  val isExternalStorageWritable: Boolean =
    Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()

  fun writeLogFile(context: Context): File {
    val logFile = File(context.filesDir, fileName)
    Log.d("KIWIX", "Writing all logs into [" + logFile.path + "]")
    if (logFile.exists() && logFile.isFile) {
      Log.d(TAG, "writeLogFile: Deleting preExistingFile")
      logFile.delete()
    }
    // clear the previous logcat and then write the new one to the file
    try {
      logFile.createNewFile()
      Runtime.getRuntime().exec("logcat -c")
      Runtime.getRuntime().exec("logcat -f $logFile -s kiwix")
      Log.d(TAG, "writeLogFile: Log report written Successfully!")
    } catch (e: IOException) {
      Log.e("KIWIX", "Error while writing logcat.txt", e)
    }
    return logFile
  }

  companion object {
    private const val TAG: String = "FileLogger"
    private val fileName = "logs" + currentTimeMillis() + ".txt"
  }
}
