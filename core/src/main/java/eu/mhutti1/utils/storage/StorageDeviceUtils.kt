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

package eu.mhutti1.utils.storage

import android.content.Context
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileFilter
import java.io.RandomAccessFile
import java.util.ArrayList

object StorageDeviceUtils {
  @JvmStatic
  fun getWritableStorage(context: Context) = validate(externalFilesDirsDevices(context, true), true)

  @JvmStatic
  fun getReadableStorage(context: Context): List<StorageDevice> {
    val storageDevices = ArrayList<StorageDevice>().apply {
      add(environmentDevices())
      addAll(externalMountPointDevices())
      addAll(externalFilesDirsDevices(context, false))
    }
    return validate(storageDevices, false)
  }

  private fun externalFilesDirsDevices(
    context: Context,
    writable: Boolean
  ) = ContextCompat.getExternalFilesDirs(context, "")
    .filterNotNull()
    .mapIndexed { index, dir -> StorageDevice(generalisePath(dir.path, writable), index == 0) }

  private fun externalMountPointDevices(): Collection<StorageDevice> =
    ExternalPaths.possiblePaths.fold(mutableListOf(), { acc, path ->
      acc.apply {
        if (path.endsWith("*")) {
          addAll(devicesBeneath(File(path.substringBeforeLast("*"))))
        } else {
          add(StorageDevice(path, false))
        }
      }
    })

  private fun devicesBeneath(directory: File) =
    directory.listFiles(FileFilter(File::isDirectory))
      ?.map { dir -> StorageDevice(dir, false) }
      .orEmpty()

  private fun environmentDevices() =
    StorageDevice(
      generalisePath(Environment.getExternalStorageDirectory().path, false),
      Environment.isExternalStorageEmulated()
    )

  // Remove app specific path from directories so that we can search them from the top
  private fun generalisePath(path: String, writable: Boolean) =
    if (writable) path
    else path.substringBefore("/Android/data/")

  // Amazingly file.canWrite() does not always return the correct value
  private fun canWrite(file: File): Boolean = "$file/test.txt".let {
    try {
      RandomAccessFile(it, "rw").use { randomAccessFile ->
        randomAccessFile.channel.use { channel ->
          channel.lock().use { fileLock ->
            fileLock.release()
            true
          }
        }
      }
    } catch (ignore: Exception) {
      false
    } finally {
      File(it).delete()
    }
  }

  private fun validate(
    storageDevices: List<StorageDevice>,
    writable: Boolean
  ) = storageDevices.asSequence()
    .filter { it.file.exists() }
    .filter { it.file.isDirectory }
    .distinctBy { it.file.canonicalPath }
    .filter { !writable || canWrite(it.file) }
    .toList()
}
