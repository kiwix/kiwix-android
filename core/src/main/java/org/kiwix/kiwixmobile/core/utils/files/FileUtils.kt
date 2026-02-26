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
import android.net.Uri
import android.content.ContentUris
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.os.Build
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Base64
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.downloader.ChunkUtils
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.extensions.deleteFile
import org.kiwix.kiwixmobile.core.extensions.hasContent
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.getSDCardOrUSBMainPathForAndroid10AndAbove
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.getSdCardOrUSBMainPathForAndroid9AndBelow
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

object FileUtils {
  private val fileOperationMutex = Mutex()
  private const val SPELLING_DB_CACHED_DIRECTORY = "SpellingsDBCachedDir"
  private const val ASSET_LOADING_CACHED_DIRECTORY = "AssetLoadingCachedDir"

  @JvmStatic
  fun getSpellingDBDir(context: Context): File? {
    val baseCacheDir = if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
      context.externalCacheDir
    } else {
      context.cacheDir
    }

    val spellingDBCacheDir = File(baseCacheDir, SPELLING_DB_CACHED_DIRECTORY).apply {
      if (!exists()) mkdirs()
    }

    // Clean up any incomplete `.tmp` folders inside the spelling DB directory
    spellingDBCacheDir.listFiles()?.forEach { file ->
      if (file.isDirectory && file.name.endsWith(".tmp")) {
        file.deleteRecursively()
        Log.w(TAG_KIWIX, "Deleted incomplete SpellingsDB folder: ${file.absolutePath}")
      }
    }

