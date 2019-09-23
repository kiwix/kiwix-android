package org.kiwix.kiwixmobile.wifi_hotspot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * HotspotStateReceiver is used to listen to the intentional/unintentional changes in
 * the hotspot state and provide callbacks to {@link HotspotService}.
 * Created by Adeel Zafar on Sept 06,2019.
 */

public class HotspotStateReceiver {

  private static final String TAG = "HotspotStateReceiver";
  private BroadcastReceiver hotspotStateReceiver;
  private HotspotStateReceiverCallbacks hotspotStateReceiverCallbacks;

  public HotspotStateReceiver(HotspotStateReceiverCallbacks hotspotStateReceiverCallbacks) {
    this.hotspotStateReceiverCallbacks = hotspotStateReceiverCallbacks;
  }

  public void setUpHotspotStateReceiver() {
    hotspotStateReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)) {
          int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);

          if (WifiManager.WIFI_STATE_ENABLED == state % 10) {
            Log.v(TAG, "Hotspot enabled");
          } else {
            hotspotStateReceiverCallbacks.onHotspotDisabled();
            Log.v(TAG, "Hotspot disabled");
          }
        }
      }
    };

    IntentFilter intentFilter = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
    hotspotStateReceiverCallbacks.registerHotspotStateReceiver(hotspotStateReceiver, intentFilter);
  }
}
