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

import android.webkit.WebResourceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Factory
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZimReaderContainer @Inject constructor(private val zimFileReaderFactory: Factory) {
  var zimFileReader: ZimFileReader? = null
    set(value) {
      field?.dispose()
      field = value
    }

  suspend fun setZimReaderSource(zimReaderSource: ZimReaderSource?) {
    if (zimReaderSource == zimFileReader?.zimReaderSource) {
      return
    }
    zimFileReader = withContext(Dispatchers.IO) {
      if (zimReaderSource?.exists() == true && zimReaderSource.canOpenInLibkiwix())
        zimFileReaderFactory.create(zimReaderSource)
      else null
    }
  }

  fun getPageUrlFromTitle(title: String) = zimFileReader?.getPageUrlFrom(title)

  fun getRandomArticleUrl() = zimFileReader?.getRandomArticleUrl()
  fun isRedirect(url: String): Boolean = zimFileReader?.isRedirect(url) == true
  fun getRedirect(url: String): String = zimFileReader?.getRedirect(url) ?: ""
  fun load(url: String, requestHeaders: Map<String, String>): WebResourceResponse = runBlocking {
    return@runBlocking WebResourceResponse(
      zimFileReader?.getMimeTypeFromUrl(url),
      Charsets.UTF_8.name(),
      zimFileReader?.load(url)
    )
      .apply {
        val headers = mutableMapOf("Accept-Ranges" to "bytes")
        if ("Range" in requestHeaders.keys) {
          setStatusCodeAndReasonPhrase(HttpURLConnection.HTTP_PARTIAL, "Partial Content")
          val fullSize = zimFileReader?.getItem(url)?.itemSize() ?: 0L
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

  val zimReaderSource get() = zimFileReader?.zimReaderSource
  val zimFileTitle get() = zimFileReader?.title
  val mainPage get() = zimFileReader?.mainPage
  val id get() = zimFileReader?.id
  val fileSize get() = zimFileReader?.fileSize ?: 0L
  val creator get() = zimFileReader?.creator
  val publisher get() = zimFileReader?.publisher
  val name get() = zimFileReader?.name
  val date get() = zimFileReader?.date
  val description get() = zimFileReader?.description
  val favicon get() = zimFileReader?.favicon
  val language get() = zimFileReader?.language
}
