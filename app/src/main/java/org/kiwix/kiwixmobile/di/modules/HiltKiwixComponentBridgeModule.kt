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

package org.kiwix.kiwixmobile.di.modules

import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import org.kiwix.kiwixmobile.KiwixApp
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.MountPointProducer
import org.kiwix.kiwixmobile.nav.destination.library.local.CopyMoveProgressBarController
import org.kiwix.kiwixmobile.nav.destination.library.local.FileOperationHandler
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.FileWritingFileSystemChecker
import org.kiwix.kiwixmobile.zimManager.MountFileSystemChecker

// TODO(#5023): temporary bridge for the Dagger -> Hilt migration, mirroring
// HiltCoreComponentBridgeModule in :core but for :app's own KiwixComponent. FileOperationHandler
// and CopyMoveProgressBarController are bridged to the legacy `KiwixApp.kiwixComponent` (already
// exposed as accessors there) since they track in-flight copy/move state that must stay a single
// shared instance. The Android system services and Fat32Checker have no such state and no
// existing KiwixComponent accessor, so they're just reconstructed here directly (matching
// KiwixModule's original @Provides bodies) rather than adding new accessors for them.
// Delete this module once every consumer is converted to Hilt and the manual graph is retired.
@InstallIn(SingletonComponent::class)
@Module
object HiltKiwixComponentBridgeModule {
  @Provides
  fun provideFileOperationHandler(application: Application): FileOperationHandler =
    (application as KiwixApp).kiwixComponent.provideFileOperationHandler()

  @Provides
  fun provideCopyMoveProgressBarController(application: Application): CopyMoveProgressBarController =
    (application as KiwixApp).kiwixComponent.provideCopyMoveProgressBarController()

  @Provides
  fun provideLocationManager(@ApplicationContext context: Context): LocationManager =
    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

  @Provides
  fun provideFat32Checker(
    kiwixDataStore: KiwixDataStore,
    mountPointProducer: MountPointProducer,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
  ): Fat32Checker =
    Fat32Checker(
      kiwixDataStore,
      listOf(MountFileSystemChecker(mountPointProducer), FileWritingFileSystemChecker()),
      ioDispatcher
    )

  @Provides
  fun providesWiFiP2pManager(@ApplicationContext context: Context): WifiP2pManager? =
    context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?

  @Provides
  fun provideWifiManager(@ApplicationContext context: Context): WifiManager =
    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
}
