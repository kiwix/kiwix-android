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
import android.util.Log
import androidx.core.net.toUri
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixlib.DirectAccessInfo
import org.kiwix.kiwixlib.JNIKiwixException
import org.kiwix.kiwixlib.JNIKiwixInt
import org.kiwix.kiwixlib.JNIKiwixReader
import org.kiwix.kiwixlib.JNIKiwixString
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.NightModeConfig
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.main.UNINITIALISER_ADDRESS
import org.kiwix.kiwixmobile.core.main.UNINITIALISE_HTML
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_PREFIX
import org.kiwix.kiwixmobile.core.search.SearchSuggestion
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import javax.inject.Inject

private const val TAG = "ZimFileReader"

class ZimFileReader constructor(
  val uri: String,
  val fd: ParcelFileDescriptor,
  private val jniKiwixReader: JNIKiwixReader = JNIKiwixReader(fd.fileDescriptor),
  private val nightModeConfig: NightModeConfig
) {
  interface Factory {
    fun create(uri: String, fd: ParcelFileDescriptor): ZimFileReader?

    class Impl @Inject constructor(private val nightModeConfig: NightModeConfig) :
      Factory {
      override fun create(uri: String, fd: ParcelFileDescriptor) =
        try {
          ZimFileReader(uri, fd, nightModeConfig = nightModeConfig)
        } catch (ignore: JNIKiwixException) {
          null
        }
    }
  }

  /**
   * Note that the value returned is NOT unique for each zim file. Versions of the same wiki
   * (complete, nopic, novid, etc) may return the same title.
   */
  val title: String get() = jniKiwixReader.title ?: "No Title Found"
  val mainPage: String get() = jniKiwixReader.mainPage
  val id: String get() = jniKiwixReader.id
  val fileSize: Int get() = jniKiwixReader.fileSize
  val creator: String get() = jniKiwixReader.creator
  val publisher: String get() = jniKiwixReader.publisher
  val name: String get() = jniKiwixReader.name?.takeIf(String::isNotEmpty) ?: id
  val date: String get() = jniKiwixReader.date
  val description: String get() = jniKiwixReader.description
  val favicon: String? get() = jniKiwixReader.favicon
  val language: String get() = jniKiwixReader.language
  val tags: String get() = "${getContent("M/Tags")}"
  private val mediaCount: Int?
    get() = try {
      jniKiwixReader.mediaCount
    } catch (ignore: UnsatisfiedLinkError) {
      null
    }
  private val articleCount: Int?
    get() = try {
      jniKiwixReader.articleCount
    } catch (ignore: UnsatisfiedLinkError) {
      null
    }

  fun searchSuggestions(prefix: String, count: Int) =
    jniKiwixReader.searchSuggestions(prefix, count)

  fun getNextSuggestion(): SearchSuggestion? {
    val title = JNIKiwixString()
    val url = JNIKiwixString()

    return if (jniKiwixReader.getNextSuggestion(title, url))
      SearchSuggestion(title.value, url.value)
    else null
  }

  fun getPageUrlFrom(title: String): String? =
    valueOfJniStringAfter { jniKiwixReader.getPageUrlFromTitle(title, it) }

  fun getRandomArticleUrl(): String? =
    valueOfJniStringAfter(jniKiwixReader::getRandomPage)

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

  fun readContentAndMimeType(uri: String): String = getContentAndMimeType(uri)
    .second.truncateMimeType.also {
      Log.d(TAG, "getting mimetype for $uri = $it")
    }

  fun getRedirect(url: String) = "${toRedirect(url)}"

  fun isRedirect(url: String) =
    when {
      getRedirect(url).isEmpty() || url.endsWith(UNINITIALISER_ADDRESS) -> false
      else -> url.startsWith(CONTENT_PREFIX) && url != getRedirect(url)
    }

  private fun toRedirect(url: String) =
    "$CONTENT_PREFIX${jniKiwixReader.checkUrl(url.toUri().filePath)}".toUri()

  private fun loadContent(uri: String) =
    try {
      val outputStream = PipedOutputStream()
      PipedInputStream(outputStream).also { streamZimContentToPipe(uri, outputStream) }
    } catch (ioException: IOException) {
      throw IOException("Could not open pipe for $uri", ioException)
    }

  private fun loadAsset(uri: String): InputStream? {
    val infoPair = jniKiwixReader.getDirectAccessInformation(uri.filePath)
    if (infoPair == null || !File(infoPair.filename).exists()) {
      return loadAssetFromCache(uri)
    }
    return AssetFileDescriptor(
      infoPair.parcelFileDescriptor,
      infoPair.offset,
      jniKiwixReader.getArticleSize(uri.filePath)
    ).createInputStream()
  }

  @Throws(IOException::class)
  private fun loadAssetFromCache(uri: String): FileInputStream {
    return File(
      FileUtils.getFileCacheDir(CoreApp.instance),
      uri.substringAfterLast("/")
    ).apply { writeBytes(getContent(uri)) }
      .inputStream()
  }

  private fun getContent(url: String) = getContentAndMimeType(url).let { (content, _) -> content }

  @SuppressLint("CheckResult")
  private fun streamZimContentToPipe(uri: String, outputStream: OutputStream) {
    Completable.fromAction {
      try {
        outputStream.use {
          if (uri.endsWith(UNINITIALISER_ADDRESS)) {
            it.write(UNINITIALISE_HTML.toByteArray())
          } else {
            getContentAndMimeType(uri).let { (content: ByteArray, mimeType: String) ->
              if ("text/css" == mimeType && nightModeConfig.isNightModeActive()) {
                it.write(INVERT_IMAGES_VIDEO.toByteArray())
              }
              it.write(content)
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

  private fun getContentAndMimeType(uri: String) = with(JNIKiwixString()) {
    getContent(url = JNIKiwixString(uri.filePath), mime = this) to value
  }

  private fun getContent(
    url: JNIKiwixString = JNIKiwixString(),
    jniKiwixString: JNIKiwixString = JNIKiwixString(),
    mime: JNIKiwixString = JNIKiwixString(),
    size: JNIKiwixInt = JNIKiwixInt()
  ) = jniKiwixReader.getContent(url, jniKiwixString, mime, size).also {
    Log.d(TAG, "reading  ${url.value}(mime: ${mime.value}, size: ${size.value}) finished.")
  }

  private fun valueOfJniStringAfter(jniStringFunction: (JNIKiwixString) -> Boolean) =
    JNIKiwixString().takeIf { jniStringFunction(it) }?.value

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

// Truncate mime-type (everything after the first space and semi-colon(if exists)
val String.truncateMimeType: String
  get() = replace("^([^ ]+).*$", "$1").substringBefore(";")

private val DirectAccessInfo.parcelFileDescriptor: ParcelFileDescriptor?
  get() = ParcelFileDescriptor.open(File(filename), ParcelFileDescriptor.MODE_READ_ONLY)
