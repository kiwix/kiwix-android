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
package org.kiwix.kiwixmobile.localFileTransfer

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.localFileTransfer.WifiDirectManager.Companion.copyToOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Helper class for the local file sharing module.
 *
 * Once the handshake between the two connected devices has taked place, this async-task is used
 * on the sender device to transfer the file to the receiver device at the FILE_TRANSFER_PORT port.
 *
 * It takes in the uri of all the files to be shared. For each file uri, creates a new connection &
 * copies all the bytes from input stream of the file to the output stream of the receiver device.
 * Also changes the status of the corresponding FileItem on the list of files for transfer.
 *
 * A single task is used by the sender for the entire transfer
 */
private const val TIME_OUT = 15000

internal class SenderDevice(
  private val context: Context,
  private val wifiDirectManager: WifiDirectManager,
  private val fileReceiverDeviceAddress: InetAddress
) {
  suspend fun send(fileItems: List<FileItem?>) = withContext(Dispatchers.IO) {
    // Delay trying to connect with receiver, to allow slow receiver devices to setup server
    delay(FOR_SLOW_RECEIVER)
    val hostAddress = fileReceiverDeviceAddress.hostAddress
    var isTransferErrorFree = true
    fileItems.asSequence()
      .takeWhile { isActive } // checks if coroutine is live
      .forEachIndexed { fileIndex, fileItem ->
        try {
          Socket().use { socket ->
            context.contentResolver.openInputStream(fileItem?.fileUri!!).use { fileInputStream ->
              socket.bind(null)
              socket.connect(
                InetSocketAddress(hostAddress, WifiDirectManager.FILE_TRANSFER_PORT),
                TIME_OUT
              )
              Log.d(TAG, "Sender socket connected to server - " + socket.isConnected)
              publishProgress(fileIndex, FileItem.FileStatus.SENDING)
              val socketOutputStream = socket.getOutputStream()
              copyToOutputStream(fileInputStream!!, socketOutputStream)
              if (BuildConfig.DEBUG) Log.d(TAG, "Sender: Data written")
              publishProgress(fileIndex, FileItem.FileStatus.SENT)
            }
          }
        } catch (e: IOException) {
          e.message?.let { message ->
            Log.e(TAG, message)
          }
          e.printStackTrace()
          isTransferErrorFree = false
          publishProgress(fileIndex, FileItem.FileStatus.ERROR)
        }
      }

    isTransferErrorFree
  }

  private suspend fun publishProgress(fileIndex: Int, fileStatus: FileItem.FileStatus) {
    withContext(Dispatchers.Main) {
      wifiDirectManager.changeStatus(fileIndex, fileStatus)
    }
  }

  companion object {
    private const val TAG = "SenderDevice"
    private const val FOR_SLOW_RECEIVER: Long = 3000
  }
}
