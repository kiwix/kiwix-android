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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import nl.adaptivity.xmlutil.serialization.XML
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getPackageInformation
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.KiwixService.ServiceCreator
import org.kiwix.kiwixmobile.core.data.remote.UserAgentInterceptor
import org.kiwix.kiwixmobile.core.di.OPDSKiwixService
import org.kiwix.kiwixmobile.core.utils.ZERO
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Singleton

const val CONNECTION_TIMEOUT = 10L

// increase the read and call timeout since the content is 19MB large so it takes
// more time to read on slow internet connection, and due to less read timeout
// the request is canceled.
const val READ_TIMEOUT = 300L
const val CALL_TIMEOUT = 300L
const val KIWIX_OPDS_LIBRARY_URL = "https://opds.library.kiwix.org/"

// VERSION_NAME lives in the app/branded modules (not core's own BuildConfig), so the
// running app's actual version is read from the installed package at request time.
fun userAgent(context: Context): String {
  val versionName = context.packageManager
    .getPackageInformation(context.packageName, ZERO).versionName
  return "kiwix/$versionName (android)"
}

@InstallIn(SingletonComponent::class)
@Module
class NetworkModule {
  @Provides
  @Singleton
  fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient =
    OkHttpClient().newBuilder()
      .followRedirects(true)
      .followSslRedirects(true)
      .connectTimeout(CONNECTION_TIMEOUT, SECONDS)
      .readTimeout(READ_TIMEOUT, SECONDS)
      .callTimeout(CALL_TIMEOUT, SECONDS)
      .addNetworkInterceptor(UserAgentInterceptor(userAgent(context)))
      .addNetworkInterceptor(
        HttpLoggingInterceptor().apply {
          level = if (BuildConfig.DEBUG) BASIC else NONE
        }
      )
      .build()

  @Provides
  @Singleton
  fun provideXML(): XML = XML {
    defaultPolicy {
      ignoreUnknownChildren()
    }
  }

  @Provides
  @Singleton
  @OPDSKiwixService
  fun provideKiwixService(okHttpClient: OkHttpClient, xml: XML): KiwixService =
    ServiceCreator.newHackListService(okHttpClient, xml, KIWIX_OPDS_LIBRARY_URL)
}
