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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.downloader.ChunkUtils
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.extensions.get
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import java.io.BufferedReader
import java.io.File
import java.io.IOException

object FileUtils {

  @JvmStatic fun getFileCacheDir(context: Context): File? =
    if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
      context.externalCacheDir
    } else {
      context.cacheDir
    }

  @JvmStatic @Synchronized fun deleteCachedFiles(context: Context) {
    getFileCacheDir(context)?.deleteRecursively()
  }

  @JvmStatic @Synchronized fun deleteZimFile(path: String) {
    var path = path
    if (path.substring(path.length - ChunkUtils.PART.length) == ChunkUtils.PART) {
      path = path.substring(0, path.length - ChunkUtils.PART.length)
    }
    Log.i("kiwix", "Deleting file: $path")
    val file = File(path)
    if (file.path.substring(file.path.length - 3) != "zim") {
      var alphabetFirst = 'a'
      fileloop@ while (alphabetFirst <= 'z') {
        var alphabetSecond = 'a'
        while (alphabetSecond <= 'z') {
          val chunkPath = path.substring(0, path.length - 2) + alphabetFirst + alphabetSecond
          val fileChunk = File(chunkPath)
          if (fileChunk.exists()) {
            fileChunk.delete()
          } else if (!deleteZimFileParts(chunkPath)) {
            break@fileloop
          }
          alphabetSecond++
        }
        alphabetFirst++
      }
    } else {
      file.delete()
      deleteZimFileParts(path)
    }
  }

  @Synchronized private fun deleteZimFileParts(path: String): Boolean {
    val file = File(path + ChunkUtils.PART)
    if (file.exists()) {
      file.delete()
      return true
    }
    val singlePart = File("$path.part")
    if (singlePart.exists()) {
      singlePart.delete()
      return true
    }
    return false
  }

  @JvmStatic fun getLocalFilePathByUri(
    context: Context,
    uri: Uri
  ): String? {
    if (DocumentsContract.isDocumentUri(context, uri)) {
      if ("com.android.externalstorage.documents" == uri.authority) {
        val documentId = DocumentsContract.getDocumentId(uri)
          .split(":")

        if (documentId[0] == "primary") {
          return "${Environment.getExternalStorageDirectory()}/${documentId[1]}"
        }
        return try {
          "${getSdCardMainPath(context)}/${documentId[1]}"
        } catch (ignore: Exception) {
          null
        }
      } else if ("com.android.providers.downloads.documents" == uri.authority)
        return try {
          documentProviderContentQuery(context, uri)
        } catch (ignore: IllegalArgumentException) {
          null
        }
    } else if (uri.scheme != null) {
      if ("content".equals(uri.scheme!!, ignoreCase = true)) {
        return contentQuery(context, uri)
      } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
        return uri.path
      }
    } else {
      return uri.path
    }

    return null
  }

  private fun documentProviderContentQuery(context: Context, uri: Uri) =
    contentQuery(
      context,
      ContentUris.withAppendedId(
        Uri.parse("content://downloads/public_downloads"),
        try {
          DocumentsContract.getDocumentId(uri).toLong()
        } catch (ignore: NumberFormatException) {
          0L
        }
      )
    )

  private fun contentQuery(
    context: Context,
    uri: Uri
  ): String? {
    val columnName = "_data"
    return try {
      context.contentResolver.query(uri, arrayOf(columnName), null, null, null)
        ?.use {
          if (it.moveToFirst() && it.getColumnIndex(columnName) != -1) {
            it[columnName]
          } else null
        }
    } catch (ignore: SecurityException) {
      null
    } catch (ignore: NullPointerException) {
      null
    }
  }

  @JvmStatic fun readLocalesFromAssets(context: Context) =
    readContentFromLocales(context).split(',')

  private fun readContentFromLocales(context: Context): String {
    try {
      context.assets.open("locales.txt")
        .use {
          val buffer = ByteArray(it.available())
          it.read(buffer)
          return@readContentFromLocales String(buffer)
        }
    } catch (ignored: IOException) {
      return ""
    }
  }

  @JvmStatic fun getAllZimParts(book: Book): List<File> {
    val files = ArrayList<File>()
    if (book.file.path.endsWith(".zim") || book.file.path.endsWith(".zim.part")) {
      if (book.file.exists()) {
        files.add(book.file)
      } else {
        files.add(File(book.file.toString() + ".part"))
      }
      return files
    }
    var path = book.file.path
    for (firstCharacter in 'a'..'z') {
      for (secondCharacter in 'a'..'z') {
        path = path.substring(0, path.length - 2) + firstCharacter + secondCharacter
        when {
          File(path).exists() -> files.add(File(path))
          File("$path.part").exists() -> files.add(File("$path.part"))
          else -> return files
        }
      }
    }
    return files
  }

  @JvmStatic fun hasPart(file: File): Boolean {
    var file = file
    file = File(getFileName(file.path))
    if (file.path.endsWith(".zim")) {
      return false
    }
    if (file.path.endsWith(".part")) {
      return true
    }
    val path = file.path
    for (firstCharacter in 'a'..'z') {
      for (secondCharacter in 'a'..'z') {
        val chunkPath = path.substring(0, path.length - 2) + firstCharacter + secondCharacter
        val fileChunk = File("$chunkPath.part")
        if (fileChunk.exists()) {
          return true
        } else if (!File(chunkPath).exists()) {
          return false
        }
      }
    }
    return false
  }

  @JvmStatic fun getFileName(fileName: String) =
    when {
      File(fileName).exists() -> fileName
      File("$fileName.part").exists() -> "$fileName.part"
      else -> "${fileName}aa"
    }

  @JvmStatic fun Context.readFile(filePath: String): String = try {
    assets.open(filePath)
      .bufferedReader()
      .use(BufferedReader::readText)
  } catch (e: IOException) {
    "".also { e.printStackTrace() }
  }

  @JvmStatic fun isValidZimFile(filePath: String): Boolean =
    filePath.endsWith(".zim") || filePath.endsWith(".zimaa")

  @JvmStatic fun getSdCardMainPath(context: Context): String =
    "${context.getExternalFilesDirs("")[1]}"
      .substringBefore(context.getString(R.string.android_directory_seperator))

  @SuppressLint("WrongConstant")
  @JvmStatic fun getPathFromUri(activity: Activity, data: Intent): String? {
    val uri: Uri? = data.data
    val takeFlags: Int = data.flags and (
      Intent.FLAG_GRANT_READ_URI_PERMISSION
        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
      )
    uri?.let {
      activity.grantUriPermission(
        activity.packageName, it,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
      )
      activity.contentResolver.takePersistableUriPermission(it, takeFlags)

      val dFile = DocumentFile.fromTreeUri(activity, it)
      if (dFile != null) {
        dFile.uri.path?.let { file ->
          val originalPath = file.substring(
            file.lastIndexOf(":") + 1
          )
          val path = "${activity.getExternalFilesDirs("")[1]}"
          return@getPathFromUri path.substringBefore(
            activity.getString(R.string.android_directory_seperator)
          )
            .plus(File.separator).plus(originalPath)
        }
      }
      activity.toast(
        activity.resources
          .getString(R.string.system_unable_to_grant_permission_message),
        Toast.LENGTH_SHORT
      )
    } ?: run {
      activity.toast(
        activity.resources
          .getString(R.string.system_unable_to_grant_permission_message),
        Toast.LENGTH_SHORT
      )
    }
    return null
  }

  /**
   * Returns the file name from the url or src. In url it gets the file name from the last '/' and
   * if it contains '.'. If the url is null then it'll get the file name from the last '/'.
   * If the url and src doesn't exist it returns the empty string.
   */
  fun getDecodedFileName(url: String?, src: String?): String =
    url?.substringAfterLast("/", "")
      ?.takeIf { it.contains(".") }
      ?: src?.substringAfterLast("/", "")
        ?.substringAfterLast("%3A") ?: ""

  @JvmStatic fun downloadFileFromUrl(
    url: String?,
    src: String?,
    zimReaderContainer: ZimReaderContainer
  ): File? {
    val fileName = getDecodedFileName(url, src)
    var root =
      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    if (CoreApp.instance.externalMediaDirs.isNotEmpty()) {
      root = CoreApp.instance.externalMediaDirs[0]
    }
    if (File(root, fileName).exists()) return File(root, fileName)
    val fileToSave = sequence {
      yield(File(root, fileName))
      yieldAll(
        generateSequence(1) { it + 1 }.map {
          File(
            root, fileName.replace(".", "_$it.")
          )
        }
      )
    }.first { !it.exists() }
    val source = if (url == null) Uri.parse(src) else Uri.parse(url)
    return try {
      zimReaderContainer.load("$source", emptyMap()).data.use { inputStream ->
        fileToSave.outputStream().use(inputStream::copyTo)
      }
      fileToSave
    } catch (e: IOException) {
      Log.w("kiwix", "Couldn't save file", e)
      null
    }
  }
}
