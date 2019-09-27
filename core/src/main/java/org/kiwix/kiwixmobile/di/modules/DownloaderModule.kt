/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.di.modules

import android.content.Context
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.Fetch.Impl
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.FetchNotificationManager
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.data.remote.KiwixService
import org.kiwix.kiwixmobile.database.newdb.dao.FetchDownloadDao
import org.kiwix.kiwixmobile.downloader.DownloadRequester
import org.kiwix.kiwixmobile.downloader.Downloader
import org.kiwix.kiwixmobile.downloader.DownloaderImpl
import org.kiwix.kiwixmobile.downloader.fetch.FetchDownloadNotificationManager
import org.kiwix.kiwixmobile.downloader.fetch.FetchDownloadRequester
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil
import javax.inject.Singleton

@Module
object DownloaderModule {
  @JvmStatic
  @Provides
  @Singleton
  fun providesDownloader(
    downloadRequester: DownloadRequester,
    downloadDao: FetchDownloadDao,
    kiwixService: KiwixService
  ): Downloader = DownloaderImpl(downloadRequester, downloadDao, kiwixService)

  @JvmStatic
  @Provides
  @Singleton
  fun providesDownloadRequester(
    fetch: Fetch,
    sharedPreferenceUtil: SharedPreferenceUtil
  ): DownloadRequester = FetchDownloadRequester(fetch, sharedPreferenceUtil)

  @JvmStatic
  @Provides
  @Singleton
  fun provideFetch(fetchConfiguration: FetchConfiguration): Fetch =
    Fetch.getInstance(fetchConfiguration)

  @JvmStatic
  @Provides
  @Singleton
  fun provideFetchConfiguration(
    context: Context,
    okHttpDownloader: OkHttpDownloader,
    fetchNotificationManager: FetchNotificationManager
  ): FetchConfiguration =
    FetchConfiguration.Builder(context).apply {
      setDownloadConcurrentLimit(5)
      enableLogging(BuildConfig.DEBUG)
      enableRetryOnNetworkGain(true)
      setHttpDownloader(okHttpDownloader)
      setNotificationManager(fetchNotificationManager)
    }.build().also(Impl::setDefaultInstanceConfiguration)

  @JvmStatic
  @Provides
  @Singleton
  fun provideOkHttpDownloader() = OkHttpDownloader(OkHttpClient.Builder().build())

  @JvmStatic
  @Provides
  @Singleton
  fun provideFetchDownloadNotificationManager(context: Context): FetchNotificationManager =
    FetchDownloadNotificationManager(context)
}
