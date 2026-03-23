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
  private fun createChunk(
    start: Long = 0L,
    end: Long = 99L,
    range: String = "$start-$end",
    fileName: String? = "test.zim.part.part",
    url: String? = "http://example.com/test.zim",
    contentLength: Long = end + 1,
    notificationID: Int = 1
  ) = Chunk(
    rangeHeader = range,
    fileName = fileName,
    url = url,
    contentLength = contentLength,
    notificationID = notificationID,
    startByte = start,
    endByte = end
  )

  @Test
  fun `constructor stores all properties correctly`() {
    val chunk = createChunk(
      start = 100,
      end = 200,
      range = "100-200",
      fileName = "wiki.zimaa.part.part",
      url = "http://mirror.example.com/wiki.zim",
      contentLength = 5000,
      notificationID = 42
    )

    assertThat(chunk.rangeHeader).isEqualTo("100-200")
    assertThat(chunk.fileName).isEqualTo("wiki.zimaa.part.part")
    assertThat(chunk.url).isEqualTo("http://mirror.example.com/wiki.zim")
    assertThat(chunk.contentLength).isEqualTo(5000L)
    assertThat(chunk.notificationID).isEqualTo(42)
    assertThat(chunk.startByte).isEqualTo(100L)
    assertThat(chunk.endByte).isEqualTo(200L)
  }

  @Test
  fun `constructor accepts null fileName and url`() {
    val chunk = createChunk(fileName = null, url = null)
    assertThat(chunk.fileName).isNull()
    assertThat(chunk.url).isNull()
  }

  @Test
  fun `size returns 1 plus endByte minus startByte`() {
    val chunk = createChunk(0, 99)
    assertThat(chunk.size).isEqualTo(100)
  }

  @Test
  fun `size is correct when startByte is non-zero`() {
    val chunk = createChunk(50, 149)
    assertThat(chunk.size).isEqualTo(100)
  }

  @Test
  fun `size is 1 when startByte equals endByte`() {
    val chunk = createChunk(0, 0)
    assertThat(chunk.size).isEqualTo(1)
  }

  @Test
  fun `size works for large ranges`() {
    val start = 0L
    val end = ChunkUtils.CHUNK_SIZE
    val chunk = createChunk(start, end)
    assertThat(chunk.size).isEqualTo(ChunkUtils.CHUNK_SIZE + 1)
  }

  @Test
  fun `size works near long boundaries`() {
    val start = Long.MAX_VALUE - 10
    val end = Long.MAX_VALUE - 1

    val chunk = createChunk(
      start = start,
      end = end,
      range = "$start-$end",
      contentLength = Long.MAX_VALUE
    )
    assertThat(chunk.size).isEqualTo(10)
  }

  @Test
  fun `size calculation still follows formula when startByte greater than endByte`() {
    val chunk = createChunk(100, 50)
    assertThat(chunk.size).isEqualTo(-49)
  }

  @Test
  fun `size returns consistent value across multiple accesses`() {
    val chunk = createChunk(0, 9)
    assertThat(chunk.size).isEqualTo(10)
    assertThat(chunk.size).isEqualTo(10)
    assertThat(chunk.size).isEqualTo(10)
  }

  @Test
  fun `isDownloaded defaults to false`() {
    val chunk = createChunk()
    assertThat(chunk.isDownloaded).isFalse()
  }

  @Test
  fun `isDownloaded can be set to true`() {
    val chunk = createChunk()
    chunk.isDownloaded = true
    assertThat(chunk.isDownloaded).isTrue()
  }

  @Test
  fun `isDownloaded can toggle between states`() {
    val chunk = createChunk()
    chunk.isDownloaded = true
    assertThat(chunk.isDownloaded).isTrue()

    chunk.isDownloaded = false
    assertThat(chunk.isDownloaded).isFalse()
  }

  @Test
  fun `size is correct for large byte ranges`() {
    val startByte = 0L
    val endByte = ChunkUtils.CHUNK_SIZE
    val chunk = createChunk(start = startByte, end = endByte)
    assertThat(chunk.size).isEqualTo(ChunkUtils.CHUNK_SIZE + 1)
  }
}
