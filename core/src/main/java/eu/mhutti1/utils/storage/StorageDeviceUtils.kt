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
import android.content.ContextWrapper
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import java.io.File
import java.io.FileFilter
import java.io.RandomAccessFile

object StorageDeviceUtils {
  @JvmStatic
  suspend fun getWritableStorage(
    context: Context,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
  ) = withContext(dispatcher) {
    validate(externalMediaFilesDirsDevices(context), true)
  }

  @JvmStatic
  fun getReadableStorage(context: Context): List<StorageDevice> {
    var sharedPreferenceUtil: SharedPreferenceUtil? = SharedPreferenceUtil(context)
    val storageDevices =
      ArrayList<StorageDevice>().apply {
        add(environmentDevices(context))
        addAll(externalMediaFilesDirsDevices(context))
        // Scan the app-specific directory as well because we have limitations in scanning
        // all directories on Android 11 and above in the Play Store variant.
        // If a user copies the ZIM file to the app-specific directory on the SD card,
        // the scanning of the app-specific directory on the SD card has not been added,
        // resulting in the copied files not being displayed on the library screen.
        // Therefore, we need to explicitly include the app-specific directory for scanning.
        // See https://github.com/kiwix/kiwix-android/issues/3579
        addAll(externalFilesDirsDevices(context, true))
        // Do not scan storage directories in the Play Store build on Android 11 and above.
        // In the Play Store variant, we can only access app-specific directories,
        // so scanning other directories is unnecessary, wastes resources,
        // and increases the scanning time.
        if (sharedPreferenceUtil?.isPlayStoreBuild == false ||
          Build.VERSION.SDK_INT < Build.VERSION_CODES.R
        ) {
          addAll(externalMountPointDevices())
          addAll(externalFilesDirsDevices(context, false))
        }
        sharedPreferenceUtil = null
      }
    return validate(storageDevices, false)
  }

  private fun externalFilesDirsDevices(
    context: Context,
    writable: Boolean
  ) = context.getExternalFilesDirs("")
    .filterNotNull()
    .mapIndexed { index, dir -> StorageDevice(generalisePath(dir.path, writable), index == 0) }

  private fun externalMediaFilesDirsDevices(context: Context) =
    ContextWrapper(context).externalMediaDirs
      .filterNotNull()
      .mapIndexed { index, dir -> StorageDevice(generalisePath(dir.path, true), index == 0) }

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

  private fun environmentDevices(context: Context) =
    StorageDevice(
      generalisePath(context.getExternalFilesDir("").toString(), true),
      Environment.isExternalStorageEmulated()
    )

  // Remove app specific path from directories so that we can search them from the top
  private fun generalisePath(path: String, writable: Boolean) =
    if (writable) {
      path
    } else {
      path.substringBefore("/Android/data/")
    }

  // Amazingly file.canWrite() does not always return the correct value
  @Suppress("NestedBlockDepth")
  private fun canWrite(file: File): Boolean =
    "$file/test.txt".let {
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
