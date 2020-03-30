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

import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixlib.JNIKiwixException
import org.kiwix.kiwixlib.JNIKiwixInt
import org.kiwix.kiwixlib.JNIKiwixReader
import org.kiwix.kiwixlib.JNIKiwixString
import org.kiwix.kiwixlib.Pair
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.NightModeConfig
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_URI
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
  val zimFile: File,
  val jniKiwixReader: JNIKiwixReader = JNIKiwixReader(zimFile.canonicalPath),
  private val nightModeConfig: NightModeConfig
) {
  interface Factory {
    fun create(file: File): ZimFileReader?

    class Impl @Inject constructor(private val nightModeConfig: NightModeConfig) :
      Factory {
      override fun create(file: File) =
        try {
          ZimFileReader(file, nightModeConfig = nightModeConfig)
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
  val tags: String get() = "${getContentAndMimeType("M/Tags")}"
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
    if (uri.matches(VIDEO_REGEX)) {
      try {
        return loadVideo(uri)
      } catch (ioException: IOException) {
        Log.e(TAG, "failed to write video for $uri", ioException)
      }
    }
    return loadContent(uri)
  }

  fun readMimeType(uri: String) = uri.removeArguments().let {
    it.mimeType?.takeIf(String::isNotEmpty) ?: mimeTypeFromReader(it)
  }.also { Log.d(TAG, "getting mimetype for $uri = $it") }

  private fun mimeTypeFromReader(it: String) = jniKiwixReader.getMimeType(it.filePath)
    // Truncate mime-type (everything after the first space
    .replace("^([^ ]+).*$", "$1")

  fun getRedirect(url: String) = "${toRedirect(url)}"

  fun isRedirect(url: String) =
    url.startsWith("$CONTENT_URI") && url != getRedirect(url)

  private fun toRedirect(url: String) =
    "$CONTENT_URI${jniKiwixReader.checkUrl(url.toUri().filePath)}".toUri()

  private fun loadContent(uri: String) =
    try {
      PipedInputStream(PipedOutputStream().also { streamZimContentToPipe(uri, it) })
    } catch (ioException: IOException) {
      throw IOException("Could not open pipe for $uri", ioException)
    }

  private fun loadVideo(uri: String): InputStream? {
    val infoPair = jniKiwixReader.getDirectAccessInformation(uri.filePath)
    if (infoPair == null || !File(infoPair.filename).exists()) {
      return loadVideoFromCache(uri)
    }
    return AssetFileDescriptor(
      infoPair.parcelFileDescriptor,
      infoPair.offset,
      articleSize(uri)
    ).createInputStream()
  }

  private fun articleSize(uri: String) = with(JNIKiwixInt()) {
    jniKiwixReader.getContentPart(uri.filePath, 0, 0, this)
    value.toLong()
  }

  @Throws(IOException::class)
  private fun loadVideoFromCache(uri: String): FileInputStream {
    return File(
      FileUtils.getFileCacheDir(CoreApp.getInstance()),
      uri.substringAfterLast("/")
    ).apply { writeBytes(getContent(uri)) }
      .inputStream()
  }

  private fun getContent(url: String) = getContentAndMimeType(url).let { (content, _) -> content }

  private fun streamZimContentToPipe(uri: String, outputStream: OutputStream) {
    Completable.fromCallable {
      try {
        outputStream.use {
          getContentAndMimeType(uri).let { (content: ByteArray, mimeType: String) ->
            if ("text/css" == mimeType && nightModeConfig.isNightModeActive()) {
              it.write(INVERT_IMAGES_VIDEO.toByteArray(Charsets.UTF_8))
            }
            it.write(content)
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
    getContent(url = JNIKiwixString(uri.filePath.removeArguments()), mime = this) to value
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

  fun toBook() = Book().apply {
    title = this@ZimFileReader.title
    id = this@ZimFileReader.id
    size = "$fileSize"
    favicon = this@ZimFileReader.favicon
    creator = this@ZimFileReader.creator
    publisher = this@ZimFileReader.publisher
    date = this@ZimFileReader.date
    description = this@ZimFileReader.description
    language = this@ZimFileReader.language
    articleCount = this@ZimFileReader.articleCount.toString()
    mediaCount = this@ZimFileReader.mediaCount.toString()
    bookName = name
    tags = this@ZimFileReader.tags
  }

  companion object {
    /*
    * these uris aren't actually nullable but unit tests fail to compile as
    * Uri.parse returns null without android dependencies loaded
    */
    @JvmField
    val UI_URI: Uri? = Uri.parse("content://org.kiwix.ui/")

    @JvmField
    val CONTENT_URI: Uri? =
      Uri.parse("content://${CoreApp.getInstance().packageName}.zim.base/")
    private val INVERT_IMAGES_VIDEO =
      """
        img, video, div[poster], div#header { 
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
    private val VIDEO_REGEX = Regex("([^\\s]+(\\.(?i)(3gp|mp4|m4a|webm|mkv|ogg|ogv))\$)")
  }
}

private fun String.removeArguments() = substringBefore("?")
private val Uri.filePath: String
  get() = toString().filePath
private val String.filePath: String
  get() = substringAfter("$CONTENT_URI").substringBefore("#")
private val String.mimeType: String?
  get() = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
    MimeTypeMap.getFileExtensionFromUrl(this)
  )
private val Pair.parcelFileDescriptor: ParcelFileDescriptor?
  get() = ParcelFileDescriptor.open(File(filename), ParcelFileDescriptor.MODE_READ_ONLY)
