package org.kiwix.kiwixmobile.di.modules

import android.app.Application
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.net.wifi.WifiManager
import dagger.Module
import dagger.Provides
import org.kiwix.kiwixlib.JNIKiwixLibrary
import org.kiwix.kiwixlib.JNIKiwixServer
import org.kiwix.kiwixmobile.di.ServiceScope
import org.kiwix.kiwixmobile.webserver.WebServerHelper
import org.kiwix.kiwixmobile.wifi_hotspot.HotspotNotificationManager
import org.kiwix.kiwixmobile.wifi_hotspot.HotspotStateListener
import org.kiwix.kiwixmobile.wifi_hotspot.HotspotStateReceiver
import org.kiwix.kiwixmobile.wifi_hotspot.HotspotStateReceiverCallbacks
import org.kiwix.kiwixmobile.wifi_hotspot.IpAddressCallbacks
import org.kiwix.kiwixmobile.wifi_hotspot.WifiHotspotManager

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
  fun providesWifiHotspotManager(
    wifiManager: WifiManager,
    hotspotStateListener: HotspotStateListener
  ): WifiHotspotManager =
    WifiHotspotManager(wifiManager, hotspotStateListener)

  @Provides
  @ServiceScope
  fun providesHotspotStateListener(service: Service): HotspotStateListener =
    service as HotspotStateListener

  @Provides
  @ServiceScope
  fun providesIpAddressCallbacks(service: Service): IpAddressCallbacks =
    service as IpAddressCallbacks

  @Provides
  @ServiceScope
  fun providesHotspotStateReceiverCallbacks(service: Service): HotspotStateReceiverCallbacks =
    service as HotspotStateReceiverCallbacks

  @Provides
  @ServiceScope
  fun providesJNIKiwixLibrary(): JNIKiwixLibrary = JNIKiwixLibrary()

  @Provides
  @ServiceScope
  fun providesJNIKiwixServer(jniKiwixLibrary: JNIKiwixLibrary): JNIKiwixServer =
    JNIKiwixServer(jniKiwixLibrary)

  @Provides
  @ServiceScope
  fun providesWifiManager(context: Application): WifiManager =
    context.getSystemService(Context.WIFI_SERVICE) as WifiManager

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
    hotspotStateReceiverCallbacks: HotspotStateReceiverCallbacks
  ): HotspotStateReceiver =
    HotspotStateReceiver(hotspotStateReceiverCallbacks)
}
