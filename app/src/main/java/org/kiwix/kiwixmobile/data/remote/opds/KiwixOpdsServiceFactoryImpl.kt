/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.data.remote.opds

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
import org.kiwix.kiwixmobile.BuildConfig.DEBUG
import org.kiwix.kiwixmobile.data.remote.AppProgressListenerProvider
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.KiwixService.Companion.ITEMS_PER_PAGE
import org.kiwix.kiwixmobile.core.data.remote.UserAgentInterceptor
import org.kiwix.kiwixmobile.core.di.modules.CALL_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.CONNECTION_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.READ_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.USER_AGENT
import org.kiwix.kiwixmobile.core.utils.DEFAULT_INT_VALUE
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.data.remote.OnlineLibraryManager
import org.kiwix.kiwixmobile.data.remote.ProgressResponseBody
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

class KiwixOpdsServiceFactoryImpl @Inject constructor(
  private val onlineLibraryManager: OnlineLibraryManager
) : KiwixOpdsServiceFactory {
  override fun create(
    baseUrl: String,
    start: Int,
    count: Int,
    query: String?,
    lang: String?,
    category: String?,
    shouldTrackProgress: Boolean,
    appProgressListener: AppProgressListenerProvider?
  ): KiwixService {
    val contentLength =
      getContentLengthOfOpdsLibrary(baseUrl, start, count, query, lang, category)
    val customOkHttpClient =
      OkHttpClient().newBuilder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(CONNECTION_TIMEOUT, SECONDS)
        .readTimeout(READ_TIMEOUT, SECONDS)
        .callTimeout(CALL_TIMEOUT, SECONDS)
        .addNetworkInterceptor(
          HttpLoggingInterceptor().apply {
            level = if (DEBUG) BASIC else NONE
          }
        )
        .addNetworkInterceptor(UserAgentInterceptor(USER_AGENT))
        .addNetworkInterceptor { chain ->
          val originalResponse = chain.proceed(chain.request())
          val body = originalResponse.body
          if (shouldTrackProgress && body != null) {
            originalResponse.newBuilder()
              .body(ProgressResponseBody(body, appProgressListener, contentLength))
              .build()
          } else {
            originalResponse
          }
        }
        .build()
    return KiwixService.ServiceCreator.newHackListService(
      customOkHttpClient,
      baseUrl
    )
  }

  private fun getContentLengthOfOpdsLibrary(
    baseUrl: String,
    start: Int = ZERO,
    count: Int = ITEMS_PER_PAGE,
    query: String? = null,
    lang: String? = null,
    category: String? = null
  ): Long {
    val requestUrl =
      onlineLibraryManager.buildLibraryUrl(baseUrl, start, count, query, lang, category)
    val headRequest =
      Request.Builder()
        .url(requestUrl)
        .head()
        .header("Accept-Encoding", "identity")
        .build()
    val client =
      OkHttpClient().newBuilder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(CONNECTION_TIMEOUT, SECONDS)
        .readTimeout(READ_TIMEOUT, SECONDS)
        .callTimeout(CALL_TIMEOUT, SECONDS)
        .addNetworkInterceptor(UserAgentInterceptor(USER_AGENT))
        .build()
    try {
      client.newCall(headRequest).execute().use { response ->
        if (response.isSuccessful) {
          return@getContentLengthOfOpdsLibrary response.header("content-length")?.toLongOrNull()
            ?: DEFAULT_INT_VALUE.toLong()
        }
      }
    } catch (_: Exception) {
      // do nothing
    }
    return DEFAULT_INT_VALUE.toLong()
  }
}
