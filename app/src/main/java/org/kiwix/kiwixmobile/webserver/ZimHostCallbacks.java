package org.kiwix.kiwixmobile.webserver;

import android.net.wifi.WifiConfiguration;

public interface ZimHostCallbacks {

  void onServerStarted(String ip);

  void onServerStopped();

  void onServerFailedToStart();

  void onHotspotTurnedOn(WifiConfiguration wifiConfiguration);

  void onHotspotFailedToStart();

  void onHotspotStateReceived(Boolean state);

}
