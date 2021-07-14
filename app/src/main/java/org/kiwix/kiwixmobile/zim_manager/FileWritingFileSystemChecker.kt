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

import android.util.Log
import org.kiwix.kiwixmobile.zim_manager.FileSystemCapability.CANNOT_WRITE_4GB
import org.kiwix.kiwixmobile.zim_manager.FileSystemCapability.CAN_WRITE_4GB
import org.kiwix.kiwixmobile.zim_manager.FileSystemCapability.INCONCLUSIVE
import java.io.File
import java.io.RandomAccessFile

class FileWritingFileSystemChecker : FileSystemChecker {
  override fun checkFilesystemSupports4GbFiles(path: String): FileSystemCapability {
    val resultFile = File("$path/.file_writing_result")
    if (resultFile.exists()) {
      when (val capability = readCapability(resultFile)) {
        CAN_WRITE_4GB,
        CANNOT_WRITE_4GB -> return capability
      }
    }
    return with(File("$path/large_file_test.txt"), {
      deleteIfExists()
      try {
        RandomAccessFile(this.path, "rw").use {
          it.setLength(Fat32Checker.FOUR_GIGABYTES_IN_BYTES)
          CAN_WRITE_4GB.alsoSaveTo(resultFile)
        }
      } catch (e: Exception) {
        e.printStackTrace()
        e.message?.let { message ->
          Log.d("Fat32Checker", message)
        }
        CANNOT_WRITE_4GB.alsoSaveTo(resultFile)
      } finally {
        deleteIfExists()
      }
    })
  }

  private fun readCapability(resultFile: File) =
    try {
      FileSystemCapability.valueOf(resultFile.readText())
    } catch (illegalArgumentException: IllegalArgumentException) {
      INCONCLUSIVE
    }

  private fun FileSystemCapability.alsoSaveTo(resultFile: File) =
    also { resultFile.writeText(name) }
}

private fun File.deleteIfExists() {
  if (exists()) delete()
}
