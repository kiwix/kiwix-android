package org.kiwix.kiwixmobile.core.di.modules

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import dagger.Module
import dagger.Provides
import org.kiwix.kiwixlib.JNIKiwixLibrary
import org.kiwix.kiwixlib.JNIKiwixServer
import org.kiwix.kiwixmobile.core.di.ServiceScope
import org.kiwix.kiwixmobile.core.webserver.WebServerHelper
import org.kiwix.kiwixmobile.core.wifi_hotspot.HotspotNotificationManager
import org.kiwix.kiwixmobile.core.wifi_hotspot.HotspotStateReceiver
import org.kiwix.kiwixmobile.core.wifi_hotspot.HotspotStateReceiver.Callback
import org.kiwix.kiwixmobile.core.wifi_hotspot.IpAddressCallbacks

@Module
class ServiceModule {

  @Provides
  @ServiceScope
  fun providesWebServerHelper(
    jniKiwixLibrary: JNIKiwixLibrary,
    kiwixServer: JNIKiwixServer,
    ipAddressCallbacks: IpAddressCallbacks
  ): WebServerHelper =
    WebServerHelper(
      jniKiwixLibrary,
      kiwixServer,
      ipAddressCallbacks
    )

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
  ): HotspotNotificationManager =
    HotspotNotificationManager(notificationManager, context)

  @Provides
  @ServiceScope
  fun providesHotspotStateReceiver(
    callback: Callback
  ): HotspotStateReceiver =
    HotspotStateReceiver(callback)

  @Provides
  @ServiceScope
  fun providesHotspotStateReceiverCallback(
    service: Service
  ): HotspotStateReceiver.Callback = service as Callback
}
