package org.kiwix.kiwixmobile.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import org.kiwix.kiwixlib.JNIKiwixLibrary
import org.kiwix.kiwixlib.JNIKiwixServer
import org.kiwix.kiwixmobile.di.ServiceScope
import org.kiwix.kiwixmobile.webserver.WebServerHelper
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
  fun providesWifiHotspotManager(context: Context): WifiHotspotManager = WifiHotspotManager(context)

  @Provides
  @ServiceScope
  fun providesJNIKiwixLibrary(): JNIKiwixLibrary = JNIKiwixLibrary()

  @Provides
  @ServiceScope
  fun providesJNIKiwixServer(jniKiwixLibrary: JNIKiwixLibrary): JNIKiwixServer =
    JNIKiwixServer(jniKiwixLibrary)
}

