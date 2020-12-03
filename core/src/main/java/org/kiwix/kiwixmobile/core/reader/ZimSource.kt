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

package org.kiwix.kiwixmobile.core.reader

import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import org.kiwix.kiwixlib.JNIKiwixReader
import org.kiwix.kiwixmobile.core.CoreApp
import java.io.File
import java.io.FileDescriptor

sealed class ZimSource {
  abstract fun exists(): Boolean
  abstract fun createJniReader(): JNIKiwixReader
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

  override fun hashCode(): Int {
    return when (this) {
      is ZimFile -> file.hashCode()
      is ZimFileDescriptor -> fileDescriptor.hashCode()
    }
  }

  fun asUri(activity: AppCompatActivity): Uri {
    return when (this) {
      is ZimFile -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          FileProvider.getUriForFile(
            activity,
            activity.packageName + ".fileprovider",
            file
          )
        } else {
          Uri.fromFile(file)
        }
      }
      is ZimFileDescriptor -> uri
    }
  }

  class ZimFile(val file: File) : ZimSource() {
    override fun exists(): Boolean = file.exists()
    override fun createJniReader(): JNIKiwixReader = JNIKiwixReader(file.canonicalPath)
    override fun toDatabase(): String = file.canonicalPath
  }

  class ZimFileDescriptor(val uri: Uri, val fileDescriptor: FileDescriptor) : ZimSource() {
    constructor(uri: Uri) : this(
      uri,
      CoreApp.instance.contentResolver.openFileDescriptor(uri, "r")!!.fileDescriptor
    )

    override fun exists(): Boolean = fileDescriptor.valid()
    override fun createJniReader(): JNIKiwixReader = JNIKiwixReader(fileDescriptor)
    override fun toDatabase(): String = "$uri"
  }
}
