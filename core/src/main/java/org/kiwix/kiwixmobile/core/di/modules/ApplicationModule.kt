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

import android.app.Application
import android.app.DownloadManager
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.storage.StorageManager
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.NightModeConfig
import org.kiwix.kiwixmobile.core.di.qualifiers.Computation
import org.kiwix.kiwixmobile.core.di.qualifiers.IO
import org.kiwix.kiwixmobile.core.di.qualifiers.MainThread
import org.kiwix.kiwixmobile.core.downloader.DownloadMonitor
import org.kiwix.kiwixmobile.core.downloader.fetch.FetchDownloadMonitor
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.utils.BookUtils
import javax.inject.Singleton

@Module(
  includes = [
    DownloaderModule::class,
    DatabaseModule::class
  ]
)
class ApplicationModule {

  @Provides
  @Singleton
  internal fun provideApplication(context: Context): Application =
    context as Application

  @Provides
  @Singleton
  internal fun provideNotificationManager(context: Context): NotificationManager =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  @Provides
  @Singleton
  internal fun provideDownloadManager(context: Context): DownloadManager =
    context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

  @Provides
  @Singleton
  internal fun provideBookUtils(): BookUtils = BookUtils()

  @IO
  @Provides
  fun provideIoThread(): Scheduler = Schedulers.io()

  @MainThread
  @Provides
  fun provideMainThread(): Scheduler = AndroidSchedulers.mainThread()

  @Computation
  @Provides
  fun provideComputationThread(): Scheduler = Schedulers.computation()

  @Provides
  @Singleton
  internal fun provideDownloadMonitor(fetchDownloadMonitor: FetchDownloadMonitor): DownloadMonitor =
    fetchDownloadMonitor

  @Provides
  @Singleton
  internal fun provideStorageManager(context: Context): StorageManager =
    context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

  @Provides
  @Singleton
  internal fun providesZimFileReaderFactory(nightModeConfig: NightModeConfig):
    ZimFileReader.Factory = ZimFileReader.Factory.Impl(nightModeConfig)

  @Provides
  @Singleton
  fun provideConnectivityManager(context: Context): ConnectivityManager =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  @Provides
  @Singleton
  fun provideWifiManager(context: Context): WifiManager =
    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
}
