/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.zim_manager

import org.kiwix.kiwixmobile.zim_manager.FileSystemCapability.CANNOT_WRITE_4GB
import org.kiwix.kiwixmobile.zim_manager.FileSystemCapability.CAN_WRITE_4GB
import org.kiwix.kiwixmobile.zim_manager.FileSystemCapability.INCONCLUSIVE
import java.io.File

class MountFileSystemChecker : FileSystemChecker {
  override fun checkFilesystemSupports4GbFiles(path: String) =
    recursivelyDetermineFilesystem(mountPoints(), path)

  private fun recursivelyDetermineFilesystem(
    mountPoints: List<MountInfo>,
    path: String
  ): FileSystemCapability =
    mountPoints.maxBy { it.matchCount(path) }
      ?.takeIf { it.matchCount(path) > 0 }
      ?.let {
        when {
          it.isVirtual -> recursivelyDetermineFilesystem(mountPoints, it.device)
          it.supports4GBFiles -> CAN_WRITE_4GB
          it.doesNotSupport4GBFiles -> CANNOT_WRITE_4GB
          else -> INCONCLUSIVE
        }
      } ?: INCONCLUSIVE

  private fun mountPoints() =
    File("proc/mounts")
      .takeIf(File::exists)
      ?.readLines()
      ?.map { MountInfo(it.split(" ")) }
      ?: emptyList()
}

data class MountInfo(val device: String, val mountPoint: String, val fileSystem: String) {
  constructor(split: List<String>) : this(split[0], split[1], split[2])

  fun matchCount(storage: String) = storage.split("/")
    .zip(mountPoint.split("/"))
    .fold(0, { acc, pair ->
      if (pair.first == pair.second) acc + 1
      else acc
    })

  val isVirtual = VIRTUAL_FILE_SYSTEMS.contains(fileSystem)
  val supports4GBFiles = SUPPORTS_4GB_FILE_SYSTEMS.contains(fileSystem)
  val doesNotSupport4GBFiles = DOES_NOT_SUPPORT_4GB_FILE_SYSTEMS.contains(fileSystem)

  companion object {
    private val VIRTUAL_FILE_SYSTEMS = listOf("fuse", "sdcardfs", "tmpfs")
    private val SUPPORTS_4GB_FILE_SYSTEMS = listOf("ext4", "exfat")
    private val DOES_NOT_SUPPORT_4GB_FILE_SYSTEMS = listOf("fat32")
  }
}
