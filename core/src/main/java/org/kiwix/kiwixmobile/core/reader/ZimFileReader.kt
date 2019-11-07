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

import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.AutoCloseOutputStream
import android.os.ParcelFileDescriptor.dup
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixlib.JNIKiwixException
import org.kiwix.kiwixlib.JNIKiwixInt
import org.kiwix.kiwixlib.JNIKiwixReader
import org.kiwix.kiwixlib.JNIKiwixString
import org.kiwix.kiwixlib.Pair
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_URI
import org.kiwix.kiwixmobile.core.search.NextSearchSuggestion
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import javax.inject.Inject

private const val TAG = "ZimFileReader"

class ZimFileReader(
  val zimFile: File,
  val jniKiwixReader: JNIKiwixReader = JNIKiwixReader(zimFile.canonicalPath),
  private val sharedPreferenceUtil: SharedPreferenceUtil
) {
  interface Factory {
    fun create(file: File): ZimFileReader?

    class Impl @Inject constructor(val sharedPreferenceUtil: SharedPreferenceUtil) :
      Factory {
      override fun create(file: File) =
        try {
          ZimFileReader(
            file,
            sharedPreferenceUtil = sharedPreferenceUtil
          )
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
  val favicon: String get() = jniKiwixReader.favicon
  val language: String get() = jniKiwixReader.language
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

  fun getNextSuggestion(): NextSearchSuggestion? {
    val title = JNIKiwixString()
    val url = JNIKiwixString()
    if (jniKiwixReader.getNextSuggestion(title, url)) {
      return NextSearchSuggestion(title.value, url.value)
    }
    return null
  }

  fun getPageUrlFrom(title: String): String? =
    valueOfJniStringAfter { jniKiwixReader.getPageUrlFromTitle(title, it) }

  fun getRandomArticleUrl(): String? =
    valueOfJniStringAfter(jniKiwixReader::getRandomPage)

  fun load(uri: Uri): ParcelFileDescriptor {
    if ("$uri".matches(VIDEO_REGEX)) {
      try {
        return loadVideo(uri)
      } catch (ioException: IOException) {
        Log.e(TAG, "failed to write video for $uri", ioException)
      }
    }
    return loadContent(uri)
  }

  fun readMimeType(uri: Uri) = "$uri".removeArguments().let {
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

  private fun loadContent(uri: Uri) =
    try {
      ParcelFileDescriptor.createPipe().also {
        streamZimContentToPipe(uri, AutoCloseOutputStream(it[1]))
      }[0]
    } catch (ioException: IOException) {
      throw IOException("Could not open pipe for $uri", ioException)
    }

  private fun loadVideo(uri: Uri): ParcelFileDescriptor {
    val infoPair = jniKiwixReader.getDirectAccessInformation(uri.filePath)
    if (infoPair == null || !File(infoPair.filename).exists()) {
      return loadVideoFromCache(uri)
    }
    return dup(infoPair.fileDescriptor)
  }

  @Throws(IOException::class)
  private fun loadVideoFromCache(uri: Uri): ParcelFileDescriptor {
    val outputFile = File(
      FileUtils.getFileCacheDir(CoreApp.getInstance()),
      "$uri".substringAfterLast("/")
    )
    FileOutputStream(outputFile).use { it.write(getContent(uri)) }
    return ParcelFileDescriptor.open(outputFile, ParcelFileDescriptor.MODE_READ_ONLY)
  }

  private fun streamZimContentToPipe(
    uri: Uri,
    outputStream: AutoCloseOutputStream
  ) {
    Single.just(Unit)
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe(
        {
          try {
            outputStream.use {
              val mime = JNIKiwixString()
              val size = JNIKiwixInt()
              val url = JNIKiwixString(uri.filePath.removeArguments())
              val content = getContent(url = url, mime = mime, size = size)
              if ("text/css" == mime.value && sharedPreferenceUtil.nightMode()) {
                it.write(INVERT_IMAGES_VIDEO.toByteArray(Charsets.UTF_8))
              }
              it.write(content)
              Log.d(
                TAG,
                "reading  ${url.value}(mime: ${mime.value}, size: ${size.value}) finished."
              )
            }
          } catch (ioException: IOException) {
            Log.e(TAG, "error writing pipe for $uri", ioException)
          }
        },
        Throwable::printStackTrace
      )
  }

  private fun getContent(uri: Uri) = getContent(JNIKiwixString(uri.filePath.removeArguments()))

  private fun getContent(
    url: JNIKiwixString = JNIKiwixString(),
    jniKiwixString: JNIKiwixString = JNIKiwixString(),
    mime: JNIKiwixString = JNIKiwixString(),
    size: JNIKiwixInt = JNIKiwixInt()
  ) = jniKiwixReader.getContent(url, jniKiwixString, mime, size)

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
    private const val INVERT_IMAGES_VIDEO =
      "img, video { \n -webkit-filter: invert(1); \n filter: invert(1); \n} \n"
    private val VIDEO_REGEX = Regex("([^\\s]+(\\.(?i)(3gp|mp4|m4a|webm|mkv|ogg|ogv))\$)")
  }
}

private fun String.removeArguments() = substringBefore("?")
private val Pair.fileDescriptor: FileDescriptor?
  get() = RandomAccessFile(filename, "r").apply { seek(offset.toLong()) }.fd
private val Uri.filePath: String
  get() = toString().filePath
private val String.filePath: String
  get() = substringAfter("$CONTENT_URI").substringBefore("#")
private val String.mimeType: String?
  get() = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
    MimeTypeMap.getFileExtensionFromUrl(this)
  )
