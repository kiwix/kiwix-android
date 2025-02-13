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
import android.net.wifi.p2p.WifiP2pManager
import dagger.Module
import dagger.Provides
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.zim_manager.MountPointProducer
import org.kiwix.kiwixmobile.di.KiwixScope
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.FileWritingFileSystemChecker
import org.kiwix.kiwixmobile.zimManager.MountFileSystemChecker

@Module
object KiwixModule {
  @Provides
  @KiwixScope
  internal fun provideLocationManager(context: Context): LocationManager =
    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

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
  // We are forced to use the nullable type because of a
  // crash on our nightly builds running on an emulator API 27
  // See: https://github.com/kiwix/kiwix-android/issues/2488
  fun providesWiFiP2pManager(context: Context): WifiP2pManager? =
    context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
}
