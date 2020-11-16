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

package org.kiwix.kiwixmobile.di.modules

import android.content.Context
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import dagger.Module
import dagger.Provides
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.zim_manager.MountPointProducer
import org.kiwix.kiwixmobile.di.KiwixScope
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker
import org.kiwix.kiwixmobile.zim_manager.FileWritingFileSystemChecker
import org.kiwix.kiwixmobile.zim_manager.MountFileSystemChecker

@Module
object KiwixModule {
  @Provides
  @KiwixScope
  internal fun provideLocationManager(context: Context): LocationManager =
    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

  @Provides
  @KiwixScope
  fun provideWifiManager(context: Context): WifiManager =
    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

  @Provides
  @KiwixScope
  internal fun provideFat32Checker(
    sharedPreferenceUtil: SharedPreferenceUtil,
    mountPointProducer: MountPointProducer
  ): Fat32Checker =
    Fat32Checker(
      sharedPreferenceUtil,
      listOf(MountFileSystemChecker(mountPointProducer), FileWritingFileSystemChecker())
    )

  @Provides
  @KiwixScope
  fun providesWiFiP2pManager(context: Context): WifiP2pManager {
    val manager: WifiP2pManager by lazy(LazyThreadSafetyMode.NONE) {
      context.getSystemService(
        Context.WIFI_P2P_SERVICE
      ) as WifiP2pManager
    }
    return manager
  }
}
