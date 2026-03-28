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
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ProgressResponseBodyTest {
  @Nested
  inner class DelegationTests {
    @Test
    fun `contentType returns correct type`() {
      val content = "Hello"
      val type = "text/plain".toMediaType()
      val body = content.toByteArray().toResponseBody(type)

      val progressBody = ProgressResponseBody(body, null, content.length.toLong())

      assertEquals(type, progressBody.contentType())
    }

    @Test
    fun `contentType returns null when responseBody has no type`() {
      val body = "Hello".toByteArray().toResponseBody(null)

      val progressBody = ProgressResponseBody(body, null, 5L)

      assertNull(progressBody.contentType())
    }

    @Test
    fun `contentLength returns responseBody length`() {
      val content = "Hello"
      val body = content.toByteArray().toResponseBody("text/plain".toMediaType())

      val progressBody = ProgressResponseBody(body, null, content.length.toLong())

      assertEquals(content.length.toLong(), progressBody.contentLength())
    }

    @Test
    fun `contentLength ignores constructor value`() {
      val content = "Hello"
      val fakeLength = 100L
      val body = content.toByteArray().toResponseBody("text/plain".toMediaType())

      val progressBody = ProgressResponseBody(body, null, fakeLength)

      assertEquals(content.length.toLong(), progressBody.contentLength())
    }
  }

  @Nested
  inner class SourceTests {
    @Test
    fun `source returns same instance on multiple calls`() {
      val content = "Hello"
      val body = content.toByteArray().toResponseBody("text/plain".toMediaType())

      val progressBody = ProgressResponseBody(body, null, content.length.toLong())

      val source1 = progressBody.source()
      val source2 = progressBody.source()

      assertSame(source1, source2)
    }

    @Test
    fun `source returns correct content`() {
      val content = "Hello"
      val body = content.toByteArray().toResponseBody("text/plain".toMediaType())

      val progressBody = ProgressResponseBody(body, null, content.length.toLong())

      val result = progressBody.source().readUtf8()

      assertEquals(content, result)
    }

    @Test
    fun `no crash when progressListener is null`() {
      val content = "Hello"
      val body = content.toByteArray().toResponseBody("text/plain".toMediaType())

      val progressBody = ProgressResponseBody(body, null, content.length.toLong())

      val result = progressBody.source().readUtf8()

      assertEquals(content, result)
    }

    @Test
    fun `handles empty response body`() {
      val body = "".toByteArray().toResponseBody("text/plain".toMediaType())

      var lastProgress = -1L
      val listener = object : OnlineLibraryProgressListener {
        override fun onProgress(bytesRead: Long, contentLength: Long) {
          lastProgress = bytesRead
        }
      }

      val progressBody = ProgressResponseBody(body, listener, 0L)
      val result = progressBody.source().readUtf8()

      assertEquals("", result)
      assertEquals(0L, lastProgress)
    }
  }

  @Nested
  inner class ProgressTests {
    @Test
    fun `progressListener receives final progress`() {
      val content = "Hello"
      val body = content.toByteArray().toResponseBody("text/plain".toMediaType())

      var lastProgress = 0L
      val listener = object : OnlineLibraryProgressListener {
        override fun onProgress(bytesRead: Long, contentLength: Long) {
          lastProgress = bytesRead
        }
      }

      val progressBody = ProgressResponseBody(body, listener, content.length.toLong())
      progressBody.source().readUtf8()

      assertEquals(content.length.toLong(), lastProgress)
    }

    @Test
    fun `progress updates correctly for partial reads`() {
      val content = "HelloWorld"
      val body = content.toByteArray().toResponseBody("text/plain".toMediaType())

      val updates = mutableListOf<Long>()
      val listener = object : OnlineLibraryProgressListener {
        override fun onProgress(bytesRead: Long, contentLength: Long) {
          updates.add(bytesRead)
        }
      }

      val progressBody = ProgressResponseBody(body, listener, content.length.toLong())
      val source = progressBody.source()

      val buffer = Buffer()
      source.read(buffer, 5)
      source.read(buffer, 5)

      assertTrue(updates.contains(10L))
    }

    @Test
    fun `progress uses provided contentLength`() {
      val content = "Hello"
      val fakeLength = 100L
      val body = content.toByteArray().toResponseBody("text/plain".toMediaType())

      var reportedLength = 0L
      val listener = object : OnlineLibraryProgressListener {
        override fun onProgress(bytesRead: Long, contentLength: Long) {
          reportedLength = contentLength
        }
      }

      val progressBody = ProgressResponseBody(body, listener, fakeLength)
      progressBody.source().readUtf8()

      assertEquals(fakeLength, reportedLength)
    }

    @Test
    fun `totalBytesRead never decreases`() {
      val content = "Hello"
      val body = content.toByteArray().toResponseBody("text/plain".toMediaType())

      var previous = 0L
      val listener = object : OnlineLibraryProgressListener {
        override fun onProgress(bytesRead: Long, contentLength: Long) {
          assertTrue(bytesRead >= previous)
          previous = bytesRead
        }
      }

      val progressBody = ProgressResponseBody(body, listener, content.length.toLong())
      progressBody.source().readUtf8()
    }

    @Test
    fun `read handles end of stream correctly`() {
      val content = "Hi"
      val body = content.toByteArray().toResponseBody("text/plain".toMediaType())

      var lastProgress = 0L
      val listener = object : OnlineLibraryProgressListener {
        override fun onProgress(bytesRead: Long, contentLength: Long) {
          lastProgress = bytesRead
        }
      }

      val progressBody = ProgressResponseBody(body, listener, content.length.toLong())
      val source = progressBody.source()

      val buffer = Buffer()
      while (source.read(buffer, 1) != -1L) {
        // to nothing
      }

      repeat(3) {
        assertEquals(-1L, source.read(buffer, 1))
      }

      assertEquals(content.length.toLong(), lastProgress)
    }
  }
}
