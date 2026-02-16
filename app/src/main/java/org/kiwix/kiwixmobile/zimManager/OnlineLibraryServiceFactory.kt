package org.kiwix.kiwixmobile.zimManager

import okhttp3.OkHttpClient
import okhttp3.Request
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.OnlineLibraryProgressListener
import org.kiwix.kiwixmobile.core.data.remote.ProgressResponseBody
import org.kiwix.kiwixmobile.core.utils.DEFAULT_INT_VALUE

data class ServiceFactoryConfig(
  val shouldTrackProgress: Boolean,
  val appProgressListener: OnlineLibraryProgressListener?,
  val baseOkHttpClient: OkHttpClient,
  val isUnitTestCase: Boolean,
  val kiwixService: KiwixService
)

object OnlineLibraryServiceFactory {
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
