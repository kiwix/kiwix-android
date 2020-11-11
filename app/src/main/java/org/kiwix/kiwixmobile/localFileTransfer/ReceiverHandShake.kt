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

import android.net.wifi.p2p.WifiP2pInfo
import android.util.Log
import org.kiwix.kiwixmobile.core.BuildConfig
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.OutputStream

class ReceiverHandShake(private val wifiDirectManager: WifiDirectManager, groupInfo: WifiP2pInfo) :
  PeerGroupHandshake(groupInfo) {

  companion object {
    private const val TAG = "ReceiverHandshake"
  }

  override fun exchangeFileTransferMetadata(inputStream: InputStream, outputStream: OutputStream) {
    try {
      ObjectInputStream(inputStream).use { objectInputStream ->
        // Read the number of files
        (objectInputStream.readObject() as? Int)?.let { total ->
          if (BuildConfig.DEBUG) Log.d(TAG, "Metadata: $total files")
          // Read names of each of those files, in order
          val fileItems = (0 until total).mapNotNull {
            (objectInputStream.readObject() as? String)?.let { fileName ->
              if (BuildConfig.DEBUG) Log.d(TAG, "Expecting $fileName")
              FileItem(fileName = fileName)
            }
          }
          wifiDirectManager.setFilesForTransfer(fileItems)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
