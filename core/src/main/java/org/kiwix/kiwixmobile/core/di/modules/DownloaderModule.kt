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

import android.app.DownloadManager
import android.app.NotificationManager
import android.content.Context
import dagger.Module
import dagger.Provides
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.downloader.DownloadRequester
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.downloader.DownloaderImpl
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadManagerBroadcastReceiver
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadManagerMonitor
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadManagerRequester
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadNotificationManager
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Singleton

@Module
object DownloaderModule {
  @Provides
  @Singleton
  fun providesDownloader(
    downloadRequester: DownloadRequester,
    downloadRoomDao: DownloadRoomDao,
    kiwixService: KiwixService,
    sharedPreferenceUtil: SharedPreferenceUtil
  ): Downloader =
    DownloaderImpl(downloadRequester, downloadRoomDao, kiwixService, sharedPreferenceUtil)

  @Provides
  @Singleton
  fun providesDownloadRequester(
    downloadManager: DownloadManager,
    sharedPreferenceUtil: SharedPreferenceUtil,
    downloadManagerMonitor: DownloadManagerMonitor
  ): DownloadRequester = DownloadManagerRequester(
    downloadManager,
    sharedPreferenceUtil,
    downloadManagerMonitor
  )

  @Provides
  @Singleton
  fun provideDownloadManagerCallback(
    downloadManagerMonitor: DownloadManagerMonitor
  ): DownloadManagerBroadcastReceiver.Callback = downloadManagerMonitor

  @Provides
  @Singleton
  fun providesDownloadManagerBroadcastReceiver(
    callback: DownloadManagerBroadcastReceiver.Callback
  ): DownloadManagerBroadcastReceiver = DownloadManagerBroadcastReceiver(callback)

  @Provides
  @Singleton
  fun providesDownloadNotificationManager(
    context: Context,
    notificationManager: NotificationManager
  ): DownloadNotificationManager = DownloadNotificationManager(context, notificationManager)
}
