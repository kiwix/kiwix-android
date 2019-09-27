package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helper class for the local file sharing module.
 *
 * Handles the broadcasts pertaining to the wifi p2p group formed in WiFi Direct. Works along with
 * the wifi p2p manager in {@link WifiDirectManager}.
 */
public class KiwixWifiP2pBroadcastReceiver extends BroadcastReceiver {

  private P2pEventListener p2pEventListener;

  public KiwixWifiP2pBroadcastReceiver(@NonNull P2pEventListener p2pEventListener) {
    this.p2pEventListener = p2pEventListener;
  }

  @Override
  public void onReceive(@NonNull Context context, @NonNull Intent intent) {

    switch (intent.getAction()) {
      case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION: {
        int wifiP2pState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
        p2pEventListener.onWifiP2pStateChanged(
          (wifiP2pState == WifiP2pManager.WIFI_P2P_STATE_ENABLED));
        break;
      }

      case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION: {
        p2pEventListener.onPeersChanged();
        break;
      }

      case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION: {
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
        p2pEventListener.onConnectionChanged(networkInfo.isConnected());
        break;
      }

      case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: {
        WifiP2pDevice userDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        p2pEventListener.onDeviceChanged(userDevice);
        break;
      }

      default:
        break;
    }
  }

  public interface P2pEventListener {
    void onWifiP2pStateChanged(boolean isEnabled);

    void onPeersChanged();

    void onConnectionChanged(boolean isConnected);

    void onDeviceChanged(@Nullable WifiP2pDevice userDevice);
  }
}
