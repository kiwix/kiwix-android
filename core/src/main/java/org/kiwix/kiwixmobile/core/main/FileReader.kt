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
package org.kiwix.kiwixmobile.core.main

import android.content.Context
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
class FileReader {
  fun readFile(filePath: String?, context: Context): String {
    return try {
      val buf = StringBuilder()
      val json = context.assets.open(filePath)
      val bufferedReader =
        BufferedReader(InputStreamReader(json, "UTF-8"))
      var str: String?
      while (bufferedReader.readLine().also { str = it } != null) {
        buf.append(str)
      }
      bufferedReader.close()
      "$buf"
    } catch (e: IOException) {
      e.printStackTrace()
      ""
    }
  }
}
