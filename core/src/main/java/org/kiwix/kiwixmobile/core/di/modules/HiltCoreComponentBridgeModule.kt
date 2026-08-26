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

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadMonitorServiceManager
import org.kiwix.kiwixmobile.core.main.reader.helper.TabsManager
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.ReaderIntentManager
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.StorageDeviceProvider
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore

// TODO(#5023): temporary bridge for the Dagger -> Hilt migration. `CoreApp.coreComponent` (the
// pre-existing, manually-built Dagger graph, guaranteed set before this runs - see
// `CoreApp.attachBaseContext`) stays the single source of truth for these singletons during the
// transition, so code still on the old manual graph and code resolved through Hilt see the exact
// same instances - important for anything stateful (ZimReaderContainer, TabsManager, etc).
// Most of the modules that used to provide these directly have already been converted to native
// Hilt @InstallIn(SingletonComponent::class) modules and no longer need bridging here; what's
// left below is either genuinely stateful (needs to stay the single shared instance until the
// manual graph is fully retired) or not yet traced through.
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
  fun provideStorageObserver(): StorageObserver = CoreApp.coreComponent.storageObserver()

  @Provides
  fun provideLibkiwixBookmarks(): LibkiwixBookmarks = CoreApp.coreComponent.libkiwixBookmarks()

  @Provides
  fun provideLibkiwixBooks(): LibkiwixBookOnDisk = CoreApp.coreComponent.libkiwixBooks()

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
}
