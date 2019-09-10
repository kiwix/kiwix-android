/*
 * Copyright (c) 2019 Kiwix
 * All rights reserved.
 */

package org.kiwix.kiwixmobile.webserver;

import android.net.wifi.WifiConfiguration;
import androidx.annotation.NonNull;

public interface ZimHostCallbacks {

  void onServerStarted(@NonNull String ip);

  void onServerStopped();

  void onServerFailedToStart();

  void onHotspotTurnedOn(@NonNull WifiConfiguration wifiConfiguration);

  void onHotspotFailedToStart();

  void requestLocationAccess();

  void onIpAddressValid();

  void onIpAddressInvalid();
}
