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

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import dagger.Module
import dagger.Provides
import org.kiwix.kiwixlib.JNIKiwixLibrary
import org.kiwix.kiwixlib.JNIKiwixServer
import org.kiwix.kiwixmobile.core.di.ServiceScope
import org.kiwix.kiwixmobile.webserver.WebServerHelper
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.HotspotNotificationManager
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.HotspotStateReceiver
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.HotspotStateReceiver.Callback
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.IpAddressCallbacks

@Module
class ServiceModule {

  @Provides
  @ServiceScope
  fun providesWebServerHelper(
    jniKiwixLibrary: JNIKiwixLibrary,
    kiwixServer: JNIKiwixServer,
    ipAddressCallbacks: IpAddressCallbacks
  ): WebServerHelper = WebServerHelper(jniKiwixLibrary, kiwixServer, ipAddressCallbacks)

  @Provides
  @ServiceScope
  fun providesIpAddressCallbacks(service: Service): IpAddressCallbacks =
    service as IpAddressCallbacks

  @Provides
  @ServiceScope
  fun providesJNIKiwixLibrary(): JNIKiwixLibrary = JNIKiwixLibrary()

  @Provides
  @ServiceScope
  fun providesJNIKiwixServer(jniKiwixLibrary: JNIKiwixLibrary): JNIKiwixServer =
    JNIKiwixServer(jniKiwixLibrary)

  @Provides
  @ServiceScope
  fun providesHotspotNotificationManager(
    notificationManager: NotificationManager,
    context: Context
  ): HotspotNotificationManager = HotspotNotificationManager(notificationManager, context)

  @Provides
  @ServiceScope
  fun providesHotspotStateReceiver(callback: Callback): HotspotStateReceiver =
    HotspotStateReceiver(callback)

  @Provides
  @ServiceScope
  fun providesHotspotStateReceiverCallback(service: Service): HotspotStateReceiver.Callback =
    service as Callback
}
