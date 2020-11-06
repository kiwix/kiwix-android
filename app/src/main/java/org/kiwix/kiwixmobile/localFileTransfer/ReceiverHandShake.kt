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

package org.kiwix.kiwixmobile.localFileTransfer

import android.util.Log
import org.kiwix.kiwixmobile.core.BuildConfig
import java.io.InputStream
import java.io.ObjectInputStream
import java.util.ArrayList

class ReceiverHandShake {
  fun handShake(wifiDirectManager: WifiDirectManager, inputStream: InputStream) {
    try {
      ObjectInputStream(inputStream).use { objectInputStream ->
        // Read the number of files
        val totalFilesObject = objectInputStream.readObject().toString()
        if (totalFilesObject.javaClass == String::class.java) {
          val total: Int = totalFilesObject.toInt()
          if (BuildConfig.DEBUG) Log.d(TAG, "Metadata: $total files")
          // Read names of each of those files, in order
          val fileItems = sequence {
            yieldAll(generateSequence(1) { it + 1 }.map {
              (objectInputStream.readObject() as? String)?.let { fileName ->
                if (BuildConfig.DEBUG) Log.d(TAG, "Expecting $fileName")
                FileItem(fileName = fileName)
              }
            })
          }.take(total)
          val arrayListOfFileItems = ArrayList(fileItems.toList().filterNotNull())
          wifiDirectManager.setFilesForTransfer(arrayListOfFileItems)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  companion object {
    private const val TAG = "ReceiverHandshake"
  }
}
