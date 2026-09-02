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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Factory
import java.net.HttpURLConnection
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.read
import kotlin.concurrent.write

@Singleton
class ZimReaderContainer @Inject constructor(
  private val zimFileReaderFactory: Factory,
  @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
  // Guards `backingZimFileReader` against the native use-after-free that happens when the
  // setter below disposes the current reader (native Archive/SuggestionSearcher) while the
  // WebView's Chromium worker thread is mid-read via isRedirect/getRedirect/load, called from
  // CoreWebViewClient.shouldInterceptRequest. The write lock ensures dispose() only runs once
  // every in-flight read of this class's own methods has finished; readers see either the old
  // or the new reader, never a disposed one.
  private val lock = ReentrantReadWriteLock()
  private var backingZimFileReader: ZimFileReader? = null

  var zimFileReader: ZimFileReader?
    get() = lock.read { backingZimFileReader }
    set(value) {
      lock.write {
        backingZimFileReader?.dispose()
        backingZimFileReader = value
      }
    }

  private inline fun <T> withReader(block: (ZimFileReader?) -> T): T =
    lock.read { block(backingZimFileReader) }

  suspend fun setZimReaderSource(
    zimReaderSource: ZimReaderSource?,
    showSearchSuggestionsSpellChecked: Boolean = false
  ) {
    if (zimReaderSource == withReader { it?.zimReaderSource }) {
      return
    }
    zimFileReader = withContext(ioDispatcher) {
      if (zimReaderSource?.exists(ioDispatcher) == true && zimReaderSource.canOpenInLibkiwix(
          ioDispatcher
        )
      ) {
        zimFileReaderFactory.create(zimReaderSource, showSearchSuggestionsSpellChecked)
      } else {
        null
      }
    }
  }

  fun getPageUrlFromTitle(title: String) = withReader { it?.getPageUrlFrom(title) }

  fun getRandomPageUrl() = withReader { it?.getRandomPageUrl() }
  fun isRedirect(url: String): Boolean = withReader { it?.isRedirect(url) == true }
  fun getRedirect(url: String): String = withReader { it?.getRedirect(url) }.orEmpty()
  fun load(url: String, requestHeaders: Map<String, String>): WebResourceResponse = runBlocking {
    return@runBlocking withReader { reader ->
      WebResourceResponse(
        reader?.getMimeTypeFromUrl(url),
        Charsets.UTF_8.name(),
        reader?.load(url)
      )
        .apply {
          val headers = mutableMapOf("Accept-Ranges" to "bytes")
          if ("Range" in requestHeaders.keys) {
            setStatusCodeAndReasonPhrase(HttpURLConnection.HTTP_PARTIAL, "Partial Content")
            val fullSize = reader?.getItem(url)?.itemSize() ?: 0L
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
  }

  val zimReaderSource get() = withReader { it?.zimReaderSource }
  val zimFileTitle get() = withReader { it?.title }
  val mainPage get() = withReader { it?.mainPage }
  val id get() = withReader { it?.id }
  val fileSize get() = withReader { it?.fileSize } ?: 0L
  val creator get() = withReader { it?.creator }
  val publisher get() = withReader { it?.publisher }
  val name get() = withReader { it?.name }
  val date get() = withReader { it?.date }
  val description get() = withReader { it?.description }
  val favicon get() = withReader { it?.favicon }
  val language get() = withReader { it?.language }
}
