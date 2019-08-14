/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.utils.files

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import org.kiwix.kiwixmobile.BuildConfig
import org.kiwix.kiwixmobile.downloader.ChunkUtils
import org.kiwix.kiwixmobile.extensions.get
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.utils.Constants.TAG_KIWIX
import java.io.File
import java.io.IOException
import java.util.ArrayList

object FileUtils {

  val saveFilePath =
    "${Environment.getExternalStorageDirectory()}${File.separator}Android" +
      "${File.separator}obb${File.separator}${BuildConfig.APPLICATION_ID}"

  @JvmStatic fun getFileCacheDir(context: Context): File =
    if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
      context.externalCacheDir!!
    } else {
      context.cacheDir
    }

  @JvmStatic @Synchronized fun deleteCachedFiles(context: Context) {
    getFileCacheDir(context).deleteRecursively()
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

  /**
   * Returns the file name (without full path) for an Expansion APK file from the given context.
   *
   * @param mainFile true for menu_main file, false for patch file
   * @return String the file name of the expansion file
   */
  @JvmStatic fun getExpansionAPKFileName(mainFile: Boolean) =
    "${if (mainFile) "main." else "patch."}${BuildConfig.CONTENT_VERSION_CODE}" +
      ".${BuildConfig.APPLICATION_ID}.obb"

  /**
   * Returns the filename (where the file should be saved) from info about a download
   */
  @JvmStatic fun generateSaveFileName(fileName: String) = "$saveFilePath${File.separator}$fileName"

  /**
   * Helper function to ascertain the existence of a file and return true/false appropriately
   *
   * @param fileName the name (sans path) of the file to query
   * @param fileSize the size that the file must match
   * @param deleteFileOnMismatch if the file sizes do not match, delete the file
   * @return true if it does exist, false otherwise
   */
  @JvmStatic fun doesFileExist(
    fileName: String,
    fileSize: Long,
    deleteFileOnMismatch: Boolean
  ): Boolean {

    Log.d(TAG_KIWIX, "Looking for '$fileName' with size=$fileSize")

    // the file may have been delivered by Market --- let's make sure
    // it's the size we expect
    val fileForNewFile = File(fileName)
    if (fileForNewFile.exists()) {
      if (fileForNewFile.length() == fileSize) {
        Log.d(TAG_KIWIX, "Correct file '$fileName' found.")
        return true
      }
      Log.d(
        TAG_KIWIX,
        "File '" + fileName + "' found but with wrong size=" + fileForNewFile.length()
      )
      if (deleteFileOnMismatch) {
        fileForNewFile.delete()
      }
    } else {
      Log.d(TAG_KIWIX, "No file '$fileName' found.")
    }
    return false
  }

  @JvmStatic fun getLocalFilePathByUri(
    ctx: Context,
    uri: Uri
  ): String? {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
      DocumentsContract.isDocumentUri(ctx, uri)
    ) {
      if ("com.android.externalstorage.documents" == uri.authority) {
        val documentId = DocumentsContract.getDocumentId(uri)
          .split(":")

        if (documentId[0] == "primary") {
          return "${Environment.getExternalStorageDirectory()}/${documentId[1]}"
        }
      } else if ("com.android.providers.downloads.documents" == uri.authority)
        return contentQuery(
          ctx,
          ContentUris.withAppendedId(
            Uri.parse("content://downloads/public_downloads"),
            DocumentsContract.getDocumentId(uri).toLong()
          )
        )
    } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {
      return contentQuery(ctx, uri)
    } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
      return uri.path
    }
    return null
  }

  private fun contentQuery(
    context: Context,
    uri: Uri
  ): String? {
    val columnName = "_data"
    return context.contentResolver.query(uri, arrayOf(columnName), null, null, null)
      ?.use {
        if (it.moveToFirst() && it.getColumnIndex(columnName) != -1) {
          it.get<String>(columnName)
        } else null
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

  @JvmStatic fun getCurrentSize(book: Book) =
    getAllZimParts(book).fold(0L, { acc, file -> acc + file.length() })
}
