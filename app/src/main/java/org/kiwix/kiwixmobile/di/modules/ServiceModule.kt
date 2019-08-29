package org.kiwix.kiwixmobile.di.modules

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.net.wifi.WifiManager
import dagger.Module
import dagger.Provides
import org.kiwix.kiwixlib.JNIKiwixLibrary
import org.kiwix.kiwixlib.JNIKiwixServer
import org.kiwix.kiwixmobile.di.ServiceScope
import org.kiwix.kiwixmobile.webserver.WebServerHelper
import org.kiwix.kiwixmobile.wifi_hotspot.HotspotNotificationManager
import org.kiwix.kiwixmobile.wifi_hotspot.WifiHotspotManager

@Module
class ServiceModule {

  @Provides
  @ServiceScope
  fun providesWebServerHelper(
    jniKiwixLibrary: JNIKiwixLibrary,
    kiwixServer: JNIKiwixServer
  ): WebServerHelper = WebServerHelper(jniKiwixLibrary, kiwixServer)

  @Provides
  @ServiceScope
  fun providesWifiHotspotManager(wifiManager: WifiManager): WifiHotspotManager =
    WifiHotspotManager(wifiManager)

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
  ):
      HotspotNotificationManager = HotspotNotificationManager(notificationManager, context)
}

