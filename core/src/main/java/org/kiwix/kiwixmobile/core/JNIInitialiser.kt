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
package org.kiwix.kiwixmobile.core

import android.content.Context
import android.util.Log
import org.kiwix.kiwixlib.JNIKiwix
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

internal class JNIInitialiser @Inject constructor(context: Context, jniKiwix: JNIKiwix) {
  init {
    loadICUData(context)?.let(jniKiwix::setDataDirectory)
  }

  private fun loadICUData(context: Context): String? {
    return try {
      val icuDir = File(context.filesDir, "icu")
      if (!icuDir.exists()) {
        icuDir.mkdirs()
      }
      val icuFileNames = context.assets.list("icu") ?: emptyArray()
      for (icuFileName in icuFileNames) {
        val icuDataFile = File(icuDir, icuFileName)
        if (!icuDataFile.exists()) {
          FileOutputStream(icuDataFile).use { outputStream ->
            context.assets.open("icu/$icuFileName").use { inputStream ->
              inputStream.copyTo(outputStream, 1024)
            }
          }
        }
      }
      icuDir.absolutePath
    } catch (e: Exception) {
      Log.w(TAG_KIWIX, "Error copying icu data file", e)
      // TODO Consider surfacing to user
      null
    }
  }
}
