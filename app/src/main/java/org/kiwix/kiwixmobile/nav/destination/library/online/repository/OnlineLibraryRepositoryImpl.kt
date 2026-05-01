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

package org.kiwix.kiwixmobile.nav.destination.library.online.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
import org.kiwix.kiwixmobile.BuildConfig.DEBUG
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.KiwixService.Companion.ITEMS_PER_PAGE
import org.kiwix.kiwixmobile.core.data.remote.ProgressResponseBody
import org.kiwix.kiwixmobile.core.data.remote.UserAgentInterceptor
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.di.OPDSKiwixService
import org.kiwix.kiwixmobile.core.di.modules.CALL_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.CONNECTION_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.KIWIX_OPDS_LIBRARY_URL
import org.kiwix.kiwixmobile.core.di.modules.READ_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.USER_AGENT
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.DEFAULT_INT_VALUE
import org.kiwix.kiwixmobile.core.utils.FIVE
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.OnlineLibraryRequest
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.OnlineLibraryState
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.OnlineLibraryState.Parsing
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.OnlineLibraryState.Success
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.OnlineLibraryState.Loading
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.OnlineLibraryState.Error
import org.kiwix.kiwixmobile.zimManager.AppProgressListenerProvider
import org.kiwix.kiwixmobile.zimManager.OnlineLibraryManager
import retrofit2.Response
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

class OnlineLibraryRepositoryImpl @Inject constructor(
  private val onlineLibraryManager: OnlineLibraryManager,
  @OPDSKiwixService private var kiwixService: KiwixService,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : OnlineLibraryRepository {
  override fun fetchOnlineLibrary(
    request: OnlineLibraryRequest,
    appProgressListener: AppProgressListenerProvider?
  ): Flow<OnlineLibraryState> = flow {
    emit(Loading(request.isLoadMoreItem))
    val maxRetries = FIVE
    repeat(maxRetries) { attempt ->
      try {
        val baseUrl = KIWIX_OPDS_LIBRARY_URL
        val start = onlineLibraryManager.getStartOffset(request.page, ITEMS_PER_PAGE)

        val service = createKiwixServiceWithProgressListener(
          baseUrl = baseUrl,
          start = start,
          count = ITEMS_PER_PAGE,
          query = request.query,
          lang = request.lang,
          category = request.category,
          shouldTrackProgress = !request.isLoadMoreItem,
          appProgressListener = appProgressListener
        )

        val url = onlineLibraryManager.buildLibraryUrl(
          baseUrl,
          start,
          ITEMS_PER_PAGE,
          request.query,
          request.lang,
          request.category
        )

        val response = service.getLibraryPage(url)
        val base = response.getResolvedBaseUrl()
        emit(Parsing)
        val books = onlineLibraryManager
          .parseOPDSStreamAndGetBooks(response.body(), base)
          .orEmpty()
        val totalResult = onlineLibraryManager.totalResult
        val totalPages = onlineLibraryManager.calculateTotalPages(
          totalResult,
          ITEMS_PER_PAGE
        )
        emit(Success(request, books, totalPages))
        return@flow
      } catch (ignore: Exception) {
        if (attempt == maxRetries - ONE) {
          emit(Error(request, ignore))
        }
      }
    }
  }.flowOn(ioDispatcher)

  private fun createKiwixServiceWithProgressListener(
    baseUrl: String,
    start: Int = ZERO,
    count: Int = ITEMS_PER_PAGE,
    query: String? = null,
    lang: String? = null,
    category: String? = null,
    shouldTrackProgress: Boolean,
    appProgressListener: AppProgressListenerProvider?
  ): KiwixService {
    val contentLength =
      getContentLengthOfLibraryXmlFile(baseUrl, start, count, query, lang, category)
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
      .also {
        kiwixService = it
      }
  }

  private fun getContentLengthOfLibraryXmlFile(
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
          return@getContentLengthOfLibraryXmlFile response.header("content-length")?.toLongOrNull()
            ?: DEFAULT_INT_VALUE.toLong()
        }
      }
    } catch (_: Exception) {
      // do nothing
    }
    return DEFAULT_INT_VALUE.toLong()
  }

  private fun Response<String>.getResolvedBaseUrl(): String {
    val url = raw().networkResponse?.request?.url ?: raw().request.url
    return "${url.scheme}://${url.host}"
  }
}
