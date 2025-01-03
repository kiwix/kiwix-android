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
package org.kiwix.kiwixmobile.core.reader

import android.annotation.SuppressLint
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.core.net.toUri
import eu.mhutti1.utils.storage.Kb
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.DarkModeConfig
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.main.UNINITIALISER_ADDRESS
import org.kiwix.kiwixmobile.core.main.UNINITIALISE_HTML
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_PREFIX
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.libkiwix.JNIKiwixException
import org.kiwix.libzim.Archive
import org.kiwix.libzim.DirectAccessInfo
import org.kiwix.libzim.Item
import org.kiwix.libzim.SuggestionSearch
import org.kiwix.libzim.SuggestionSearcher
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.URLDecoder
import javax.inject.Inject

private const val TAG = "ZimFileReader"

@Suppress("LongParameterList")
class ZimFileReader constructor(
  val zimReaderSource: ZimReaderSource,
  val jniKiwixReader: Archive,
  private val darkModeConfig: DarkModeConfig,
  private val searcher: SuggestionSearcher
) {
  interface Factory {
    suspend fun create(zimReaderSource: ZimReaderSource): ZimFileReader?

    class Impl @Inject constructor(private val darkModeConfig: DarkModeConfig) :
      Factory {
      override suspend fun create(zimReaderSource: ZimReaderSource): ZimFileReader? =
        withContext(Dispatchers.IO) { // Bug Fix #3805
          try {
            zimReaderSource.createArchive()?.let {
              ZimFileReader(
                zimReaderSource,
                darkModeConfig = darkModeConfig,
                jniKiwixReader = it,
                searcher = SuggestionSearcher(it)
              ).also {
                Log.e(TAG, "create: ${zimReaderSource.toDatabase()}")
              }
            } ?: kotlin.run {
              Log.e(
                TAG,
                "Error in creating ZimFileReader," +
                  " because file does not exist on path: ${zimReaderSource.toDatabase()}"
              )
              null
            }
          } catch (ignore: JNIKiwixException) {
            null
          } catch (ignore: Exception) { // for handing the error, if any zim file is corrupted
            null
          }
        }
    }
  }

  /**
   * Note that the value returned is NOT unique for each zim file. Versions of the same wiki
   * (complete, nopic, novid, etc) may return the same title.
   */
  val title: String
    get() = getSafeMetaData("Title", "No Title Found")
  val mainPage: String?
    get() =
      try {
        jniKiwixReader.mainEntry.getItem(true).path
      } catch (exception: Exception) {
        Log.e(TAG, "Unable to find the main page, original exception $exception")
        null
      }
  val id: String get() = jniKiwixReader.uuid

  /*
     libzim returns file size in kib so we need to convert it into bytes.
     More information here https://github.com/kiwix/java-libkiwix/issues/41
   */
  val fileSize: Long get() = jniKiwixReader.filesize / 1024
  val creator: String get() = getSafeMetaData("Creator", "")
  val publisher: String get() = getSafeMetaData("Publisher", "")
  val name: String get() = getSafeMetaData("Name", id)
  val date: String get() = getSafeMetaData("Date", "")
  val description: String get() = getSafeMetaData("Description", "")
  val favicon: String?
    get() = if (jniKiwixReader.hasIllustration(ILLUSTRATION_SIZE))
      Base64.encodeToString(
        jniKiwixReader.getIllustrationItem(ILLUSTRATION_SIZE).data.data,
        Base64.DEFAULT
      )
    else
      null
  val language: String get() = getSafeMetaData("Language", "")

  val tags: String
    get() = getSafeMetaData("Tags", "")
  val mediaCount: Int?
    get() = try {
      jniKiwixReader.mediaCount
    }
    // Catch all exceptions to prevent the rendering process of other zim files from aborting.
    // If the zim file is split with zim-tool,
    // refer to https://github.com/kiwix/kiwix-android/issues/3827.
    catch (ignore: Exception) {
      Log.e(TAG, "Unable to find the media count $ignore")
      null
    }
  val articleCount: Int?
    get() = try {
      jniKiwixReader.articleCount
    }
    // Catch all exceptions to prevent the rendering process of other zim files from aborting.
    // If the zim file is split with zim-tool,
    // refer to https://github.com/kiwix/kiwix-android/issues/3827.
    catch (ignore: Exception) {
      Log.e(TAG, "Unable to find the article count $ignore")
      null
    }

  fun searchSuggestions(prefix: String): SuggestionSearch? =
    try {
      searcher.suggest(prefix)
    } catch (exception: Exception) {
      // to handled the exception if there is no FT Xapian index found in the current zim file
      Log.e(TAG, "Unable to search in this file as it does not have FT Xapian index. $exception")
      null
    }

  fun getPageUrlFrom(title: String): String? =
    try {
      jniKiwixReader.getEntryByTitle(title).path
    } catch (exception: Exception) {
      Log.e(TAG, "Could not get path for title = $title \n original exception = $exception")
      null
    }

  fun getRandomArticleUrl(): String? =
    try {
      jniKiwixReader.randomEntry.path
    } catch (exception: Exception) {
      Log.e(TAG, "Could not get random entry \n original exception = $exception")
      null
    }

  @Suppress("UnreachableCode")
  suspend fun load(uri: String): InputStream? = withContext(Dispatchers.IO) {
    val extension = uri.substringAfterLast(".")
    if (assetExtensions.any { it == extension }) {
      try {
        return@withContext loadAsset(uri)
      } catch (ioException: IOException) {
        Log.e(TAG, "failed to write video for $uri", ioException)
      }
    }
    return@withContext loadContent(uri, extension)
  }

  @Suppress("UnreachableCode", "NestedBlockDepth", "ReturnCount")
  private fun loadContent(uri: String, extension: String): InputStream? {
    val item = getItem(uri)
    if (compressedExtensions.any { it != extension }) {
      item?.itemSize()?.let {
        // Check if the item size exceeds 1 MB
        if (it / Kb > 1024) {
          // Retrieve direct access information for the item
          val infoPair = getDirectAccessInfoOfItem(item, uri)
          val file = infoPair?.filename?.let(::File)
          // If no file found or file does not exist, return input stream from item data
          if (infoPair == null || file == null || !file.exists()) {
            return@loadContent ByteArrayInputStream(item.data?.data)
          }
          // Return the input stream from the direct access information
          return@loadContent getInputStreamFromDirectAccessInfo(item, file, infoPair)
        }
      }
    }
    return loadContent(item, uri)
  }

  fun getMimeTypeFromUrl(uri: String): String? = getItem(uri)?.mimetype
    ?.truncateMimeType.also {
      Log.d(TAG, "getting mimetype for $uri = $it")
    }

  fun getRedirect(url: String) = "${toRedirect(url)}"

  fun isRedirect(url: String) =
    when {
      getRedirect(url).isEmpty() || url.endsWith(UNINITIALISER_ADDRESS) -> false
      else -> url.startsWith(CONTENT_PREFIX) && url != getRedirect(url)
    }

  private fun toRedirect(url: String) =
    "$CONTENT_PREFIX${getActualUrl(url)}".toUri()

  private fun getActualUrl(url: String): String {
    val actualPath = url.toUri().filePath
    var redirectPath = try {
      jniKiwixReader.getEntryByPath(actualPath)
        .getItem(true)
        .path
    } catch (ignore: Exception) {
      actualPath
    }
    if (url.contains("?")) {
      redirectPath += extractQueryParam(url)
    }
    return redirectPath
  }

  // For fixing the issue with new created zim files,
  // see https://github.com/kiwix/kiwix-android/issues/3098#issuecomment-1642083152 to know more about the this.
  private fun extractQueryParam(url: String): String =
    "?" + url.substringAfterLast("?", "")

  private fun loadContent(item: Item?, uri: String) =
    try {
      val outputStream = PipedOutputStream()
      PipedInputStream(outputStream).also { streamZimContentToPipe(item, uri, outputStream) }
    } catch (ioException: IOException) {
      throw IOException("Could not open pipe for $uri", ioException)
    }

  private suspend fun loadAsset(uri: String): InputStream? = withContext(Dispatchers.IO) {
    val item = try {
      jniKiwixReader.getEntryByPath(uri.filePath).getItem(true)
    } catch (exception: Exception) {
      Log.e(TAG, "Could not get Item for uri = $uri \n original exception = $exception")
      null
    }
    val infoPair = getDirectAccessInfoOfItem(item, uri)
    val file = infoPair?.filename?.let(::File)
    if (infoPair == null || file == null || !file.exists()) {
      return@withContext loadAssetFromCache(uri)
    }
    return@withContext getInputStreamFromDirectAccessInfo(item, file, infoPair)
  }

  private fun getDirectAccessInfoOfItem(item: Item?, uri: String): DirectAccessInfo? =
    try {
      item?.directAccessInformation
    } catch (ignore: Exception) {
      Log.e(
        TAG,
        "Could not get directAccessInformation for uri = $uri \n" +
          "original exception = $ignore"
      )
      null
    }

  private fun getInputStreamFromDirectAccessInfo(
    item: Item?,
    file: File,
    infoPair: DirectAccessInfo
  ): InputStream? =
    item?.itemSize()?.let {
      AssetFileDescriptor(
        infoPair.parcelFileDescriptor(file),
        infoPair.offset,
        it
      ).createInputStream()
    }

  @Throws(IOException::class)
  private fun loadAssetFromCache(uri: String): FileInputStream {
    return File(
      FileUtils.getFileCacheDir(CoreApp.instance),
      uri.substringAfterLast("/")
    ).apply { getContent(uri)?.let(::writeBytes) }
      .inputStream()
  }

  private fun getContent(url: String) = try {
    getItem(url)?.data?.data
  } catch (ignore: Exception) {
    Log.e(TAG, "Could not get content for url = $url original exception = $ignore")
    null
  }

  @SuppressLint("CheckResult")
  private fun streamZimContentToPipe(item: Item?, uri: String, outputStream: OutputStream) {
    Completable.fromAction {
      try {
        outputStream.use {
          if (uri.endsWith(UNINITIALISER_ADDRESS)) {
            it.write(UNINITIALISE_HTML.toByteArray())
          } else {
            item?.let { item ->
              if ("text/css" == item.mimetype && darkModeConfig.isDarkModeActive()) {
                it.write(INVERT_IMAGES_VIDEO.toByteArray())
              }
              it.write(item.data.data)
            }
          }
        }
      } catch (ignore: Exception) {
        Log.e(TAG, "error writing pipe for $uri", ignore)
      }
    }.subscribeOn(Schedulers.io())
      .subscribe({ }, Throwable::printStackTrace)
  }

  fun getItem(url: String): Item? =
    try {
      val actualPath = url.toUri().filePath.decodeUrl
      jniKiwixReader.getEntryByPath(actualPath)
        .getItem(true)
    } catch (exception: Exception) {
      try {
        // check if we can get the article data when there are (#, ?, etc) any of these in the URL.
        // See #3924 for more details.
        jniKiwixReader.getEntryByPath(
          url.toUri().toString().substringAfter(CONTENT_PREFIX).decodeUrl
        )
          .getItem(true)
      } catch (exception: Exception) {
        Log.e(TAG, "Could not get Item for url = $url \n original exception = $exception")
        null
      }
    }

  @Suppress("ExplicitThis") // this@ZimFileReader.name is required
  fun toBook() = Book().apply {
    title = this@ZimFileReader.title
    id = this@ZimFileReader.id
    size = "$fileSize"
    favicon = this@ZimFileReader.favicon.toString()
    creator = this@ZimFileReader.creator
    publisher = this@ZimFileReader.publisher
    date = this@ZimFileReader.date
    description = this@ZimFileReader.description
    language = this@ZimFileReader.language
    articleCount = this@ZimFileReader.articleCount.toString()
    mediaCount = this@ZimFileReader.mediaCount.toString()
    bookName = this@ZimFileReader.name
    tags = this@ZimFileReader.tags
  }

  fun dispose() {
    jniKiwixReader.dispose()
    searcher.dispose()
  }

  @Suppress("TooGenericExceptionCaught")
  private fun getSafeMetaData(name: String, missingDelimiterValue: String) =
    try {
      jniKiwixReader.getMetadata(name)
    } catch (ignore: Exception) {
      missingDelimiterValue
    }

  companion object {
    /*
    * these uris aren't actually nullable but unit tests fail to compile as
    * Uri.parse returns null without android dependencies loaded
    */
    @JvmField
    val UI_URI: Uri? = Uri.parse("content://org.kiwix.ui/")

    const val CONTENT_PREFIX = "https://kiwix.app/"

    private val INVERT_IMAGES_VIDEO =
      """
        img, video, div[poster] { 
           -webkit-filter: invert(1); 
           filter: invert(1); 
        }
        div[poster] img, div[poster] video {
          -webkit-filter: invert(0); 
          filter: invert(0); 
        }
      """.trimIndent()
    private val assetExtensions =
      listOf("3gp", "mp4", "m4a", "webm", "mkv", "ogg", "ogv", "svg", "warc")
    private val compressedExtensions =
      listOf("zip", "7z", "gz", "rar", "sitx")
  }
}

