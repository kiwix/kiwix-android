package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helper class for the local file sharing module.
 *
 * Handles the broadcasts pertaining to the wifi p2p group formed in WiFi Direct. Works along with
 * the wifi p2p manager in {@link LocalFileTransferActivity}.
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

  private WifiP2pManager manager;
  private WifiP2pManager.Channel channel;
  private WifiDirectManager wifiDirectManager;

  public WifiDirectBroadcastReceiver(@NonNull WifiP2pManager manager, @NonNull WifiP2pManager.Channel channel,
      @NonNull WifiDirectManager wifiDirectManager) {
    super();
    this.manager = manager;
    this.channel = channel;
    this.wifiDirectManager = wifiDirectManager;
  }

  @Override
  public void onReceive(@NonNull Context context, @NonNull Intent intent) {
    String action = intent.getAction();

    if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
      // Update wifi p2p state
      int wifiP2pState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

      if (wifiP2pState == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
        ((BroadcastListener) wifiDirectManager).setWifiP2pEnabled(true);
      } else {
        ((BroadcastListener) wifiDirectManager).setWifiP2pEnabled(false);
      }
      Log.d(LocalFileTransferActivity.TAG, "WiFi P2P state changed - " + wifiP2pState);


    } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
      if (manager != null) {
        /* List of available peers has changed, so request & use the new list through
         * PeerListListener.requestPeers() callback */
        manager.requestPeers(channel, wifiDirectManager);
      }
      Log.d(LocalFileTransferActivity.TAG, "P2P peers changed");


    } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
      if (manager == null) {
        return;
      }
      NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

      if (networkInfo.isConnected()) {
        // Request connection info about the wifi p2p group formed upon connection
        manager.requestConnectionInfo(channel, wifiDirectManager);
      } else {
        // Not connected after connection change -> Disconnected
        ((BroadcastListener) wifiDirectManager).onDisconnected();
      }


    } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
      ((BroadcastListener) wifiDirectManager).onDeviceChanged(intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
    }
  }

  public interface BroadcastListener {
    void setWifiP2pEnabled(boolean wifiP2pEnabled);

    void onDisconnected();

    void onDeviceChanged(@Nullable WifiP2pDevice userDevice);
  }
}
