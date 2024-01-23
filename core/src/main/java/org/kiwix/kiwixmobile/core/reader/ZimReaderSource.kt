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
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.getAssetFileDescriptorFromUri
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.isFileDescriptorCanOpenWithLibkiwix
import org.kiwix.libzim.Archive
import java.io.File

sealed class ZimReaderSource {
  abstract fun exists(): Boolean
  abstract fun canOpenInLibkiwix(): Boolean
  abstract fun createArchive(): Archive?
  abstract fun toDatabase(): String

  companion object {
    fun fromDatabaseValue(databaseValue: String?) =
      databaseValue?.let {
        if (it.startsWith("content://")) ZimFileDescriptor(it.toUri())
        else ZimFile(File(it))
      }
  }

  override fun equals(other: Any?): Boolean {
    return when {
      other is ZimFile && this is ZimFile -> file.canonicalPath == other.file.canonicalPath
      other is ZimFileDescriptor && this is ZimFileDescriptor -> uri == other.uri
      else -> false
    }
  }

  fun getUri(activity: AppCompatActivity): Uri? {
    return when (this) {
      is ZimFile -> {
        FileProvider.getUriForFile(
          activity,
          activity.packageName + ".fileprovider",
          file
        )
      }

      is ZimFileDescriptor -> uri
    }
  }

  override fun hashCode(): Int {
    return when (this) {
      is ZimFile -> file.hashCode()
      is ZimFileDescriptor -> assetFileDescriptor.hashCode()
    }
  }

  class ZimFile(val file: File) : ZimReaderSource() {
    override fun exists() = file.exists()
    override fun canOpenInLibkiwix(): Boolean = file.canReadFile()

    override fun createArchive() = Archive(file.canonicalPath)
    override fun toDatabase(): String = file.canonicalPath
  }

  class ZimFileDescriptor(val uri: Uri?, val assetFileDescriptor: AssetFileDescriptor?) :
    ZimReaderSource() {

    constructor(uri: Uri) : this(
      uri,
      getAssetFileDescriptorFromUri(CoreApp.instance, uri)
    )

    override fun exists(): Boolean =
      assetFileDescriptor?.parcelFileDescriptor?.fileDescriptor?.valid() == true

    override fun canOpenInLibkiwix(): Boolean =
      isFileDescriptorCanOpenWithLibkiwix(assetFileDescriptor?.parcelFileDescriptor?.fd)

    override fun createArchive() = assetFileDescriptor?.let {
      Archive(
        assetFileDescriptor.parcelFileDescriptor.dup().fileDescriptor,
        assetFileDescriptor.startOffset,
        assetFileDescriptor.length
      )
    }

    override fun toDatabase(): String = uri.toString()
  }
}
