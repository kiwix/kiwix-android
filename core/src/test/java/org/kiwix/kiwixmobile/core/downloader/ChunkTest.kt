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

package org.kiwix.kiwixmobile.core.downloader

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ChunkTest {
  @Test
  fun `size returns 1 plus endByte minus startByte`() {
    val chunk = Chunk(
      rangeHeader = "0-99",
      fileName = "test.zim.part.part",
      url = "http://example.com/test.zim",
      contentLength = 100L,
      notificationID = 1,
      startByte = 0L,
      endByte = 99L
    )
    assertThat(chunk.size).isEqualTo(100L)
  }

  @Test
  fun `size is correct for non-zero startByte`() {
    val chunk = Chunk(
      rangeHeader = "50-149",
      fileName = "test.zim.part.part",
      url = "http://example.com/test.zim",
      contentLength = 200L,
      notificationID = 2,
      startByte = 50L,
      endByte = 149L
    )
    assertThat(chunk.size).isEqualTo(100L)
  }

  @Test
  fun `size is 1 when startByte equals endByte`() {
    val chunk = Chunk(
      rangeHeader = "0-0",
      fileName = "test.zim.part.part",
      url = "http://example.com/test.zim",
      contentLength = 1L,
      notificationID = 3,
      startByte = 0L,
      endByte = 0L
    )
    assertThat(chunk.size).isEqualTo(1L)
  }

  @Test
  fun `isDownloaded defaults to false`() {
    val chunk = Chunk(
      rangeHeader = "0-99",
      fileName = "test.zim.part.part",
      url = "http://example.com/test.zim",
      contentLength = 100L,
      notificationID = 1,
      startByte = 0L,
      endByte = 99L
    )
    assertThat(chunk.isDownloaded).isFalse()
  }

  @Test
  fun `isDownloaded can be set to true`() {
    val chunk = Chunk(
      rangeHeader = "0-99",
      fileName = "test.zim.part.part",
      url = "http://example.com/test.zim",
      contentLength = 100L,
      notificationID = 1,
      startByte = 0L,
      endByte = 99L
    )
    chunk.isDownloaded = true
    assertThat(chunk.isDownloaded).isTrue()
  }

  @Test
  fun `constructor stores all properties correctly`() {
    val chunk = Chunk(
      rangeHeader = "100-200",
      fileName = "wiki.zimaa.part.part",
      url = "http://mirror.example.com/wiki.zim",
      contentLength = 5000L,
      notificationID = 42,
      startByte = 100L,
      endByte = 200L
    )
    assertThat(chunk.rangeHeader).isEqualTo("100-200")
    assertThat(chunk.fileName).isEqualTo("wiki.zimaa.part.part")
    assertThat(chunk.url).isEqualTo("http://mirror.example.com/wiki.zim")
    assertThat(chunk.contentLength).isEqualTo(5000L)
    assertThat(chunk.notificationID).isEqualTo(42)
  }

  @Test
  fun `constructor accepts null fileName and url`() {
    val chunk = Chunk(
      rangeHeader = "0-",
      fileName = null,
      url = null,
      contentLength = 100L,
      notificationID = 1,
      startByte = 0L,
      endByte = 99L
    )
    assertThat(chunk.fileName).isNull()
    assertThat(chunk.url).isNull()
  }

  @Test
  fun `size is correct for large byte ranges`() {
    val startByte = 0L
    val endByte = ChunkUtils.CHUNK_SIZE
    val chunk = Chunk(
      rangeHeader = "$startByte-$endByte",
      fileName = "large.zim.part.part",
      url = "http://example.com/large.zim",
      contentLength = endByte + 1,
      notificationID = 10,
      startByte = startByte,
      endByte = endByte
    )
    assertThat(chunk.size).isEqualTo(ChunkUtils.CHUNK_SIZE + 1)
  }
}
