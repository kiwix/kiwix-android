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
package org.kiwix.kiwixmobile.core.di.modules

import android.content.Context
import android.net.ConnectivityManager
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.KiwixService.ServiceCreator
import org.kiwix.kiwixmobile.core.data.remote.UserAgentInterceptor
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Singleton

@Module
class NetworkModule {
  @Provides @Singleton fun provideOkHttpClient(): OkHttpClient {
    return OkHttpClient().newBuilder().followRedirects(true).followSslRedirects(true)
      .connectTimeout(CONNECTION_TIMEOUT, SECONDS)
      .readTimeout(READ_TIMEOUT, SECONDS)
      .addNetworkInterceptor(HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) BASIC else NONE
      })
      .addNetworkInterceptor(UserAgentInterceptor(userAgent))
      .build()
  }

  @Provides @Singleton fun provideKiwixService(okHttpClient: OkHttpClient?): KiwixService =
    ServiceCreator.newHacklistService(okHttpClient, KIWIX_DOWNLOAD_URL)

  @Provides @Singleton fun provideConnectivityManager(context: Context): ConnectivityManager =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  companion object {
    private const val CONNECTION_TIMEOUT = 10L
    private const val READ_TIMEOUT = 60L
    private const val userAgent = "kiwix-android-version:${BuildConfig.VERSION_CODE}"
    private const val KIWIX_DOWNLOAD_URL = "http://mirror.download.kiwix.org/"
  }
}
