/*
 * Copyright 2016 Isaac Hutt <mhutti1@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package eu.mhutti1.utils.storage

import android.os.Build
import android.os.StatFs
import java.io.File

data class StorageDevice(
  val file: File,
  val isInternal: Boolean
) {

  constructor(path: String, internal: Boolean) : this(File(path), internal)

  val name: String
    get() = file.path

  val availableSpace: String
    get() = Bytes(availableBytes).humanReadable

  private val availableBytes: Long
    get() = StatFs(file.path).let {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
        it.blockSizeLong * it.availableBlocksLong
      else
        it.blockSize.toLong() * it.availableBlocks.toLong()
    }

  val totalSize: String
    get() = Bytes(totalBytes).humanReadable

  // Get total space on device
  private val totalBytes: Long
    get() = StatFs(file.path).let {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
        it.blockSizeLong * it.blockCountLong
      else
        it.blockSize.toLong() * it.blockCount.toLong()
    }
}
