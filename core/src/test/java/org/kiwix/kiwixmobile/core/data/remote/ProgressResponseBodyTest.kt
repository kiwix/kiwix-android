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

package org.kiwix.kiwixmobile.core.data.remote

import okhttp3.MediaType.Companion.toMediaType
import org.junit.jupiter.api.Test
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals

class ProgressResponseBodyTest {
  @Test
  fun `contentType returns correct type`() {
    val content = "Hello"
    val type = "text/plain".toMediaType()
    val body = content.toByteArray().toResponseBody(type)
    val progressBody = ProgressResponseBody(body, null, content.length.toLong())

    assertEquals(type, progressBody.contentType())
  }

  @Test
  fun `contentLength returns correct length`() {
    val content = "Hello"
    val type = "text/plain".toMediaType()
    val body = content.toByteArray().toResponseBody(type)
    val progressBody = ProgressResponseBody(body, null, content.length.toLong())

    assertEquals(content.length.toLong(), progressBody.contentLength())
  }

  @Test
  fun `source returns correct content`() {
    val content = "Hello"
    val type = "text/plain".toMediaType()
    val body = content.toByteArray().toResponseBody(type)
    val progressBody = ProgressResponseBody(body, null, content.length.toLong())

    val readContent = progressBody.source().readUtf8()

    assertEquals(content, readContent)
  }

  @Test
  fun `progressListener is called correctly`() {
    val content = "Hello"
    val type = "text/plain".toMediaType()
    val body = content.toByteArray().toResponseBody(type)

    var lastProgress = 0L
    val listener = object : OnlineLibraryProgressListener {
      override fun onProgress(bytesRead: Long, contentLength: Long) {
        lastProgress = bytesRead
      }
    }

    val progressBody = ProgressResponseBody(body, listener, content.length.toLong())
    progressBody.source().readUtf8() // read all bytes

    assertEquals(content.length.toLong(), lastProgress) // progress reached end
  }
}
