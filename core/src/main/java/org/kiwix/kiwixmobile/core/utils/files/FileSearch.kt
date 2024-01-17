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

package org.kiwix.kiwixmobile.core.utils.files

import android.content.Context
import android.provider.MediaStore.Files
import android.provider.MediaStore.MediaColumns
import eu.mhutti1.utils.storage.StorageDevice
import eu.mhutti1.utils.storage.StorageDeviceUtils
import io.reactivex.Flowable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.extensions.forEachRow
import org.kiwix.kiwixmobile.core.extensions.get
import java.io.File
import javax.inject.Inject

class FileSearch @Inject constructor(private val context: Context) {

  private val zimFileExtensions = arrayOf("zim", "zimaa")

  fun scan(): Flowable<List<File>> =
    Flowable.combineLatest(
      Flowable.fromCallable(::scanFileSystem).subscribeOn(Schedulers.io()),
      Flowable.fromCallable(::scanMediaStore).subscribeOn(Schedulers.io()),
      BiFunction<List<File>, List<File>, List<File>> { filesSystemFiles, mediaStoreFiles ->
        filesSystemFiles + mediaStoreFiles
      }
    )

  private fun scanMediaStore() = mutableListOf<File>().apply {
    queryMediaStore()
      ?.forEachRow { cursor ->
        File(cursor.get<String>(MediaColumns.DATA)).takeIf(File::canRead)
          ?.also { add(it) }
      }
  }

  private fun queryMediaStore() = context.contentResolver
    .query(
      Files.getContentUri("external"),
      arrayOf(MediaColumns.DATA),
      MediaColumns.DATA + " like ? or " + MediaColumns.DATA + " like ? ",
      arrayOf("%." + zimFileExtensions[0], "%." + zimFileExtensions[1]),
      null
    )

  private fun scanFileSystem() =
    directoryRoots()
      .fold(mutableListOf<File>(), { acc, root ->
        acc.apply { addAll(scanDirectory(root)) }
      })
      .distinctBy { it.canonicalPath }

  private fun directoryRoots() =
    StorageDeviceUtils.getReadableStorage(context).map(StorageDevice::name)

  private fun scanDirectory(directory: String): List<File> {
    return File(directory).walk()
      .onEnter { dir ->
        // Excluding the "data," "obb," and "Trash" folders from scanning is justified for
        // several reasons. The "Trash" folder contains deleted files,
        // making it unnecessary for scanning. Additionally,
        // the "data" and "obb" folders are specifically designed for the
        // app's private directory, and users usually do not store ZIM files there.
        // Most file managers prohibit direct copying of files into these directories.
        // Therefore, scanning these folders is not essential. Moreover,
        // such scans consume time, given the presence of numerous files written by other apps,
        // which are irrelevant to our application.
        !dir.name.equals(".Trash", ignoreCase = true) &&
          !dir.name.equals("data", ignoreCase = true) &&
          !dir.name.equals("obb", ignoreCase = true)
      }.filter {
        it.extension.isAny(*zimFileExtensions)
      }.toList()
  }
}

internal fun String.isAny(vararg suffixes: String) =
  suffixes.firstOrNull { endsWith(it) } != null
