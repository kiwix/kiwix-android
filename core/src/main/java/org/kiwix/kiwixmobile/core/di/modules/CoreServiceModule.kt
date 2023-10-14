/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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
import android.app.Service
import android.content.Context
import dagger.Module
import dagger.Provides
import org.kiwix.kiwixmobile.core.di.CoreServiceScope
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudNotificationManger
import org.kiwix.kiwixmobile.core.webserver.KiwixServer
import org.kiwix.kiwixmobile.core.webserver.WebServerHelper
import org.kiwix.kiwixmobile.core.webserver.wifi_hotspot.HotspotNotificationManager
import org.kiwix.kiwixmobile.core.webserver.wifi_hotspot.HotspotStateReceiver
import org.kiwix.kiwixmobile.core.webserver.wifi_hotspot.IpAddressCallbacks

@Module
class CoreServiceModule {
  @Provides
  @CoreServiceScope
  fun providesReadAloudNotificationManager(
    notificationManager: NotificationManager,
    context: Context
  ): ReadAloudNotificationManger = ReadAloudNotificationManger(notificationManager, context)

  @Provides
  @CoreServiceScope
  fun providesWebServerHelper(
    kiwixServerFactory: KiwixServer.Factory,
    ipAddressCallbacks: IpAddressCallbacks
  ): WebServerHelper = WebServerHelper(kiwixServerFactory, ipAddressCallbacks)

  @Provides
  @CoreServiceScope
  fun providesIpAddressCallbacks(service: Service): IpAddressCallbacks =
    service as IpAddressCallbacks

  @Provides
  @CoreServiceScope
  fun providesHotspotNotificationManager(
    notificationManager: NotificationManager,
    context: Context
  ): HotspotNotificationManager = HotspotNotificationManager(notificationManager, context)

  @Provides
  @CoreServiceScope
  fun providesHotspotStateReceiver(callback: HotspotStateReceiver.Callback): HotspotStateReceiver =
    HotspotStateReceiver(callback)

  @Provides
  @CoreServiceScope
  fun providesHotspotStateReceiverCallback(service: Service): HotspotStateReceiver.Callback =
    service as HotspotStateReceiver.Callback
}
