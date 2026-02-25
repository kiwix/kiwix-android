package org.kiwix.kiwixmobile.zimManager

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
import org.kiwix.kiwixmobile.BuildConfig.DEBUG
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.OnlineLibraryProgressListener
import org.kiwix.kiwixmobile.core.data.remote.ProgressResponseBody
import org.kiwix.kiwixmobile.core.data.remote.UserAgentInterceptor
import org.kiwix.kiwixmobile.core.di.modules.CALL_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.CONNECTION_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.READ_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.USER_AGENT
import org.kiwix.kiwixmobile.core.utils.DEFAULT_INT_VALUE
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

data class ServiceFactoryConfig(
  val shouldTrackProgress: Boolean,
  val appProgressListener: OnlineLibraryProgressListener?,
  val baseOkHttpClient: OkHttpClient,
  val isUnitTestCase: Boolean,
  val kiwixService: KiwixService
)

class OnlineLibraryServiceFactory @Inject constructor() {
  fun createBaseOkHttpClient(): OkHttpClient =
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
      .build()

  fun createKiwixServiceWithProgressListener(
    baseUrl: String,
    libraryUrl: String,
    config: ServiceFactoryConfig
  ): KiwixService {
    if (config.isUnitTestCase) return config.kiwixService
    val contentLength = getContentLengthOfLibraryXmlFile(
      libraryUrl,
      config.baseOkHttpClient
    )

    val customOkHttpClient = config.baseOkHttpClient.newBuilder()
      .addNetworkInterceptor { chain ->
        val originalResponse = chain.proceed(chain.request())
        val body = originalResponse.body
        if (config.shouldTrackProgress && body != null && config.appProgressListener != null) {
          originalResponse.newBuilder()
            .body(
              ProgressResponseBody(body, config.appProgressListener, contentLength)
            )
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

  private fun getContentLengthOfLibraryXmlFile(
    requestUrl: String,
    client: OkHttpClient
  ): Long {
    val headRequest = Request.Builder()
      .url(requestUrl)
      .head()
      .header("Accept-Encoding", "identity")
      .build()

    return try {
      client.newCall(headRequest).execute().use { response ->
        if (response.isSuccessful) {
          response.header("content-length")?.toLongOrNull()
            ?: DEFAULT_INT_VALUE.toLong()
        } else {
          DEFAULT_INT_VALUE.toLong()
        }
      }
    } catch (_: Exception) {
      DEFAULT_INT_VALUE.toLong()
    }
  }
}
