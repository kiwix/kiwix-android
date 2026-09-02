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
  // isRedirect()/getRedirect()/load() below are called from CoreWebViewClient on
  // Chromium's own IO thread, concurrently with setZimReaderSource() swapping the
  // reader (and disposing the native Archive underneath it) from a coroutine on
  // ioDispatcher. Without this lock, dispose() can run while a native call on the
  // old reader is still in flight - a use-after-free across the JNI boundary,
  // i.e. a native SIGSEGV, not something a try/catch can stop. The read lock is
  // held for the *entire* native call, not just the field access, and the write
  // lock (dispose + swap) can't proceed until every in-flight read has finished.
  private val lock = ReentrantReadWriteLock()
  private var _zimFileReader: ZimFileReader? = null

  var zimFileReader: ZimFileReader?
    get() = lock.read { _zimFileReader }
    private set(value) {
      lock.write {
        _zimFileReader?.dispose()
        _zimFileReader = value
      }
    }

  private inline fun <T> withReader(default: T, block: (ZimFileReader) -> T): T =
    lock.read { _zimFileReader?.let(block) ?: default }

  suspend fun setZimReaderSource(
    zimReaderSource: ZimReaderSource?,
    showSearchSuggestionsSpellChecked: Boolean = false
  ) {
    if (zimReaderSource == zimFileReader?.zimReaderSource) {
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

  fun getPageUrlFromTitle(title: String) = withReader(null) { it.getPageUrlFrom(title) }

  fun getRandomPageUrl() = withReader(null) { it.getRandomPageUrl() }
  fun isRedirect(url: String): Boolean = withReader(false) { it.isRedirect(url) }
  fun getRedirect(url: String): String = withReader("") { it.getRedirect(url) }
  fun load(url: String, requestHeaders: Map<String, String>): WebResourceResponse = runBlocking {
    // load() on ZimFileReader is itself suspend (it hops to ioDispatcher internally),
    // so this can't go through withReader()'s plain (non-suspend) lambda - the read
    // lock is taken and released by hand instead, but for the exact same reason:
    // held for the whole call, including that internal dispatcher hop, so dispose()
    // still can't run until this is completely done.
    lock.readLock().lock()
    try {
      val reader = _zimFileReader
        ?: return@runBlocking WebResourceResponse(null, Charsets.UTF_8.name(), null)
      WebResourceResponse(
        reader.getMimeTypeFromUrl(url),
        Charsets.UTF_8.name(),
        reader.load(url)
      )
        .apply {
          val headers = mutableMapOf("Accept-Ranges" to "bytes")
          if ("Range" in requestHeaders.keys) {
            setStatusCodeAndReasonPhrase(HttpURLConnection.HTTP_PARTIAL, "Partial Content")
            val fullSize = reader.getItem(url)?.itemSize() ?: 0L
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
    } finally {
      lock.readLock().unlock()
    }
  }

  val zimReaderSource get() = withReader(null) { it.zimReaderSource }
  val zimFileTitle get() = withReader(null) { it.title }
  val mainPage get() = withReader(null) { it.mainPage }
  val id get() = withReader(null) { it.id }
  val fileSize get() = withReader(0L) { it.fileSize }
  val creator get() = withReader(null) { it.creator }
  val publisher get() = withReader(null) { it.publisher }
  val name get() = withReader(null) { it.name }
  val date get() = withReader(null) { it.date }
  val description get() = withReader(null) { it.description }
  val favicon get() = withReader(null) { it.favicon }
  val language get() = withReader(null) { it.language }
}
