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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.utils.HUNDERED
import org.kiwix.kiwixmobile.core.utils.ZERO
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class FileOperationHandlerImpl @Inject constructor(
  @param:ApplicationContext private val context: Context,
  @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FileOperationHandler {
  override suspend fun copy(
    sourceUri: Uri,
    destinationFile: File,
    onProgress: suspend (Int) -> Unit
  ) {
    withContext(ioDispatcher) {
      val source = context.contentResolver.openAssetFileDescriptor(sourceUri, "r")
        ?: throw FileNotFoundException("The selected file could not be opened")
      source.use { descriptor ->
        val fileSize = descriptor.length
        val bytesCopied = descriptor.createInputStream().use { input ->
          destinationFile.outputStream().use { output ->
            copyStream(input, output, fileSize, onProgress)
          }
        }
        if (bytesCopied == 0L || (fileSize >= 0L && bytesCopied != fileSize)) {
          throw IOException("The selected ZIM file is empty or could not be copied completely")
        }
      }
      onProgress(HUNDERED)
    }
  }

  private suspend fun copyStream(
    input: InputStream,
    output: OutputStream,
    fileSize: Long,
    onProgress: suspend (Int) -> Unit
  ): Long {
    val buffer = ByteArray(COPY_BUFFER_SIZE)
    var bytesCopied = 0L
    var lastProgress = ZERO
    while (true) {
      currentCoroutineContext().ensureActive()
      val bytesRead = input.read(buffer)
      if (bytesRead == -1) break
      output.write(buffer, 0, bytesRead)
      bytesCopied += bytesRead
      if (fileSize > 0 && bytesCopied < fileSize) {
        val progress = (bytesCopied * HUNDERED / fileSize).toInt()
        if (progress != lastProgress) {
          onProgress(progress)
          lastProgress = progress
        }
      }
    }
    return bytesCopied
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
    destinationFile.parentFile!!.toUri(),
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
      contentResolver
        .query(
          uri,
          arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
          null,
          null,
          null
        )?.use { cursor ->
          if (cursor.moveToFirst()) cursor.getInt(ZERO) else ZERO
        } ?: ZERO

    return flags and DocumentsContract.Document.FLAG_SUPPORTS_MOVE != ZERO
  }

  override suspend fun delete(uri: Uri, selectedFile: DocumentFile) = withContext(ioDispatcher) {
    runCatching {
      DocumentsContract.deleteDocument(context.contentResolver, uri)
    }.onFailure {
      selectedFile.delete()
      it.printStackTrace()
    }.getOrDefault(false)
  }

  private companion object {
    const val COPY_BUFFER_SIZE = 1024 * 1024
  }
}
