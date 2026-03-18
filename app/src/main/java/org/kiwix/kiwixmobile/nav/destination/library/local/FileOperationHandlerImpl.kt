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

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.utils.HUNDERED
import org.kiwix.kiwixmobile.core.utils.ZERO
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import javax.inject.Inject

class FileOperationHandlerImpl @Inject constructor(
  private val context: Context
) : FileOperationHandler {
  @Suppress("MagicNumber", "InjectDispatcher")
  override suspend fun copy(
    sourceUri: Uri,
    destinationFile: File,
    onProgress: suspend (Int) -> Unit
  ) {
    withContext(Dispatchers.IO) {
      val contentResolver = context.contentResolver
      val parcelFileDescriptor =
        contentResolver.openFileDescriptor(sourceUri, "r")
          ?: throw FileNotFoundException("The selected file could not be opened")
      var totalBytesTransferred = 0L
      parcelFileDescriptor.use { pfd ->
        FileInputStream(pfd.fileDescriptor).channel.use { sourceChannel ->
          FileOutputStream(destinationFile).channel.use { destinationChannel ->
            val fileSize = sourceChannel.size()
            if (fileSize <= 0L) {
              // if the file does not have any content simply returns.
              onProgress(HUNDERED)
              return@withContext
            }

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
              val progress = (totalBytesTransferred * HUNDERED / fileSize).toInt()
              onProgress.invoke(progress.coerceAtMost(HUNDERED))
            }
          }
        }
      }
    }
  }

  override suspend fun move(
    selectedFile: DocumentFile,
    sourceUri: Uri,
    destinationFolderUri: Uri,
    destinationFile: File,
    onProgress: suspend (Int) -> Unit
  ): Boolean =
    selectedFile.parentFile?.uri?.let { parentUri ->
      tryMoveWithDocumentContract(sourceUri, parentUri, destinationFolderUri)
    } ?: run {
      copy(sourceUri, destinationFile, onProgress)
      true
    }

  @Suppress("UnsafeCallOnNullableType")
  override fun rollbackMove(
    destinationFile: File,
    originalParentUri: Uri
  ): Boolean = tryMoveWithDocumentContract(
    destinationFile.toUri(),
    destinationFile.parentFile.toUri(),
    originalParentUri
  )

  private fun tryMoveWithDocumentContract(
    sourceUri: Uri,
    sourceParentUri: Uri,
    destinationFolderUri: Uri
  ): Boolean = runCatching {
    val contentResolver = context.contentResolver
    if (documentCanMove(sourceUri, contentResolver)) {
      DocumentsContract.moveDocument(
        contentResolver,
        sourceUri,
        sourceParentUri,
        destinationFolderUri
      )
      true
    } else {
      false
    }
  }.onFailure { it.printStackTrace() }.getOrDefault(false)

  private fun documentCanMove(uri: Uri, contentResolver: ContentResolver): Boolean {
    if (!DocumentsContract.isDocumentUri(context, uri)) return false

    val flags =
      contentResolver.query(
        uri,
        arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
        null,
        null,
        null
      )
        ?.use { cursor ->
          if (cursor.moveToFirst()) cursor.getInt(ZERO) else ZERO
        } ?: ZERO

    return flags and DocumentsContract.Document.FLAG_SUPPORTS_MOVE != ZERO
  }

  @Suppress("InjectDispatcher")
  override suspend fun delete(uri: Uri, selectedFile: DocumentFile) = withContext(Dispatchers.IO) {
    runCatching {
      DocumentsContract.deleteDocument(context.contentResolver, uri)
    }.onFailure {
      selectedFile.delete()
      it.printStackTrace()
    }.getOrDefault(false)
  }
}
