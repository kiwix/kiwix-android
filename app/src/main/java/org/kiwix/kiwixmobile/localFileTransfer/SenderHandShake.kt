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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.BuildConfig
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class SenderHandShake() :
  PeerGroupHandshake {
  private val HANDSHAKE_MESSAGE = "Request Kiwix File Sharing"
  override suspend fun handshake(wifiDirectManager: WifiDirectManager): InetAddress? =
    withContext(Dispatchers.IO) {
      if (BuildConfig.DEBUG) {
        Log.d(TAG, "Handshake in progress")
      }
      if (wifiDirectManager.isGroupFormed && wifiDirectManager.isGroupOwner && this.isActive) {
        try {
          ServerSocket(PEER_HANDSHAKE_PORT)
            .use { serverSocket ->
              serverSocket.reuseAddress = true
              val server = serverSocket.accept()
              val objectInputStream = ObjectInputStream(server.getInputStream())
              val kiwixHandShakeMessage = objectInputStream.readObject()

              // Verify that the peer trying to communicate is a kiwix app intending to transfer files
              return@withContext if (isKiwixHandshake(kiwixHandShakeMessage) && this.isActive) {
                if (BuildConfig.DEBUG) {
                  Log.d(TAG, "Client IP address: " + server.inetAddress)
                }
                exchangeFileTransferMetadata(
                  wifiDirectManager,
                  server.getOutputStream(),
                  server.getInputStream()
                )
                server.inetAddress
              } else {
                // Selected device is not accepting wifi direct connections through the kiwix app
                null
              }
            }
        } catch (ex: Exception) {
          ex.printStackTrace()
          return@withContext null
        }
      } else if (wifiDirectManager.isGroupFormed && this.isActive) { // && !groupInfo.isGroupOwner
        try {
          Socket().use { client ->
            client.reuseAddress = true
            client.connect(
              InetSocketAddress(
                wifiDirectManager.groupOwnerAddress.hostAddress,
                PEER_HANDSHAKE_PORT
              ), 15000
            )
            val objectOutputStream = ObjectOutputStream(client.getOutputStream())
            // Send message for the peer device to verify
            objectOutputStream.writeObject(HANDSHAKE_MESSAGE)
            exchangeFileTransferMetadata(
              wifiDirectManager,
              client.getOutputStream(),
              client.getInputStream()
            )
            return@withContext wifiDirectManager.groupOwnerAddress
          }
        } catch (ex: Exception) {
          ex.printStackTrace()
          return@withContext null
        }
      }
      return@withContext null
    }

  private fun exchangeFileTransferMetadata(
    wifiDirectManager: WifiDirectManager,
    outputStream: OutputStream,
    inputStream: InputStream
  ) {
    if (wifiDirectManager.isFileSender) {
      try {
        ObjectOutputStream(outputStream).use { objectOutputStream ->
          // Send total number of files which will be transferred
          objectOutputStream.writeObject(wifiDirectManager.totalFilesForTransfer)
          // Send the names of each of those files, in order
          wifiDirectManager.getFilesForTransfer().forEach { fileItem ->
            objectOutputStream.writeObject(fileItem.fileName)
            Log.d(TAG, "Sending " + fileItem.fileUri.toString())
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    } else {
      try {
        ObjectInputStream(inputStream).use { objectInputStream ->
          // Read the number of files
          (objectInputStream.readObject() as? Int)?.let { total ->
            if (BuildConfig.DEBUG) Log.d(TAG, "Metadata: $total files")
            // Read names of each of those files, in order
            val fileItems = (0 until total).map {
              (objectInputStream.readObject() as? String)?.let { fileName ->
                if (BuildConfig.DEBUG) Log.d(TAG, "Expecting $fileName")
                FileItem(fileName = fileName)
              }
            }
            wifiDirectManager.setFilesForTransfer(fileItems.toList() as List<FileItem>)
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  private fun isKiwixHandshake(handshakeMessage: Any): Boolean =
    HANDSHAKE_MESSAGE == handshakeMessage

  companion object {
    private const val TAG = "SenderHandShake"
    private const val PEER_HANDSHAKE_PORT = 8009
  }
}