private val Uri.filePath: String
  get() = toString().filePath
private val String.filePath: String
  get() = substringAfter(CONTENT_PREFIX).substringBefore("#").substringBefore("?")

// Decode the URL if it is encoded because libkiwix does not return the path for encoded paths.
val String.decodeUrl: String
  get() = try {
    URLDecoder.decode(this, "UTF-8")
  } catch (illegalArgumentException: IllegalArgumentException) {
    // Searched item already has the decoded URL,
    // and if any URL contains % in it, then it will fail to decode that URL and will not load the page.
    // e.g. https://kiwix.app/A/FT%, More info https://github.com/kiwix/kiwix-android/pull/3514
    Log.e(
      "ZimFileReader",
      "Could not decode url $this \n original exception = $illegalArgumentException"
    )
    this
  }

// Truncate mime-type (everything after the first space and semi-colon(if exists)
val String.truncateMimeType: String
  get() = replace("^([^ ]+).*$", "$1").substringBefore(";")

// Encode question mark with %3F after getting url from checkUrl() method
// for issue https://github.com/kiwix/kiwix-android/issues/2671
val String.replaceWithEncodedString: String
  get() = replace("?", "%3F")

private fun DirectAccessInfo.parcelFileDescriptor(file: File): ParcelFileDescriptor? =
  ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

// Default illustration size for ZIM file favicons
const val ILLUSTRATION_SIZE = 48

// add content prefix to url since searched items return the url inside of zim without content prefix.
val String.addContentPrefix: String
  get() = if (startsWith(CONTENT_PREFIX)) this else CONTENT_PREFIX + this

/**
 * Handles any error thrown by this method. Developers should handle the flow if this method
 * returns null. For more details, see: https://github.com/kiwix/kiwix-android/issues/4157
 */
fun Item.itemSize(): Long? = try {
  size
} catch (ignore: Exception) {
  Log.e(TAG, "Could not retrieve the item size.\n Original exception: $ignore")
  null
}
