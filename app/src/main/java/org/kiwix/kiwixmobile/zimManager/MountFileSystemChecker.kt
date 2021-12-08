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

package org.kiwix.kiwixmobile.zimManager

import org.kiwix.kiwixmobile.core.zim_manager.MountInfo
import org.kiwix.kiwixmobile.core.zim_manager.MountPointProducer
import org.kiwix.kiwixmobile.zim_manager.FileSystemCapability.CANNOT_WRITE_4GB
import org.kiwix.kiwixmobile.zim_manager.FileSystemCapability.CAN_WRITE_4GB
import org.kiwix.kiwixmobile.zim_manager.FileSystemCapability.INCONCLUSIVE
import javax.inject.Inject

class MountFileSystemChecker @Inject constructor(
  private val mountPointProducer: MountPointProducer
) : FileSystemChecker {
  override fun checkFilesystemSupports4GbFiles(path: String) =
    recursivelyDetermineFilesystem(mountPointProducer.produce(), path)

  private fun recursivelyDetermineFilesystem(mountPoints: List<MountInfo>, path: String):
    FileSystemCapability =
    mountPoints.maxBy { it.matchCount(path) }
      ?.takeIf { it.matchCount(path) > 0 }
      ?.let {
        when {
          it.isVirtual -> recursivelyDetermineFilesystem(mountPoints - it, it.device)
          it.supports4GBFiles -> CAN_WRITE_4GB
          it.doesNotSupport4GBFiles -> CANNOT_WRITE_4GB
          else -> INCONCLUSIVE
        }
      } ?: INCONCLUSIVE
}
