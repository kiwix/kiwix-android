package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;

import org.kiwix.kiwixmobile.R;

public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

  private WifiP2pManager manager;
  private WifiP2pManager.Channel channel;
  private LocalFileTransferActivity wifiActivity;

  public WifiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, LocalFileTransferActivity activity) {
    super();
    this.manager = manager;
    this.channel = channel;
    this.wifiActivity = activity;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();

    if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

      int wifiP2pState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
      if(wifiP2pState == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
        wifiActivity.setWifiP2pEnabled(true);
      } else {
        wifiActivity.setWifiP2pEnabled(false);
        Toast.makeText(wifiActivity, "Cannot discover peers without WiFi", Toast.LENGTH_SHORT).show();
        //TODO
        wifiActivity.resetPeers();
      }
      Log.d(wifiActivity.TAG, "WiFi P2P state changed - " + wifiP2pState);

    } else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

      if(manager != null) {
        //TODO
        manager.requestPeers(channel, (WifiP2pManager.PeerListListener) wifiActivity.getSupportFragmentManager().findFragmentById(R.id.fragment_device_list));
      }
      Log.d(wifiActivity.TAG, "P2P peers changed");

    } else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

      if(manager == null) {
        return;
      }

      NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

      if(networkInfo.isConnected()) {
        //TODO
        //

        manager.requestConnectionInfo(channel, (DeviceListFragment) wifiActivity.getSupportFragmentManager().findFragmentById(R.id.fragment_device_list));
      } else {
        wifiActivity.resetData();
      }

    } else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
      //TODO
      DeviceListFragment deviceListFragment = (DeviceListFragment) wifiActivity.getSupportFragmentManager().findFragmentById(R.id.fragment_device_list);
      deviceListFragment.updateUserDevice((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
    }
  }
}
