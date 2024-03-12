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
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.io.InputStream
import java.io.ObjectOutputStream
import java.io.OutputStream

class SenderHandShake(private val wifiDirectManager: WifiDirectManager, groupInfo: WifiP2pInfo) :
  PeerGroupHandshake(groupInfo) {

  override fun exchangeFileTransferMetadata(inputStream: InputStream, outputStream: OutputStream) {
    try {
      ObjectOutputStream(outputStream).use { objectOutputStream ->
        // Send total number of files which will be transferred
        objectOutputStream.writeObject(wifiDirectManager.totalFilesForTransfer)
        // Send the names of each of those files, in order
        wifiDirectManager.getFilesForTransfer().iterator().forEach { fileItem ->
          objectOutputStream.writeObject(fileItem.fileName)
          Log.d(TAG, "Sending " + fileItem.fileUri.toString())
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  companion object {
    private const val TAG = "SenderHandShake"
  }
}
