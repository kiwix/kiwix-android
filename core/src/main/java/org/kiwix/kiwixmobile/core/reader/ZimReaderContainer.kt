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
import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.WebResourceResponse
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Factory
import java.io.File
import java.io.FileDescriptor
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZimReaderContainer @Inject constructor(private val zimFileReaderFactory: Factory) {
  var contentResolver: ContentResolver? = null
  var uri: String? = null
  var zimFileReader: ZimFileReader? = null
    set(value) {
      field?.dispose()
      field = value
    }

  @SuppressLint("Recycle") fun setZimFile(contentResolver: ContentResolver, uri: String?) {
    this.contentResolver = contentResolver
    this.uri = uri
    // TODO investigate the close() method
    zimFileReader = uri?.let {
      zimFileReaderFactory.create(
        it,
        contentResolver.openFileDescriptor(
          Uri.parse(uri),
          "r"
        )!!
      )
    }
  }

  fun getPageUrlFromTitle(title: String) = zimFileReader?.getPageUrlFrom(title)

  fun getRandomArticleUrl() = zimFileReader?.getRandomArticleUrl()
  fun isRedirect(url: String): Boolean = zimFileReader?.isRedirect(url) == true
  fun getRedirect(url: String): String = zimFileReader?.getRedirect(url) ?: ""
  fun load(url: String, requestHeaders: Map<String, String>): WebResourceResponse {
    val data = zimFileReader?.load(url)
    return WebResourceResponse(
      zimFileReader?.readContentAndMimeType(url),
      Charsets.UTF_8.name(),
      data
    )
      .apply {
        val headers = mutableMapOf("Accept-Ranges" to "bytes")
        if ("Range" in requestHeaders.keys) {
          setStatusCodeAndReasonPhrase(HttpURLConnection.HTTP_PARTIAL, "Partial Content")
          val fullSize = data?.available()?.toLong() ?: 0L
          val lastByte = fullSize - 1
          val byteRanges = requestHeaders.getValue("Range").substringAfter("=").split("-")
          headers["Content-Range"] = "bytes ${byteRanges[0]}-$lastByte/$fullSize"
          if (byteRanges.size == 1) {
            headers["Connection"] = "close"
          }
        } else {
          setStatusCodeAndReasonPhrase(HttpURLConnection.HTTP_OK, "OK")
        }
        responseHeaders = headers
      }
  }

  fun copyReader(): ZimFileReader? {
    val reader = zimFileReader ?: return null
    return zimFileReaderFactory.create(reader.uri, reader.fd)
  }

  val zimFile get() = uri

  val zimCanonicalPath get() = uri
  val zimFileTitle get() = zimFileReader?.title
  val mainPage get() = zimFileReader?.mainPage
  val id get() = zimFileReader?.id
  val fileSize get() = zimFileReader?.fileSize ?: 0
  val creator get() = zimFileReader?.creator
  val publisher get() = zimFileReader?.publisher
  val name get() = zimFileReader?.name
  val date get() = zimFileReader?.date
  val description get() = zimFileReader?.description
  val favicon get() = zimFileReader?.favicon
  val language get() = zimFileReader?.language
}

data class SearchResult(val title: String?)
