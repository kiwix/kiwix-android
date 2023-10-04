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
import android.util.Log
import androidx.core.net.toUri
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.NightModeConfig
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.main.UNINITIALISER_ADDRESS
import org.kiwix.kiwixmobile.core.main.UNINITIALISE_HTML
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_PREFIX
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.libkiwix.JNIKiwixException
import org.kiwix.libzim.Archive
import org.kiwix.libzim.DirectAccessInfo
import org.kiwix.libzim.Item
import org.kiwix.libzim.SuggestionSearch
import org.kiwix.libzim.SuggestionSearcher
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

class ZimFileReader constructor(
  val zimFile: File,
  private val jniKiwixReader: Archive,
  private val nightModeConfig: NightModeConfig,
  private val searcher: SuggestionSearcher = SuggestionSearcher(jniKiwixReader)
) {
  interface Factory {
    fun create(file: File): ZimFileReader?

    class Impl @Inject constructor(private val nightModeConfig: NightModeConfig) :
      Factory {
      override fun create(file: File) =
        try {
          ZimFileReader(
            file,
            nightModeConfig = nightModeConfig,
            jniKiwixReader = Archive(file.canonicalPath)
          ).also {
            Log.e(TAG, "create: ${file.path}")
          }
        } catch (ignore: JNIKiwixException) {
          null
        } catch (ignore: Exception) { // for handing the error, if any zim file is corrupted
          null
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
  private val mediaCount: Int?
    get() = try {
      jniKiwixReader.mediaCount
    } catch (unsatisfiedLinkError: UnsatisfiedLinkError) {
      Log.e(TAG, "Unable to find the media count $unsatisfiedLinkError")
      null
    }
  private val articleCount: Int?
    get() = try {
      jniKiwixReader.articleCount
    } catch (unsatisfiedLinkError: UnsatisfiedLinkError) {
      Log.e(TAG, "Unable to find the article count $unsatisfiedLinkError")
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

  fun getRandomArticleUrl(): String? = jniKiwixReader.randomEntry.path

  fun load(uri: String): InputStream? {
    val extension = uri.substringAfterLast(".")
    if (assetExtensions.any { it == extension }) {
      try {
        return loadAsset(uri)
      } catch (ioException: IOException) {
        Log.e(TAG, "failed to write video for $uri", ioException)
      }
    }
    return loadContent(uri)
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
    "$CONTENT_PREFIX${getActualUrl(url, true)}".toUri()

  private fun getActualUrl(url: String, actualUrl: Boolean = false): String {
    val actualPath = url.toUri().filePath.decodeUrl
    var redirectPath = try {
      jniKiwixReader.getEntryByPath(actualPath)
        .getItem(true)
        .path
        .replaceWithEncodedString
    } catch (ignore: Exception) {
      actualPath.replaceWithEncodedString
    }
    if (actualUrl && url.decodeUrl.contains("?")) {
      redirectPath += extractQueryParam(url)
    }
    return redirectPath
  }

  // For fixing the issue with new created zim files,
  // see https://github.com/kiwix/kiwix-android/issues/3098#issuecomment-1642083152 to know more about the this.
  private fun extractQueryParam(url: String): String =
    "?" + url.substringAfterLast("?", "")

  private fun loadContent(uri: String) =
    try {
      val outputStream = PipedOutputStream()
      PipedInputStream(outputStream).also { streamZimContentToPipe(uri, outputStream) }
    } catch (ioException: IOException) {
      throw IOException("Could not open pipe for $uri", ioException)
    }

  private fun loadAsset(uri: String): InputStream? {
    val article = try {
      jniKiwixReader.getEntryByPath(uri.filePath).getItem(true)
    } catch (exception: Exception) {
      Log.e(TAG, "Could not get Item for uri = $uri \n original exception = $exception")
      null
    }
    val infoPair = article?.directAccessInformation
    if (infoPair == null || !File(infoPair.filename).exists()) {
      return loadAssetFromCache(uri)
    }
    return AssetFileDescriptor(
      infoPair.parcelFileDescriptor,
      infoPair.offset,
      article.size
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

  private fun getContent(url: String) = getItem(url)?.data?.data

  @SuppressLint("CheckResult")
  private fun streamZimContentToPipe(uri: String, outputStream: OutputStream) {
    Completable.fromAction {
      try {
        outputStream.use {
          if (uri.endsWith(UNINITIALISER_ADDRESS)) {
            it.write(UNINITIALISE_HTML.toByteArray())
          } else {
            getItem(uri)?.let { item ->
              if ("text/css" == item.mimetype && nightModeConfig.isNightModeActive()) {
                it.write(INVERT_IMAGES_VIDEO.toByteArray())
              }
              it.write(item.data.data)
            }
          }
        }
      } catch (ioException: IOException) {
        Log.e(TAG, "error writing pipe for $uri", ioException)
      }
    }
      .subscribeOn(Schedulers.io())
      .subscribe({ }, Throwable::printStackTrace)
  }

  private fun getItem(url: String): Item? =
    try {
      jniKiwixReader.getEntryByPath(getActualUrl(url)).getItem(true)
    } catch (exception: Exception) {
      Log.e(TAG, "Could not get Item for url = $url \n original exception = $exception")
      null
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
        img#header-profile{
          -webkit-filter: invert(0); 
          filter: invert(0); 
        }
        div[poster] > video {
          -webkit-filter: invert(0); 
          filter: invert(0); 
        }
      """.trimIndent()
    private val assetExtensions =
      listOf("3gp", "mp4", "m4a", "webm", "mkv", "ogg", "ogv", "svg", "warc")
  }
}

private val Uri.filePath: String
  get() = toString().filePath
private val String.filePath: String
  get() = substringAfter(CONTENT_PREFIX).substringBefore("#").substringBefore("?")

// Decode the URL if it is encoded because libkiwix does not return the path for encoded paths.
val String.decodeUrl: String
  get() = URLDecoder.decode(this, "UTF-8")

// Truncate mime-type (everything after the first space and semi-colon(if exists)
val String.truncateMimeType: String
  get() = replace("^([^ ]+).*$", "$1").substringBefore(";")

// Encode question mark with %3F after getting url from checkUrl() method
// for issue https://github.com/kiwix/kiwix-android/issues/2671
val String.replaceWithEncodedString: String
  get() = replace("?", "%3F")

private val DirectAccessInfo.parcelFileDescriptor: ParcelFileDescriptor?
  get() = ParcelFileDescriptor.open(File(filename), ParcelFileDescriptor.MODE_READ_ONLY)

// Default illustration size for ZIM file favicons
const val ILLUSTRATION_SIZE = 48
