/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */

package org.kiwix.kiwixmobile.downloader

import io.mockk.every
import io.mockk.mockkStatic
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.utils.StorageUtils

class ChunkUtilsTest {

  private val url =
    "http://mirror.netcologne.de/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"

  init {
    mockkStatic("org.kiwix.kiwixmobile.utils.StorageUtils")
    every { StorageUtils.getFileNameFromUrl(url) }.returns("TestFileName")
    every { StorageUtils.getFileNameFromUrl("TestURL") }.returns("TestFileName.xml")
  }

  @Test
  fun testGetChunks() {
    var listReturned: List<Chunk>
    var size = ChunkUtils.CHUNK_SIZE

    // When the file size is exactly equal to CHUNK_SIZE
    listReturned = ChunkUtils.getChunks(url, size, 27)

    assertEquals(
      "verify that the list contains correct number of chunks", 1, listReturned.size.toLong()
    )
    assertEquals(
      "verify that the range format is correct", "0-",
      listReturned[0].rangeHeader
    )
    assertEquals(
      "verify that the same notificationID is passed to the chunk", 27,
      listReturned[0].notificationID.toLong()
    )
    assertEquals(
      "verify that the file name is correctly assigned in case of a single file",
      "TestFileName.part.part", listReturned[0].fileName
    )
    assertEquals(
      "verify that the same URL is passed on to the chunk", url,
      listReturned[0].url
    )

    // When the file size is more than CHUNK_SIZE
    size = ChunkUtils.CHUNK_SIZE * 5.toLong() + (1024 * 1024).toLong()
    listReturned = ChunkUtils.getChunks(url, size, 56)

    assertEquals(
      "verify that the list contains correct number of chunks", 6, listReturned.size.toLong()
    )
    assertEquals(
      "verify that the rangehandler for the last chunk is correct", "10737418245-",
      listReturned[listReturned.size - 1].rangeHeader
    )
    assertEquals(
      "verify that the rangehandler for the first chunk is corect", "0-2147483648",
      listReturned[0].rangeHeader
    )

    assertEquals(
      "verify that the same notificationID is passed on to each chunk",
      true, listReturned[0].url == url &&
        listReturned[1].url == url &&
        listReturned[2].url == url &&
        listReturned[3].url == url &&
        listReturned[4].url == url &&
        listReturned[5].url == url
    )

    assertEquals(
      "verify that the same URL is passed on to each chunk",
      true, listReturned[0].notificationID == 56 &&
        listReturned[1].notificationID == 56 &&
        listReturned[2].notificationID == 56 &&
        listReturned[3].notificationID == 56 &&
        listReturned[4].notificationID == 56 &&
        listReturned[5].notificationID == 56
    )

    // test assignment of file names
    val alphabet = "abcdefghijklmnopqrstuvwxyz"
    for (i in listReturned.indices) {
      val extension = listReturned[i]
        .fileName
        .substringAfter('.')
      val expectedExtension = "zim" + alphabet[i / 26] + alphabet[i % 26] + ".part.part"
      assertThat(extension).isEqualTo(expectedExtension)
    }

    // When the file size is less than CHUNK_SIZE
    size = ChunkUtils.CHUNK_SIZE - (1024 * 1024).toLong()
    listReturned = ChunkUtils.getChunks(url, size, 37)

    assertEquals(
      "verify that the list contains correct number of chunks", 1, listReturned.size.toLong()
    )
    assertEquals(
      "verify that the range format is correct", "0-",
      listReturned[0].rangeHeader
    )
    assertEquals(
      "verify that the same notificationID is passed to the chunk", 37,
      listReturned[0].notificationID.toLong()
    )
    assertEquals(
      "verify that the file name is correctly assigned in case of a single file",
      "TestFileName.part.part", listReturned[0].fileName
    )
    assertEquals(
      "verify that the same URL is passed on to the chunk", url,
      listReturned[0].url
    )

    // verify that filename is correctly generated
    size = ChunkUtils.CHUNK_SIZE
    listReturned = ChunkUtils.getChunks("TestURL", size, 0)
    assertEquals(
      "verify that previous extension in the filename (if any) is removed" +
        " in case of files having 1 chunk",
      "TestFileName.xml.part.part", listReturned[0].fileName
    )

    size = ChunkUtils.CHUNK_SIZE * 2.toLong()
    listReturned = ChunkUtils.getChunks("TestURL", size, 0)
    assertEquals(
      "verify that previous extension in the filename (if any) is removed" +
        " in case of files having more than 1 chunk",
      "TestFileName.zimaa.part.part", listReturned[0].fileName
    )
    assertEquals(
      "verify that previous extension in the filename (if any) is removed" +
        " in case of files having more than 1 chunk",
      "TestFileName.zimab.part.part", listReturned[1].fileName
    )
  }
}
