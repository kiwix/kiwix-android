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

import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.localFileTransfer.WifiDirectManager.Companion.copyToOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.ServerSocket

/**
 * Helper class for the local file sharing module.
 *
 * Once the handshake has successfully taken place, this async-task is used to receive files from
 * the sender device on the FILE_TRANSFER_PORT port. No. of files to be received (and their names)
 * are learnt beforehand during the handshake.
 *
 * A single Task is used for the entire file transfer (the server socket accepts connections as
 * many times as the no. of files).
 */
internal class ReceiverDevice(private val wifiDirectManager: WifiDirectManager) {
  private var incomingFileName: String? = null
  suspend fun receive() = withContext(Dispatchers.IO) {
    try {
      ServerSocket(WifiDirectManager.FILE_TRANSFER_PORT).use { serverSocket ->
        Log.d(TAG, "Server: Socket opened at " + WifiDirectManager.FILE_TRANSFER_PORT)
        val zimStorageRootPath = wifiDirectManager.zimStorageRootPath
        val fileItems = wifiDirectManager.getFilesForTransfer()
        var isTransferErrorFree = true
        if (BuildConfig.DEBUG) Log.d(TAG, "Expecting " + fileItems.size + " files")
        var fileItemIndex = 0
        while (fileItemIndex < fileItems.size && this.isActive) {
          incomingFileName = fileItems[fileItemIndex].fileName
          try {
            serverSocket.accept().use { client ->
              if (BuildConfig.DEBUG) {
                Log.d(TAG, "Sender device connected for " + fileItems[fileItemIndex].fileName)
              }
              publishProgress(fileItemIndex, FileItem.FileStatus.SENDING)
              val clientNoteFileLocation = File(zimStorageRootPath + incomingFileName)
              val dirs = File(clientNoteFileLocation.parent)
              if (!dirs.exists() && !dirs.mkdirs()) {
                Log.d(TAG, "ERROR: Required parent directories couldn't be created")
                isTransferErrorFree = false
                fileItemIndex++
              }
              val fileCreated = clientNoteFileLocation.createNewFile()
              if (BuildConfig.DEBUG) Log.d(TAG, "File creation: $fileCreated")
              copyToOutputStream(
                client.getInputStream(),
                FileOutputStream(clientNoteFileLocation)
              )
              publishProgress(fileItemIndex, FileItem.FileStatus.SENT)
            }
          } catch (e: IOException) {
            Log.e(TAG, e.message)
            isTransferErrorFree = false
            publishProgress(fileItemIndex, FileItem.FileStatus.ERROR)
          }
          fileItemIndex++
        }
        return@withContext isTransferErrorFree
      }
    } catch (e: IOException) {
      Log.e(TAG, e.message)
      return@withContext false // Returned when an error was encountered during transfer
    }
  }

  private suspend fun publishProgress(fileIndex: Int, fileStatus: FileItem.FileStatus) {
    withContext(Dispatchers.Main) {
      wifiDirectManager.changeStatus(fileIndex, fileStatus)
      if (fileStatus == FileItem.FileStatus.ERROR) {
        val activity = wifiDirectManager.activity
        activity.toast(
          activity.getString(R.string.error_transferring, incomingFileName!!),
          Toast.LENGTH_SHORT
        )
      }
    }
  }

  companion object {
    private const val TAG = "ReceiverDeviceAsyncTask"
  }
}
