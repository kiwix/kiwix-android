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
import android.content.ContentUris
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.webkit.URLUtil
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.downloader.ChunkUtils
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.extensions.deleteFile
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

object FileUtils {

  private val fileOperationMutex = Mutex()

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
    CoroutineScope(Dispatchers.IO).launch {
      getFileCacheDir(context)?.deleteRecursively()
    }
  }

  @JvmStatic
  suspend fun deleteZimFile(path: String) {
    fileOperationMutex.withLock {
      var path = path
      if (path.substring(path.length - ChunkUtils.PART.length) == ChunkUtils.PART) {
        path = path.substring(0, path.length - ChunkUtils.PART.length)
      }
      val file = File(path)
      if (file.path.substring(file.path.length - 3) != "zim") {
        var alphabetFirst = 'a'
        fileloop@ while (alphabetFirst <= 'z') {
          var alphabetSecond = 'a'
          while (alphabetSecond <= 'z') {
            val chunkPath = path.substring(0, path.length - 2) + alphabetFirst + alphabetSecond
            val fileChunk = File(chunkPath)
            if (fileChunk.isFileExist()) {
              fileChunk.deleteFile()
            } else if (!deleteZimFileParts(chunkPath)) {
              break@fileloop
            }
            alphabetSecond++
          }
          alphabetFirst++
        }
      } else {
        file.deleteFile()
        deleteZimFileParts(path)
      }
    }
  }

  @Suppress("ReturnCount")
  private suspend fun deleteZimFileParts(path: String): Boolean {
    val file = File(path + ChunkUtils.PART)
    if (file.isFileExist()) {
      file.deleteFile()
      return true
    }
    val singlePart = File("$path.part")
    if (singlePart.isFileExist()) {
      singlePart.deleteFile()
      return true
    }
    return false
  }

  @JvmStatic
  fun getLocalFilePathByUri(
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
          var sdCardOrUsbMainPath = getSdCardOrUSBMainPath(context, documentId[0])
          if (sdCardOrUsbMainPath == null) {
            // USB sticks are mounted under the `/mnt/media_rw` directory.
            sdCardOrUsbMainPath = "/mnt/media_rw/${documentId[0]}"
          }
          "$sdCardOrUsbMainPath/${documentId[1]}"
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
      if ("content".equals(uri.scheme, ignoreCase = true)) {
        return contentQuery(context, uri)
      } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        return uri.path
      }
    } else {
      return uri.path
    }

    return null
  }

  fun documentProviderContentQuery(
    context: Context,
    uri: Uri,
    documentsContractWrapper: DocumentResolverWrapper = DocumentResolverWrapper()
  ): String? {
    // Extracting the document ID from the URI.
    val documentId = extractDocumentId(uri, documentsContractWrapper)

    // Attempt to handle cases where the document ID is a direct path to a ZIM file.
    if (isValidZimFile(documentId)) {
      return documentId.substringAfter("raw:")
    }

    // Try different content URI prefixes in some case download content prefix is different.
    val contentUriPrefixes = arrayOf(
      "content://downloads/public_downloads",
      "content://downloads/my_downloads",
      "content://downloads/all_downloads"
    )
    val actualDocumentId = try {
      documentId.toLong()
    } catch (ignore: NumberFormatException) {
      0L
    }
    return queryForActualPath(
      context,
      actualDocumentId,
      contentUriPrefixes,
      documentsContractWrapper
    )
  }

  private fun queryForActualPath(
    context: Context,
    documentId: Long,
    contentUriPrefixes: Array<String>,
    documentsContractWrapper: DocumentResolverWrapper
  ): String? {
    try {
      for (prefix in contentUriPrefixes) {
        contentQuery(
          context,
          ContentUris.withAppendedId(Uri.parse(prefix), documentId),
          documentsContractWrapper
        )?.let {
          return@queryForActualPath it
        }
      }
    } catch (ignore: Exception) {
      Log.e(
        "kiwix",
        "Error in getting path for documentId = $documentId \nException = $ignore"
      )
    }

    return null
  }

  fun extractDocumentId(
    uri: Uri,
    documentsContractWrapper: DocumentResolverWrapper
  ): String {
    try {
      return documentsContractWrapper.getDocumentId(uri)
    } catch (ignore: Exception) {
      Log.e(
        "kiwix",
        "Unable to get documentId for uri = $uri \nException = $ignore"
      )
    }
    return ""
  }

  private fun contentQuery(
    context: Context,
    uri: Uri,
    documentsContractWrapper: DocumentResolverWrapper = DocumentResolverWrapper()
  ): String? {
    val columnName = "_data"
    return try {
      documentsContractWrapper.query(
        context,
        uri,
        columnName,
        null,
        null,
        null
      )
    } catch (ignore: Exception) {
      Log.e(
        "kiwix",
        "Could not get path for uri = $uri \nException = $ignore"
      )
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

  @Suppress("NestedBlockDepth")
  @JvmStatic fun getAllZimParts(book: Book): List<File> {
    val files = ArrayList<File>()
    book.file?.let {
      if (it.path.endsWith(".zim") || it.path.endsWith(".zim.part")) {
        if (it.isFileExist()) {
          files.add(it)
        } else {
          files.add(File("$it.part"))
        }
      } else {
        var path = it.path
        for (firstCharacter in 'a'..'z') {
          for (secondCharacter in 'a'..'z') {
            path = path.substring(0, path.length - 2) + firstCharacter + secondCharacter
            when {
              File(path).isFileExist() -> files.add(File(path))
              File("$path.part").isFileExist() -> files.add(File("$path.part"))
              else -> return@getAllZimParts files
            }
          }
        }
      }
    }
    return files
  }

  @JvmStatic
  fun hasPart(file: File): Boolean {
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
        if (fileChunk.isFileExist()) {
          return true
        } else if (!File(chunkPath).isFileExist()) {
          return false
        }
      }
    }
    return false
  }

  @JvmStatic
  fun getFileName(fileName: String) =
    when {
      File(fileName).isFileExist() -> fileName
      File("$fileName.part").isFileExist() -> "$fileName.part"
      else -> "${fileName}aa"
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
  fun isValidZimFile(filePath: String): Boolean =
    filePath.endsWith(".zim") || filePath.endsWith(".zimaa")

  /**
   * Get the main storage path for a given storage name (SD card or USB stick).
   *
   * @param context The application context.
   * @param storageName The name of the storage (e.g., "sdcard" or "usbstick").
   * @return The main storage path for the given storage name,
   *         or null if the path is a USB path on Android 10 and above
   *         (due to limitations in `context.getExternalFilesDirs("")` behavior).
   */
  @JvmStatic
  fun getSdCardOrUSBMainPath(context: Context, storageName: String) =
    context.getExternalFilesDirs("")
      .firstOrNull { it.path.contains(storageName) }
      ?.path?.substringBefore(context.getString(R.string.android_directory_seperator))

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
   * Remove colon if any contains in the fileName since most of the fileSystem
     will not allow to create file which contains colon in it.
     see https://github.com/kiwix/kiwix-android/issues/3737
   */
  private fun getDecodedFileName(url: String?): String? {
    var fileName: String? = null
    val decodedFileName = URLUtil.guessFileName(url, null, null)
    if (!decodedFileName.endsWith(".bin")) {
      fileName = decodedFileName.replace(":", "")
    }
    return fileName
  }

  fun getSafeFileNameAndSourceFromUrlOrSrc(url: String?, src: String?): Pair<String?, String?>? {
    var fileNameAndSource: Pair<String?, String?>? = null
    if (url != null) {
      fileNameAndSource = getDecodedFileName(url) to url
    }
    if (src != null && fileNameAndSource?.first.isNullOrEmpty()) {
      fileNameAndSource = getDecodedFileName(src) to src
    }
    return fileNameAndSource
  }

  @Suppress("ReturnCount")
  @JvmStatic
  fun downloadFileFromUrl(
    url: String?,
    src: String?,
    zimReaderContainer: ZimReaderContainer,
    sharedPreferenceUtil: SharedPreferenceUtil
  ): File? {
    val fileName = getSafeFileNameAndSourceFromUrlOrSrc(url, src) ?: return null
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
      if (!root.isFileExist()) root.mkdir()
    }
    val fileToSave = File(root, fileName.first)
    if (fileToSave.isFileExist()) return fileToSave
    return try {
      fileName.second?.let {
        zimReaderContainer.load(it, emptyMap()).data.use { inputStream ->
          fileToSave.outputStream().use(inputStream::copyTo)
        }
        fileToSave
      }
    } catch (e: IOException) {
      Log.w("kiwix", "Couldn't save file", e)
      null
    }
  }

  @JvmStatic
  fun getDemoFilePathForCustomApp(context: Context) =
    "${ContextCompat.getExternalFilesDirs(context, null)[0]}/demo.zim"

  @SuppressLint("Recycle")
  @JvmStatic
  fun getAssetFileDescriptorFromUri(
    context: Context,
    uri: Uri
  ): List<AssetFileDescriptor>? {
    return try {
      val assetFileDescriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
      // Verify whether libkiwix can successfully open this file descriptor or not.
      return if (
        isFileDescriptorCanOpenWithLibkiwix(assetFileDescriptor?.parcelFileDescriptor?.fd)
      ) {
        assetFileDescriptor?.let(::listOf)
      } else {
        null
      }
    } catch (ignore: FileNotFoundException) {
      null
    } catch (ignore: Exception) {
      // It may throw a SecurityException in the Play Store variant
      // since we have limited access to storage and URIs in the Play Store variant.
      // If the user opens the ZIM file via app linking and closes the application,
      // the next time they try to open that ZIM file, we won't have access to this URI.
      null
    }
  }

  @JvmStatic
  fun isFileDescriptorCanOpenWithLibkiwix(fdNumber: Int?): Boolean {
    return try {
      // Attempt to create a FileInputStream object using the specified path.
      // Since libkiwix utilizes this path to create the archive object internally,
      // it is crucial to verify if we can successfully read the file descriptor (fd)
      // via the given file path before passing it to libkiwix.
      // This precaution helps prevent runtime crashes.
      // For more details, refer to https://github.com/kiwix/kiwix-android/pull/3636.
      FileInputStream("dev/fd/$fdNumber")
      true
    } catch (ignore: Exception) {
      ignore.printStackTrace()
      false
    }
  }
}
