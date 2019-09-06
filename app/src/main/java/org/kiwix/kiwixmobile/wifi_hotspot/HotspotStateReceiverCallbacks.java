package org.kiwix.kiwixmobile.wifi_hotspot;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.annotation.NonNull;

public interface HotspotStateReceiverCallbacks {

  void onHotspotDisabled();

  void registerHotspotStateReceiver(@NonNull BroadcastReceiver broadcastReceiver,
      @NonNull IntentFilter intentFilter);
}
