package org.kiwix.kiwixmobile.webserver;

import android.net.wifi.WifiConfiguration;

public interface ServerStateListener {
  
  void serverStarted(String ip);

  void serverStopped();

  void hotspotTurnedOn(WifiConfiguration wifiConfiguration);

  void hotspotFailed();

  void hotspotState(Boolean state);

}
