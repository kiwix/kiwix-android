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

package org.kiwix.kiwixmobile.core.di.modules

import android.app.NotificationManager
import android.net.ConnectivityManager
import android.print.PdfPrint
import com.tonyodev.fetch2.Fetch
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.LibkiwixBookFactory
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.HistoryRoomDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.dao.NotesRoomDao
import org.kiwix.kiwixmobile.core.dao.RecentSearchRoomDao
import org.kiwix.kiwixmobile.core.dao.WebViewHistoryRoomDao
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.di.MainDispatcher
import org.kiwix.kiwixmobile.core.di.OPDSKiwixService
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadMonitorServiceManager
import org.kiwix.kiwixmobile.core.main.reader.helper.TabsManager
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.ReaderIntentManager
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchResultGenerator
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.StorageDeviceProvider
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.libkiwix.JNIKiwix

// TODO(#5023): temporary bridge for the Dagger -> Hilt migration. `CoreApp.coreComponent` (the
// pre-existing, manually-built Dagger graph, guaranteed set before this runs - see
// `CoreApp.attachBaseContext`) stays the single source of truth for these singletons during the
// transition, so code still on the old manual graph (Fragments, Services, other Activities not
// yet converted) and code resolved through Hilt see the exact same instances - important for
// anything stateful (the Room DAOs, ZimReaderContainer, Mutex, TabsManager, Downloader, etc).
// The modules that used to provide these directly (ApplicationModule, NetworkModule, JNIModule,
// SearchModule, MutexModule, KiwixPermissionModule, ReaderModule, DownloaderModule,
// DatabaseModule, CoroutineModule, core/data/DataModule) are now @DisableInstallInCheck and only
// serve the legacy `CoreComponent`/`DaggerCoreComponent` graph.
// Delete this module once every consumer across the app is converted to Hilt and the manual
// graph is retired - at that point each type below should get a normal Hilt binding instead
// (either an @Inject constructor or a @Provides/@Binds installed directly in SingletonComponent).
@InstallIn(SingletonComponent::class)
@Module
object HiltCoreComponentBridgeModule {
  @Provides
  fun provideZimReaderContainer(): ZimReaderContainer = CoreApp.coreComponent.zimReaderContainer()

  @Provides
  fun provideKiwixDataStore(): KiwixDataStore = CoreApp.coreComponent.kiwixDataStore()

  @Provides
  fun provideZimFileReaderFactory(): ZimFileReader.Factory =
    CoreApp.coreComponent.zimFileReaderFactory()

  @Provides
  fun provideLibkiwixBookFactory(): LibkiwixBookFactory =
    CoreApp.coreComponent.libkiwixBookFactory()

  @Provides
  fun provideJniKiwix(): JNIKiwix = CoreApp.coreComponent.jniKiwix()

  @Provides
  fun provideStorageObserver(): StorageObserver = CoreApp.coreComponent.storageObserver()

  @OPDSKiwixService
  @Provides
  fun provideOPDSKiwixService(): KiwixService = CoreApp.coreComponent.provideOPDSKiwixService()

  @Provides
  fun provideBookUtils(): BookUtils = CoreApp.coreComponent.bookUtils()

  @Provides
  fun provideDataSource(): DataSource = CoreApp.coreComponent.dataSource()

  @Provides
  fun provideDownloadRoomDao(): DownloadRoomDao = CoreApp.coreComponent.downloadRoomDao()

  @Provides
  fun provideConnectivityManager(): ConnectivityManager =
    CoreApp.coreComponent.connectivityManager()

  @Provides
  fun provideLibkiwixBookmarks(): LibkiwixBookmarks = CoreApp.coreComponent.libkiwixBookmarks()

  @Provides
  fun provideLibkiwixBooks(): LibkiwixBookOnDisk = CoreApp.coreComponent.libkiwixBooks()

  @Provides
  fun provideRecentSearchRoomDao(): RecentSearchRoomDao =
    CoreApp.coreComponent.recentSearchRoomDao()

  @Provides
  fun provideHistoryRoomDao(): HistoryRoomDao = CoreApp.coreComponent.historyRoomDao()

  @Provides
  fun provideWebViewHistoryRoomDao(): WebViewHistoryRoomDao =
    CoreApp.coreComponent.webViewHistoryRoomDao()

  @Provides
  fun provideNoteRoomDao(): NotesRoomDao = CoreApp.coreComponent.noteRoomDao()

  @Provides
  fun provideDownloader(): Downloader = CoreApp.coreComponent.downloader()

  @Provides
  fun provideNotificationManager(): NotificationManager =
    CoreApp.coreComponent.notificationManager()

  @Provides
  fun provideSearchResultGenerator(): SearchResultGenerator =
    CoreApp.coreComponent.searchResultGenerator()

  @Provides
  fun provideMutex(): Mutex = CoreApp.coreComponent.mutex()

  @Provides
  fun provideKiwixPermissionChecker(): KiwixPermissionChecker =
    CoreApp.coreComponent.kiwixPermissionChecker()

  @IoDispatcher
  @Provides
  fun provideIoDispatcher(): CoroutineDispatcher = CoreApp.coreComponent.provideIoDispatcher()

  @MainDispatcher
  @Provides
  fun provideMainDispatcher(): MainCoroutineDispatcher =
    CoreApp.coreComponent.provideMainDispatcher()

  @Provides
  fun providePdfPrinter(): PdfPrint = CoreApp.coreComponent.providePdfPrinter()

  @Provides
  fun provideTabsManager(): TabsManager = CoreApp.coreComponent.provideTabsManager()

  @Provides
  fun provideReaderIntentManager(): ReaderIntentManager =
    CoreApp.coreComponent.provideReaderIntentManager()

  @Provides
  fun provideStorageDeviceProvider(): StorageDeviceProvider =
    CoreApp.coreComponent.provideStorageDeviceProvider()

  @Provides
  fun provideDownloadMonitorServiceManager(): DownloadMonitorServiceManager =
    CoreApp.coreComponent.provideDownloadMonitorServiceManager()

  @Provides
  fun provideFetch(): Fetch = CoreApp.coreComponent.fetch()
}
