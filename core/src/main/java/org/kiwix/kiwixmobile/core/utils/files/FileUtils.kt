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
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.MediaStore
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
        return getFilePathOfContentUri(context, uri)
      } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        return uri.path
      }
    } else {
      return uri.path
    }

    return null
  }

  /**
   * Retrieves the file path from a given content URI. This method first attempts to get the path
   * using the content resolver (via `contentQuery`). If that returns null or empty, it falls back
   * to a secondary method to resolve the actual path.
   *
   * This fallback is especially necessary when:
   * 1. The user clicks directly on a downloaded file from browsers, where different browsers
   *    return URIs using their own file providers.
   * 2. On devices below Android 11, when files are clicked directly in the file manager, the content
   *    resolver may not be able to retrieve the path for certain URIs.
   */
  private fun getFilePathOfContentUri(context: Context, uri: Uri): String? {
    val filePath = contentQuery(context, uri)
    return if (!filePath.isNullOrEmpty()) {
      filePath
    } else {
      // Fallback method to get the actual path of the URI
      getActualFilePathOfContentUri(context, uri)
    }
  }

  private fun getFullFilePathFromFilePath(
    context: Context,
    filePath: String?
  ): String? {
    var actualFilePath: String? = null
    if (filePath?.isNotEmpty() == true) {
      getStorageVolumesList(context).forEach { volume ->
        // Check if the volume is part of the file path and remove it
        val trimmedFilePath = filePath.removePrefix(volume)
        val file = File("$volume/$trimmedFilePath")
        if (file.isFileExist()) {
          actualFilePath = file.path
        }
      }
    }
    return actualFilePath
  }

  private fun getStorageVolumesList(context: Context): HashSet<String> {
    val storageVolumes = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    val storageVolumesList = HashSet<String>()
    storageVolumes.storageVolumes.filterNotNull().forEach {
      if (it.isPrimary) {
        storageVolumesList.add("${Environment.getExternalStorageDirectory()}/")
      } else {
        val externalStorageName = it.uuid?.let { uuid ->
          "/$uuid/"
        } ?: kotlin.run {
          "/${it.getDescription(context)}/"
        }
        storageVolumesList.add("/storage$externalStorageName")
      }
    }
    return storageVolumesList
  }

  private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var cursor: Cursor? = null
    val projection = arrayOf(
      MediaStore.MediaColumns.DISPLAY_NAME
    )
    return try {
      cursor = context.contentResolver.query(
        uri, projection, null, null,
        null
      )
      if (cursor != null && cursor.moveToFirst()) {
        val index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        cursor.getString(index)
      } else {
        null
      }
    } catch (ignore: Exception) {
      null
    } finally {
      cursor?.close()
    }
  }

  /**
   * Retrieves the actual file path from a given content URI. This method handles various cases based on
   * the type of URI and the source (file managers, browser downloads, etc.).
   *
   * 1. For file managers that include the full file path in the URI (common in devices below Android 11),
   *    it triggers when the user clicks directly on the ZIM file in the file manager. The file manager may
   *    return the path with their own file provider, and we extract the path.
   *
   * 2. For URIs from the download provider (e.g., when opening files directly from browsers), this method
   *    constructs the full path using the `DIRECTORY_DOWNLOADS` directory and the file name.
   *
   * 3. For other URIs, it attempts to resolve the full file path from the provided URI using a custom
   *    method to retrieve the folder and file path.
   */
  private fun getActualFilePathOfContentUri(context: Context, uri: Uri): String? {
    return when {
      // For file managers that provide the full path in the URI (common on devices below Android 11).
      // This triggers when the user clicks directly on a ZIM file in the file manager, and the file
      // manager returns the path via its own file provider.
      "$uri".contains("root") && "$uri".endsWith("zim") -> {
        "$uri".substringAfter("/root")
      }

      // Handles URIs from the download provider, commonly used when files are opened from browsers.
      // Some browsers return URIs with their DownloadProvider.
      isDownloadProviderUri(uri) -> {
        getFullFilePathFromFilePath(
          context,
          "$DIRECTORY_DOWNLOADS/${getFileNameFromUri(context, uri)}"
        )
      }

      else -> {
        // Attempts to retrieve the full path from the URI using a custom method.
        getFullFilePathFromFilePath(
          context,
          getFilePathWithFolderFromUri(uri)
        )
      }
    }
  }

  private fun getFilePathWithFolderFromUri(uri: Uri): String? {
    val pathSegments = uri.pathSegments
    if (pathSegments.isNotEmpty()) {
      // Returns the path of the folder containing the file with the specified fileName,
      // from which the user selects the file.
      return pathSegments.drop(1)
        .filterNot { it.startsWith("0") } // remove the prefix of primary storage device
        .joinToString(separator = "/")
    }
    return null
  }

  private fun isDownloadProviderUri(uri: Uri): Boolean =
    "$uri".contains("DownloadProvider") || "$uri".contains("/downloads")

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
    ) ?: kotlin.run {
      // Fallback method to get the actual path of the URI. This will be called
      // when queryForActualPath returns null, especially in cases where the user directly opens
      // the file from the file manager in the downloads folder, and the URI contains a different
      // document ID (particularly on tablets). See https://github.com/kiwix/kiwix-android/issues/4008
      val fileName = getFileNameFromUri(context, uri)
      getFullFilePathFromFilePath(context, "$DIRECTORY_DOWNLOADS/$fileName")
    }
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
