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

import android.app.Activity
import android.content.ContentResolver
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.localFileTransfer.WifiDirectManager.Companion.copyToOutputStream
import java.io.IOException
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
  private val wifiDirectManager: WifiDirectManager,
  activity: Activity
) {
  private val contentResolver: ContentResolver = activity.contentResolver
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
  suspend fun send(fileItems: Array<out FileItem?>) = withContext(ioDispatcher) {
    val job = async {
      if (!delayForSlowReceiverDevicesToSetupServer()) {
        return@async false
      }
      val hostAddress =
        wifiDirectManager.getFileReceiverDeviceAddress().hostAddress
      var isTransferErrorFree = true
      var fileIndex = 0
      while (fileIndex < fileItems.size && this.isActive) {
        val fileItem = fileItems[fileIndex]
        try {
          Socket().use { socket ->
            Log.d("gouri", "${Thread.currentThread().name} thread")
            contentResolver.openInputStream(fileItem?.fileUri!!).use { fileInputStream ->
              socket.bind(null)
              socket.connect(
                InetSocketAddress(hostAddress, WifiDirectManager.FILE_TRANSFER_PORT),
                TIME_OUT
              )
              Log.d(TAG, "Sender socket connected to server - " + socket.isConnected)
              publishProgress(fileIndex, FileItem.FileStatus.SENDING.ordinal)
              val socketOutputStream = socket.getOutputStream()
              copyToOutputStream(fileInputStream!!, socketOutputStream)
              if (BuildConfig.DEBUG) Log.d(TAG, "Sender: Data written")
              publishProgress(fileIndex, FileItem.FileStatus.SENT.ordinal)
            }
          }
        } catch (e: IOException) {
          Log.e(TAG, e.message)
          e.printStackTrace()
          isTransferErrorFree = false
          publishProgress(fileIndex, FileItem.FileStatus.ERROR.ordinal)
        }
        fileIndex++
      }
      return@async isTransferErrorFree
    }
    return@withContext job.await()
  }

  private suspend fun publishProgress(vararg values: Int?) {
    withContext(Dispatchers.Main) {
      val fileIndex = values[0]
      val fileStatus = values[1]
      if (fileIndex != null && fileStatus != null) {
        wifiDirectManager.changeStatus(fileIndex, FileItem.FileStatus.values()[fileStatus])
      }
    }
  }

  @SuppressWarnings("MagicNumber")
  private suspend fun delayForSlowReceiverDevicesToSetupServer(): Boolean {
    try { // Delay trying to connect with receiver, to allow slow receiver devices to setup server
      delay(3000)
    } catch (e: InterruptedException) {
      Log.e(TAG, e.message)
      return false
    }
    return true
  }

  companion object {
    private const val TAG = "SenderDeviceAsyncTask"
  }
}
