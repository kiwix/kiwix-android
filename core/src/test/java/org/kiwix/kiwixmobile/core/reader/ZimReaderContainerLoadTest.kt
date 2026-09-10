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

package org.kiwix.kiwixmobile.core.reader

import android.os.Build
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

/**
 * Covers the range-request bug found in review (#5070): the delivered body previously
 * always started at offset 0, no matter what Content-Range the response declared.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ZimReaderContainerLoadTest {
  private val zimFileReaderFactory: ZimFileReader.Factory = mockk()
  private val container = ZimReaderContainer(zimFileReaderFactory, Dispatchers.Unconfined)

  private fun setUpReader(body: ByteArray) {
    val reader: ZimFileReader = mockk(relaxed = true)
    coEvery { reader.load(any()) } returns ByteArrayInputStream(body)
    every { reader.getItem(any()) } returns null
    container.zimFileReader = reader
  }

  @Test
  fun `load with a Range header skips the body to the requested offset`() {
    setUpReader("0123456789".toByteArray())

    val response = container.load(
      "https://kiwix.app/A/foo",
      mapOf("Range" to "bytes=5-")
    )

    assertEquals(206, response.statusCode)
    assertEquals("bytes 5--1/0", response.responseHeaders["Content-Range"])
    assertEquals("56789", response.data.readBytes().toString(Charsets.UTF_8))
  }

  @Test
  fun `load without a Range header returns the full body from offset zero`() {
    setUpReader("0123456789".toByteArray())

    val response = container.load("https://kiwix.app/A/foo", emptyMap())

    assertEquals(200, response.statusCode)
    assertEquals("0123456789", response.data.readBytes().toString(Charsets.UTF_8))
  }

  @Test
  fun `load with a malformed Range header falls back to offset zero instead of crashing`() {
    setUpReader("0123456789".toByteArray())

    val response = container.load(
      "https://kiwix.app/A/foo",
      mapOf("Range" to "bytes=not-a-number-")
    )

    assertEquals(206, response.statusCode)
    assertEquals("0123456789", response.data.readBytes().toString(Charsets.UTF_8))
  }

}
