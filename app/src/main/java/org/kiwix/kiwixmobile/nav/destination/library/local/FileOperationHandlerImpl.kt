/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.nav.destination.library.local

import android.app.Activity
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import javax.inject.Inject

class FileOperationHandlerImpl @Inject constructor(
  private val activity: Activity
) : FileOperationHandler {
  @Suppress("MagicNumber", "InjectDispatcher")
  override suspend fun copy(
    sourceUri: Uri,
    destinationFile: File,
    onProgress: suspend (Int) -> Unit
  ) {
    withContext(Dispatchers.IO) {
      val contentResolver = activity.contentResolver

      val parcelFileDescriptor = contentResolver.openFileDescriptor(sourceUri, "r")
      val fileSize =
        parcelFileDescriptor?.fileDescriptor?.let { FileInputStream(it).channel.size() } ?: 0L
      var totalBytesTransferred = 0L

      parcelFileDescriptor?.use { pfd ->
        val sourceFd = pfd.fileDescriptor
        FileInputStream(sourceFd).channel.use { sourceChannel ->
          FileOutputStream(destinationFile).channel.use { destinationChannel ->
            var bytesTransferred: Long
            val bufferSize = 1024 * 1024
            while (totalBytesTransferred < fileSize) {
              // Transfer data from source to destination in chunks
              bytesTransferred = sourceChannel.transferTo(
                totalBytesTransferred,
                bufferSize.toLong(),
                destinationChannel
              )
              totalBytesTransferred += bytesTransferred
              val progress = (totalBytesTransferred * 100 / fileSize).toInt()
              onProgress.invoke(progress)
            }
          }
        }
      } ?: throw FileNotFoundException("The selected file could not be opened")
    }
  }

  override suspend fun move(
    sourceUri: Uri,
    sourceParentUri: Uri?,
    destinationFolderUri: Uri,
    destinationFile: File,
    onProgress: suspend (Int) -> Unit
  ): Boolean {
  }

  @Suppress("InjectDispatcher")
  override suspend fun delete(uri: Uri, selectedFile: DocumentFile?) = withContext(Dispatchers.IO) {
    try {
      DocumentsContract.deleteDocument(activity.applicationContext.contentResolver, uri)
      true
    } catch (ignore: Exception) {
      selectedFile?.delete()
      ignore.printStackTrace()
      false
    }
  }
}
