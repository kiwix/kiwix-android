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
package org.kiwix.kiwixmobile.core.di.components

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import dagger.BindsInstance
import dagger.Component
import kotlinx.coroutines.sync.Mutex
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.LibkiwixBookFactory
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.dao.AppUpdateDao
import org.kiwix.kiwixmobile.core.dao.DownloadApkDao
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.HistoryRoomDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.dao.NotesRoomDao
import org.kiwix.kiwixmobile.core.dao.RecentSearchRoomDao
import org.kiwix.kiwixmobile.core.dao.WebViewHistoryRoomDao
import org.kiwix.kiwixmobile.core.data.DataModule
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.di.modules.ApplicationModule
import org.kiwix.kiwixmobile.core.di.modules.CoreViewModelModule
import org.kiwix.kiwixmobile.core.di.modules.JNIModule
import org.kiwix.kiwixmobile.core.di.modules.MutexModule
import org.kiwix.kiwixmobile.core.di.modules.NetworkModule
import org.kiwix.kiwixmobile.core.di.modules.SearchModule
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.error.ErrorActivity
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchResultGenerator
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import javax.inject.Singleton

@Singleton
@Component(
  modules = [
    ApplicationModule::class,
    NetworkModule::class,
    JNIModule::class,
    DataModule::class,
    CoreViewModelModule::class,
    SearchModule::class,
    MutexModule::class
  ]
)
interface CoreComponent {
  @Component.Builder
  interface Builder {
    @BindsInstance fun context(context: Context): Builder

    fun build(): CoreComponent
  }

  fun activityComponentBuilder(): CoreActivityComponent.Builder
  fun zimReaderContainer(): ZimReaderContainer
  fun kiwixDataStore(): KiwixDataStore
  fun zimFileReaderFactory(): ZimFileReader.Factory
  fun libkiwixBookFactory(): LibkiwixBookFactory
  fun storageObserver(): StorageObserver
  fun kiwixService(): KiwixService
  fun application(): Application
  fun bookUtils(): BookUtils
  fun dataSource(): DataSource
  fun downloadRoomDao(): DownloadRoomDao
  fun connectivityManager(): ConnectivityManager
  fun libkiwixBookmarks(): LibkiwixBookmarks
  fun libkiwixBooks(): LibkiwixBookOnDisk
  fun recentSearchRoomDao(): RecentSearchRoomDao
  fun historyRoomDao(): HistoryRoomDao
  fun webViewHistoryRoomDao(): WebViewHistoryRoomDao
  fun noteRoomDao(): NotesRoomDao
  fun appUpdateDao(): AppUpdateDao
  fun downloadApkDao(): DownloadApkDao
  fun context(): Context
  fun downloader(): Downloader
  fun notificationManager(): NotificationManager
  fun searchResultGenerator(): SearchResultGenerator
  fun mutex(): Mutex

  fun inject(application: CoreApp)
  fun inject(kiwixWebView: KiwixWebView)

  fun inject(errorActivity: ErrorActivity)
  fun coreServiceComponent(): CoreServiceComponent.Builder
}
