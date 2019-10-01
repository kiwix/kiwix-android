package org.kiwix.kiwixmobile.core.webserver;

import androidx.annotation.NonNull;

public interface ZimHostCallbacks {

  void onServerStarted(@NonNull String ip);

  void onServerStopped();

  void onServerFailedToStart();

  void onIpAddressValid();

  void onIpAddressInvalid();
}
