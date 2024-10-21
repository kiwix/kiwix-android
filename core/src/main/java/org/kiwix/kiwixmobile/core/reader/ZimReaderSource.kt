/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.reader

import android.content.res.AssetFileDescriptor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.extensions.canReadFile
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.getAssetFileDescriptorFromUri
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.isFileDescriptorCanOpenWithLibkiwix
import org.kiwix.libzim.Archive
import org.kiwix.libzim.FdInput
import java.io.File

class ZimReaderSource(
  val file: File? = null,
  val uri: Uri? = null,
  val assetFileDescriptorList: List<AssetFileDescriptor>? = null
) {
  constructor(uri: Uri) : this(
    uri = uri,
    assetFileDescriptorList = getAssetFileDescriptorFromUri(CoreApp.instance, uri)
  )

  constructor(file: File) : this(file = file, uri = null)

  companion object {
    fun fromDatabaseValue(databaseValue: String?) =
      databaseValue?.run {
        if (startsWith("content://")) ZimReaderSource(toUri())
        else ZimReaderSource(File(this))
      }
  }

  fun exists(): Boolean {
    return when {
      file != null -> file.isFileExist()
      assetFileDescriptorList?.isNotEmpty() == true ->
        assetFileDescriptorList[0].parcelFileDescriptor.fileDescriptor.valid()

      else -> false
    }
  }

  suspend fun canOpenInLibkiwix(): Boolean {
    return when {
      file?.canReadFile() == true -> true
      assetFileDescriptorList?.get(0)?.parcelFileDescriptor?.fd
        ?.let(::isFileDescriptorCanOpenWithLibkiwix) == true -> true

      else -> false
    }
  }

  suspend fun createArchive(): Archive? {
    if (canOpenInLibkiwix()) {
      return when {
        file != null -> Archive(file.canonicalPath)
        assetFileDescriptorList?.isNotEmpty() == true -> {
          val fdInputArray = getFdInputArrayFromAssetFileDescriptorList(assetFileDescriptorList)
          if (fdInputArray.size == 1) {
            Archive(fdInputArray[0])
          } else {
            Archive(fdInputArray)
          }
        }

        else -> null
      }
    }

    return null
  }

  private fun getFdInputArrayFromAssetFileDescriptorList(
    assetFileDescriptorList: List<AssetFileDescriptor>
  ): Array<FdInput> =
    assetFileDescriptorList.map {
      FdInput(
        it.parcelFileDescriptor.fileDescriptor,
        it.startOffset,
        it.length
      )
    }.toTypedArray()

  fun toDatabase(): String = file?.canonicalPath ?: uri.toString()

  override fun equals(other: Any?): Boolean {
    return when {
      file != null && other is ZimReaderSource && other.file != null ->
        file.canonicalPath == other.file.canonicalPath

      uri != null && other is ZimReaderSource && other.uri != null -> uri == other.uri
      else -> false
    }
  }

  fun getUri(activity: AppCompatActivity): Uri? {
    return when {
      file != null -> {
        FileProvider.getUriForFile(
          activity,
          "${activity.packageName}.fileprovider",
          file
        )
      }

      else -> uri
    }
  }

  override fun hashCode(): Int = file?.hashCode() ?: assetFileDescriptorList.hashCode()
}
