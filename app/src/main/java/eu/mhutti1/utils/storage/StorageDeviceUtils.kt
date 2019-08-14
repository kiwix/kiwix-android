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

import android.content.Context
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileFilter
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.util.ArrayList

object StorageDeviceUtils {

  @JvmStatic
  fun getStorageDevices(context: Context, writable: Boolean): List<StorageDevice> {
    val storageDevices = ArrayList<StorageDevice>()

    storageDevices.add(environmentDevices(Environment.getExternalStorageDirectory().path, writable))
    storageDevices.addAll(externalMountPointDevices())
    storageDevices.addAll(externalFilesDirsDevices(context, writable))

    return validate(storageDevices, writable).also(::deleteStorageMarkers)
  }

  private fun externalFilesDirsDevices(
    context: Context,
    writable: Boolean
  ) = ContextCompat.getExternalFilesDirs(context, "")
    .filterNotNull()
    .map { dir -> StorageDevice(generalisePath(dir.path, writable), false) }

  private fun externalMountPointDevices(): Collection<StorageDevice> {
    val storageDevices = ArrayList<StorageDevice>()
    for (path in ExternalPaths.possiblePaths) {
      if (path.endsWith("*")) {
        val root = File(path.substringBeforeLast("*"))
        root.listFiles(FileFilter(File::isDirectory))
          ?.mapTo(storageDevices) { dir -> StorageDevice(dir, false) }
      } else {
        storageDevices.add(StorageDevice(path, false))
      }
    }
    return storageDevices
  }

  private fun environmentDevices(
    environmentPath: String,
    writable: Boolean
  ) =
    // This is our internal storage directory
    if (Environment.isExternalStorageEmulated())
      StorageDevice(generalisePath(environmentPath, writable), true)
    // This is an external storage directory
    else StorageDevice(generalisePath(environmentPath, writable), false)

  // Remove app specific path from directories so that we can search them from the top
  private fun generalisePath(path: String, writable: Boolean) =
    if (writable) path
    else path.substringBeforeLast("/Android/data/")

  // Amazingly file.canWrite() does not always return the correct value
  private fun canWrite(file: File) = "$file/test.txt".let {
    try {
      RandomAccessFile(it, "rw").use { randomAccessFile ->
        randomAccessFile.channel.use { channel ->
          channel.lock().also(FileLock::release)
          true
        }
      }
      false
    } finally {
      File(it).delete()
    }
  }

  private fun validate(
    storageDevices: ArrayList<StorageDevice>,
    writable: Boolean
  ): List<StorageDevice> {
    return storageDevices.asSequence().distinct()
      .filter { it.file.exists() }
      .filter { it.file.isDirectory }
      .filter { (!writable || canWrite(it.file)) }
      .filterNot(StorageDevice::isDuplicate).toList()
  }

  private fun deleteStorageMarkers(validatedDevices: List<StorageDevice>) {
    validatedDevices.forEach { recursiveDeleteStorageMarkers(it.file) }
  }

  private fun recursiveDeleteStorageMarkers(file: File) {
    file.listFiles().forEach {
      if (it.isDirectory) {
        recursiveDeleteStorageMarkers(it)
      } else if (it.extension == "storageMarker") {
        it.delete()
      }
    }
  }
}
