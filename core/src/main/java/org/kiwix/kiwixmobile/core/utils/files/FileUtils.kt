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
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.URLUtil
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.downloader.ChunkUtils
import org.kiwix.kiwixmobile.core.extensions.get
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import java.io.BufferedReader
import java.io.File
import java.io.IOException

object FileUtils {

  @JvmStatic
  fun getFileCacheDir(context: Context): File? =
    if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
      context.externalCacheDir
    } else {
      context.cacheDir
    }

  @JvmStatic
  @Synchronized
  fun deleteCachedFiles(context: Context) {
    getFileCacheDir(context)?.deleteRecursively()
  }

  @JvmStatic
  @Synchronized
  fun deleteZimFile(path: String) {
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

  @Synchronized
  private fun deleteZimFileParts(path: String): Boolean {
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

  @JvmStatic
  fun readLocalesFromAssets(context: Context) =
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

  @JvmStatic
  fun Context.readFile(filePath: String): String = try {
    assets.open(filePath)
      .bufferedReader()
      .use(BufferedReader::readText)
  } catch (e: IOException) {
    "".also { e.printStackTrace() }
  }

  @JvmStatic
  fun getSdCardMainPath(context: Context): String =
    "${context.getExternalFilesDirs("")[1]}"
      .substringBefore(context.getString(R.string.android_directory_seperator))

  @SuppressLint("WrongConstant")
  @JvmStatic
  fun getPathFromUri(activity: Activity, data: Intent): String? {
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

  /*
   * This method returns a file name guess from the url using URLUtils.guessFileName()
     method of android.webkit. which is using Uri.decode method to extract the filename
     from url. After that it splits filename between base and extension
     (e.g for DemoFile.png, DemoFile is base and png is extension).
     if there is no extension in url then it will automatically add the .bin extension to filename.

   * If it's failed to guess the file name then it will return default filename downloadfile.bin.
     If it returns this default value or containing the .bin in file name,
     then we are returning null from this function which is handled in downloadFileFromUrl method.

   * We are placing a condition here for if the file name does not have a .bin extension,
     then it returns the original file name.
   */
  fun getDecodedFileName(url: String?): String? {
    var fileName: String? = null
    val decodedFileName = URLUtil.guessFileName(url, null, null)
    if (!decodedFileName.endsWith(".bin")) {
      fileName = decodedFileName
    }
    return fileName
  }

  @Suppress("ReturnCount")
  @JvmStatic
  fun downloadFileFromUrl(
    url: String?,
    src: String?,
    zimReaderContainer: ZimReaderContainer,
    sharedPreferenceUtil: SharedPreferenceUtil
  ): File? {
    val fileName = getDecodedFileName(url ?: src) ?: return null
    var root: File? = null
    if (sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove() ||
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    ) {
      if (CoreApp.instance.externalMediaDirs.isNotEmpty()) {
        root = CoreApp.instance.externalMediaDirs[0]
      }
    } else {
      root =
        File(
          "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}" +
            "/org.kiwix"
        )
      if (!root.exists()) root.mkdir()
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
