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

import org.kiwix.kiwixmobile.core.utils.StorageUtils.getFileNameFromUrl
import java.util.ArrayList
import java.util.Locale

object ChunkUtils {
  private const val ALPHABET = "abcdefghijklmnopqrstuvwxyz"
  private const val ZIM_EXTENSION = ".zim"
  const val PART = ".part.part"
  const val CHUNK_SIZE = 1024L * 1024L * 1024L * 2L

  fun getChunks(
    url: String?,
    contentLength: Long,
    notificationID: Int
  ): List<Chunk> {
    val fileCount = getZimChunkFileCount(contentLength)
    val filename = getFileNameFromUrl(url)
    val fileNames = getZimChunkFileNames(filename, fileCount)
    return generateChunks(contentLength, url, fileNames, notificationID)
  }

  private fun generateChunks(
    contentLength: Long,
    url: String?,
    fileNames: Array<String?>,
    notificationID: Int
  ): List<Chunk> {
    val chunks: MutableList<Chunk> = ArrayList()
    var currentRange: Long = 0
    for (zim in fileNames) {
      var range: String
      if (currentRange + CHUNK_SIZE >= contentLength) {
        range = String.format(Locale.US, "%d-", currentRange)
        chunks.add(
          Chunk(
            range, zim, url, contentLength, notificationID, currentRange, contentLength
          )
        )
        currentRange += CHUNK_SIZE + 1
      } else {
        range = String.format(
          Locale.US,
          "%d-%d",
          currentRange,
          currentRange + CHUNK_SIZE
        )
        chunks.add(
          Chunk(
            range, zim, url, contentLength, notificationID, currentRange, currentRange + CHUNK_SIZE
          )
        )
        currentRange += CHUNK_SIZE + 1
      }
    }
    return chunks
  }

  private fun getZimChunkFileCount(contentLength: Long): Int {
    val fits = (contentLength / CHUNK_SIZE).toInt()
    val hasRemainder = contentLength % CHUNK_SIZE > 0
    return if (hasRemainder) fits + 1 else fits
  }

  private fun getZimChunkFileNames(
    fileName: String,
    count: Int
  ): Array<String?> {
    if (count == 1) {
      return arrayOf(fileName + PART)
    }
    val position = fileName.lastIndexOf(".")
    val baseName = if (position > 0) fileName.substring(0, position) else fileName
    val fileNames = arrayOfNulls<String>(count)
    for (i in 0 until count) {
      val first = ALPHABET[i / 26]
      val second = ALPHABET[i % 26]
      val chunkExtension = "$first" + second
      fileNames[i] = baseName + ZIM_EXTENSION + chunkExtension + PART
    }
    return fileNames
  }
}
