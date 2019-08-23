package org.kiwix.kiwixmobile.wifi_hotspot;

import android.net.wifi.WifiConfiguration;
import androidx.annotation.NonNull;

public interface HotspotStateListener {
  void onHotspotTurnedOn(@NonNull WifiConfiguration wifiConfiguration);

  void onHotspotFailedToStart();

  void onHotspotStopped();
}
