package org.kiwix.kiwixmobile.wifip2p.broadcast;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import org.kiwix.kiwixmobile.wifip2p.WifiPeerListReciverActivity;
import org.kiwix.kiwixmobile.wifip2p.WifiPeerListReciverActivity;
import org.kiwix.kiwixmobile.wifip2p.wifip2pclasses.Constant;
import org.kiwix.kiwixmobile.wifip2p.wifip2pclasses.MyPeerListener;
import org.kiwix.kiwixmobile.wifip2p.wifip2pclasses.MyReceiverPeerListener;

public class WifiP2PReciverBroadcastReceiver extends BroadcastReceiver {

    public static final String TAG = "Rishabh WIFI PEER BROADCAST";

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    //this is the activity where we Reciver the file
    private WifiPeerListReciverActivity wifiPeerListReciverActivity;


    public WifiP2PReciverBroadcastReceiver(WifiP2pManager mManager, WifiP2pManager.Channel mChannel, WifiPeerListReciverActivity wifiPeerListReciverActivity) {
        this.mManager = mManager;
        this.mChannel = mChannel;
        this.wifiPeerListReciverActivity = wifiPeerListReciverActivity;
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.d(WifiP2PReciverBroadcastReceiver.TAG, "WIFI P2P ENABLED");
                wifiPeerListReciverActivity.setStatusView(Constant.P2P_WIFI_ENABLED);
            } else {
                //Log.d(WifiPeerListReciverActivity.TAG, "WIFI P2P NOT ENABLED");
                wifiPeerListReciverActivity.setStatusView(Constant.P2P_WIFI_DISABLED);
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.d(WifiP2PReciverBroadcastReceiver.TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
            if (mManager != null) {
                MyReceiverPeerListener myPeerListener = new MyReceiverPeerListener(wifiPeerListReciverActivity);
                mManager.requestPeers(mChannel, myPeerListener);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (mManager == null) {
                return;
            }
            NetworkInfo networkInfo = intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                wifiPeerListReciverActivity.setStatusView(Constant.NETWORK_CONNECT);
            } else {
                // It's a disconnect
                Log.d(WifiP2PReciverBroadcastReceiver.TAG, "Its a disconnect");
                wifiPeerListReciverActivity.setStatusView(Constant.NETWORK_DISCONNECT);
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.d(WifiP2PReciverBroadcastReceiver.TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            // Respond to this device's wifi state changing


        } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {

            int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 10000);
            if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
                wifiPeerListReciverActivity.setStatusView(Constant.DISCOVERY_INITATITED);
            } else if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
                wifiPeerListReciverActivity.setStatusView(Constant.DISCOVERY_STOPPED);
            }

        }
    }


}

