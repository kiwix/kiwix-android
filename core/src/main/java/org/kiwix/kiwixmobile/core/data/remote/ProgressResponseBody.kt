/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.data.remote

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import org.kiwix.kiwixmobile.core.utils.files.Log

class ProgressResponseBody(
  private val responseBody: ResponseBody,
  private val progressListener: OnlineLibraryProgressListener
) : ResponseBody() {

  private lateinit var bufferedSource: BufferedSource

  override fun contentType(): MediaType? = responseBody.contentType()

  override fun contentLength(): Long = responseBody.contentLength()

  override fun source(): BufferedSource {
    if (!::bufferedSource.isInitialized) {
      bufferedSource = source(responseBody.source()).buffer()
    }
    return bufferedSource
  }

  private fun source(source: Source): Source {
    return object : ForwardingSource(source) {
      var totalBytesRead = 0L
      override fun read(sink: Buffer, byteCount: Long): Long {
        val bytesRead = super.read(sink, byteCount)
        totalBytesRead += if (bytesRead != -1L) bytesRead else 0
        val isDone = bytesRead == -1L
        progressListener.onProgress(
          totalBytesRead,
          responseBody.contentLength(),
          isDone
        )
          .also {
            Log.e(
              "PROGRESS",
              "onProgress: ${contentLength()} and byteRead = $totalBytesRead\n" +
                " sink ${bytesRead == -1L} \n byteRead = $bytesRead " +
                "\n bufferedSource = ${bufferedSource.isOpen}"
            )
          }
        return bytesRead
      }
    }
  }
}