    return spellingDBCacheDir
  }

  @JvmStatic
  fun getFileCacheDir(context: Context): File? {
    val baseCacheDir = if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
      context.externalCacheDir
    } else {
      context.cacheDir
    }
    return File(baseCacheDir, ASSET_LOADING_CACHED_DIRECTORY).apply {
      if (!exists()) mkdirs()
    }
  }

  @JvmStatic
  @Synchronized
  fun deleteCachedFiles(
    context: Context,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
  ) {
    CoroutineScope(dispatcher).launch {
      getFileCacheDir(context)?.deleteRecursively()
    }
  }

  @JvmStatic
  suspend fun deleteZimFile(path: String) {
    fileOperationMutex.withLock {
      var filePath = path
      if (filePath.substring(filePath.length - ChunkUtils.PART.length) == ChunkUtils.PART) {
        filePath = filePath.substring(0, filePath.length - ChunkUtils.PART.length)
      }
      val file = File(filePath)
      if (file.path.substring(file.path.length - 3) != "zim") {
        var alphabetFirst = 'a'
        fileloop@ while (alphabetFirst <= 'z') {
          var alphabetSecond = 'a'
          while (alphabetSecond <= 'z') {
            val chunkPath =
              filePath.substring(0, filePath.length - 2) + alphabetFirst + alphabetSecond
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
        deleteZimFileParts(filePath)
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
  suspend fun getLocalFilePathByUri(
    context: Context,
    uri: Uri,
    documentsContractWrapper: DocumentResolverWrapper = DocumentResolverWrapper()
  ): String? {
    Log.e(TAG_KIWIX, "Trying to get the ZIM file path for Uri = $uri")
    return when {
      DocumentsContract.isDocumentUri(context, uri) ->
        getProviderDocumentPath(context, uri, documentsContractWrapper)
      uri.scheme != null -> getUriSchemePath(context, uri, documentsContractWrapper)
      else -> uri.path
    }
  }

  private suspend fun getProviderDocumentPath(
    context: Context,
    uri: Uri,
    wrapper: DocumentResolverWrapper
  ): String? =
    when (uri.authority) {
      "com.android.externalstorage.documents" -> getExternalStorageDocumentPath(context, uri)
      "com.android.providers.downloads.documents" -> getDownloadsDocumentPath(context, uri, wrapper)
      else -> null
    }

  private fun getExternalStorageDocumentPath(context: Context, uri: Uri): String? {
    val documentId = DocumentsContract.getDocumentId(uri).split(":")
    if (documentId[0] == "primary") {
      return "${Environment.getExternalStorageDirectory()}/${documentId[1]}"
    }
    return try {
      val sdCardOrUsbMainPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getSDCardOrUSBMainPathForAndroid10AndAbove(context, documentId[0])
      } else {
        getSdCardOrUSBMainPathForAndroid9AndBelow(context, documentId[0])
      }
      "$sdCardOrUsbMainPath/${documentId[1]}"
    } catch (_: Exception) {
      null
    }
  }

  private suspend fun getDownloadsDocumentPath(
    context: Context,
    uri: Uri,
    wrapper: DocumentResolverWrapper
  ): String? =
    try {
      documentProviderContentQuery(context, uri, wrapper)
    } catch (_: IllegalArgumentException) {
      null
    }

  private suspend fun getUriSchemePath(
    context: Context,
    uri: Uri,
    wrapper: DocumentResolverWrapper
  ): String? =
    if ("content".equals(uri.scheme, ignoreCase = true)) {
      getFilePathOfContentUri(context, uri, wrapper)
    } else if ("file".equals(uri.scheme, ignoreCase = true)) {
      uri.path
    } else {
      uri.path
    }

  /**
   * Retrieves the main storage path for a given external storage device (SD card, USB stick, or external hard drive).
   *
   * @param context The application context.
   * @param storageName The name of the storage (e.g., "sdcard" or "usbstick").
   * @return The main storage path for the given storage name, or `null` if not found.
   *
   * This method leverages `getStorageVolumesList`, which directly provides the storage path
   * for USB and other mounted devices on Android 10 (API 29) and above.
   *
   * For Android 9 (API 28) and below, refer to `getSdCardOrUSBMainPath` for retrieving the USB path.
   *
   * @see getSdCardOrUSBMainPathForAndroid9AndBelow
   */
  @RequiresApi(Build.VERSION_CODES.Q)
  private fun getSDCardOrUSBMainPathForAndroid10AndAbove(context: Context, storageName: String) =
    getStorageVolumesList(context).firstOrNull { it.contains(storageName) }

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
  private suspend fun getFilePathOfContentUri(
    context: Context,
    uri: Uri,
    documentsContractWrapper: DocumentResolverWrapper = DocumentResolverWrapper()
  ): String? {
    val filePath = contentQuery(context, uri, documentsContractWrapper)
    return if (!filePath.isNullOrEmpty()) {
      filePath
    } else {
      // Fallback method to get the actual path of the URI
      getActualFilePathOfContentUri(context, uri, documentsContractWrapper)
    }
  }

  private suspend fun getFullFilePathFromFilePath(
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

  /**
   * Retrieves a list of storage volume paths available on the device.
   *
   * This method uses the `StorageManager` system service to obtain a list of storage volumes,
   * including internal storage, SD cards, and USB devices. The method accounts for API differences:
   * - On Android 11 (API 30) and above, it directly retrieves the storage path using `StorageVolume.directory`.
   * - On Android 10 (API 29) and below, it constructs the storage path based on the volume's UUID or description.
   *
   * @param context The application context used to access system services.
   * @return A `HashSet<String>` containing paths of available storage volumes.
   */
  private fun getStorageVolumesList(context: Context): HashSet<String> {
    val storageVolumes = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    val storageVolumesList = HashSet<String>()
    storageVolumes.storageVolumes.filterNotNull().forEach {
      storageVolumesList.add(getStoragePath(context, it))
    }
    return storageVolumesList
  }

  /**
   * Determines the appropriate storage path for a given volume.
   *
   * This method retrieves the storage path based on the Android version:
   * - **Android 11+ (API 30+)**: Directly retrieves the storage path from `StorageVolume.directory`.
   * - **Primary storage (Internal storage)**: Returns the path using `Environment.getExternalStorageDirectory()`.
   * - **External storage (SD card, USB, etc.)**:
   *   - If the volume has a UUID, constructs the path using `/storage/{UUID}/`.
   *   - If no UUID is available, falls back to using the volume description.
   *
   * @param context The application context used for accessing volume descriptions.
   * @param volume The `StorageVolume` whose path needs to be determined.
   * @return The storage path as a `String`.
   */
  private fun getStoragePath(context: Context, volume: StorageVolume): String {
    return when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
        // On Android 11 (API 30) and above, return the storage path directly.
        "${volume.directory?.path}"
      }

      volume.isPrimary -> {
        // If this is the primary internal storage, return the default external storage directory.
        "${Environment.getExternalStorageDirectory()}/"
      }

      else -> {
        // If this is an external storage device, construct the path using UUID or description.
        val externalStorageName =
          volume.uuid?.let { uuid ->
            "/$uuid/"
          } ?: "/${volume.getDescription(context)}/"

        // On Android 10 and below, external storage devices are mounted under `/storage`.
        "/storage$externalStorageName"
      }
    }
  }

  private fun getFileNameFromUri(
    context: Context,
    uri: Uri,
    wrapper: DocumentResolverWrapper = DocumentResolverWrapper()
  ): String? {
    return wrapper.query(context, uri, MediaStore.MediaColumns.DISPLAY_NAME, null, null, null)
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
  private suspend fun getActualFilePathOfContentUri(
    context: Context,
    uri: Uri,
    documentsContractWrapper: DocumentResolverWrapper = DocumentResolverWrapper()
  ): String? {
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
          "$DIRECTORY_DOWNLOADS/${getFileNameFromUri(context, uri, documentsContractWrapper)}"
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

  suspend fun documentProviderContentQuery(
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
    val contentUriPrefixes =
      arrayOf(
        "content://downloads/public_downloads",
        "content://downloads/my_downloads",
        "content://downloads/all_downloads"
      )
    val actualDocumentId =
      try {
        documentId.toLong()
      } catch (_: NumberFormatException) {
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
      val fileName = getFileNameFromUri(context, uri, documentsContractWrapper)
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
          ContentUris.withAppendedId(prefix.toUri(), documentId),
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
    } catch (_: IOException) {
      return ""
    }
  }

  @Suppress("NestedBlockDepth")
  @JvmStatic
  suspend fun getAllZimParts(book: LibkiwixBook): List<File> {
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
  suspend fun hasPart(file: File): Boolean {
    var tempFile = file
    tempFile = File(getFileName(tempFile.path))
    if (tempFile.path.endsWith(".zim")) {
      return false
    }
    if (tempFile.path.endsWith(".part")) {
      return true
    }
    val path = tempFile.path
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
  suspend fun getFileName(fileName: String) =
    when {
      File(fileName).isFileExist() -> fileName
      File("$fileName.part").isFileExist() -> "$fileName.part"
      else -> "${fileName}aa"
    }

  @JvmStatic
  fun Context.readFile(filePath: String): String =
    try {
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
   * Determines whether the given file path corresponds to a split ZIM file.
   *
   * A split ZIM file has an extension that starts with ".zima" followed by a single character,
   * such as ".zimaa", ".zimab", etc. This method checks if the file path ends with this specific pattern.
   *
   * @param filePath The file path to evaluate.
   * @return `true` if the file path corresponds to a split ZIM file, `false` otherwise.
   */
  @JvmStatic
  fun isSplittedZimFile(filePath: String): Boolean =
    filePath.matches(Regex(".*\\.zima.$"))

  /**
   * Get the main storage path for a given storage name (SD card or USB stick).
   *
   * @param context The application context.
   * @param storageName The name of the storage (e.g., "sdcard" or "usbstick").
   * @return The main storage path for the given storage name,
   *         or null if the path is a USB path on Android 10 and above
   *         (due to limitations in `context.getExternalFilesDirs("")` behavior).
   *
   *  To get the SD card or USB main path for Android 10 and above refer to:
   *  @See getSDCardOrUSBMainPathForAndroid10AndAbove
   */
  @JvmStatic
  fun getSdCardOrUSBMainPathForAndroid9AndBelow(context: Context, storageName: String) =
    context.getExternalFilesDirs("")
      .firstOrNull { it.path.contains(storageName) }
      ?.path?.substringBefore(context.getString(R.string.android_directory_seperator))

  private fun isBase64DataUri(src: String?): Boolean {
    return src?.startsWith("data:", ignoreCase = true) == true &&
      src.contains(";base64,", ignoreCase = true)
  }

  private fun generateBase64FileName(extension: String): String =
    "image_${System.currentTimeMillis()}.$extension"

  fun decodeBase64DataUri(src: String?): Pair<String, ByteArray>? {
    return try {
      val headerAndData = src?.split(",", limit = 2)
      if (headerAndData?.size != 2) return null

      val header = headerAndData[0]
      val base64Data = headerAndData[1]

      val mimeType = header
        .substringAfter("data:")
        .substringBefore(";")

      val extension = MimeTypeMap.getSingleton()
        .getExtensionFromMimeType(mimeType) ?: "bin"

      val bytes = Base64.decode(base64Data, Base64.DEFAULT)
      extension to bytes
    } catch (_: Exception) {
      null
    }
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

  suspend fun getDownloadRootDir(kiwixDataStore: KiwixDataStore): File? {
    return if (
      kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove() ||
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    ) {
      CoreApp.instance.externalMediaDirs.firstOrNull()
    } else {
      File(
        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/org.kiwix"
      ).apply {
        if (!exists()) mkdir()
      }
    }
  }

  @Suppress("ReturnCount", "NestedBlockDepth")
  @JvmStatic
  suspend fun downloadFileFromUrl(
    url: String?,
    src: String?,
    zimReaderContainer: ZimReaderContainer,
    kiwixDataStore: KiwixDataStore
  ): File? {
    val root = getDownloadRootDir(kiwixDataStore) ?: return null
    when {
      isBase64DataUri(src) -> {
        val decoded = decodeBase64DataUri(src) ?: return null
        val (extension, bytes) = decoded
        val file = File(root, generateBase64FileName(extension))

        return try {
          file.outputStream().use { it.write(bytes) }
          file
        } catch (e: IOException) {
          Log.w("kiwix", "Couldn't save base64 file", e)
          null
        }
      }

      else -> {
        val fileName = getSafeFileNameAndSourceFromUrlOrSrc(url, src)
        if (fileName?.first == null) return null
        val fileToSave = File(root, fileName.first)
        if (fileToSave.isFileExist()) return fileToSave
        return try {
          fileName.second?.let {
            zimReaderContainer.load(it, emptyMap()).data.use { inputStream ->
              fileToSave.outputStream().use(inputStream::copyTo)
            }
            if (fileToSave.hasContent()) fileToSave else null
          }
        } catch (e: IOException) {
          Log.w("kiwix", "Couldn't save file", e)
          null
        }
      }
    }
  }

  @JvmStatic
  fun getDemoFilePathForCustomApp(context: Context) =
    "${context.getExternalFilesDirs(null)[0]}/demo.zim"

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
    } catch (_: FileNotFoundException) {
      null
    } catch (_: Exception) {
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
