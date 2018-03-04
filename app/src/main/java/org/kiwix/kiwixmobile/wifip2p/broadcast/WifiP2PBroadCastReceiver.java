package org.kiwix.kiwixmobile.wifip2p.broadcast;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import org.kiwix.kiwixmobile.wifip2p.WifiPeerListTransferActivity;
import org.kiwix.kiwixmobile.wifip2p.wifip2pclasses.Constant;
import org.kiwix.kiwixmobile.wifip2p.wifip2pclasses.MyPeerListener;

/**
 * Created by Rishabh Rawat on 3/3/2018.
 */


public class WifiP2PBroadCastReceiver extends BroadcastReceiver {

    public static final String TAG = "Rishabh WIFI PEER BROADCAST";

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    //this is the activity where we transfer the file
    private WifiPeerListTransferActivity wifiPeerListTransferActivity;

    public WifiP2PBroadCastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                    WifiPeerListTransferActivity wifiPeerListTransferActivity) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.wifiPeerListTransferActivity = wifiPeerListTransferActivity;
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.d(WifiP2PBroadCastReceiver.TAG, "WIFI P2P ENABLED");
                wifiPeerListTransferActivity.setStatusView(Constant.P2P_WIFI_ENABLED);
            } else {
                Log.d(wifiPeerListTransferActivity.TAG, "WIFI P2P NOT ENABLED");
                wifiPeerListTransferActivity.setStatusView(Constant.P2P_WIFI_DISABLED);
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.d(WifiP2PBroadCastReceiver.TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
            if (mManager != null) {
                MyPeerListener myPeerListener = new MyPeerListener(wifiPeerListTransferActivity);
                mManager.requestPeers(mChannel, myPeerListener);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (mManager == null) {
                return;
            }
            NetworkInfo networkInfo = intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            //WifiP2pInfo p2pInfo = intent
            //        .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);

            //if (p2pInfo != null && p2pInfo.groupOwnerAddress != null) {
            //    String goAddress = Utils.getDottedDecimalIP(p2pInfo.groupOwnerAddress
            //            .getAddress());
            //    boolean isGroupOwner = p2pInfo.isGroupOwner;
            //     Log.d(WifiBroadcastReceiver.TAG,"I am a group owner");
            // }
            if (networkInfo.isConnected()) {
                wifiPeerListTransferActivity.setStatusView(Constant.NETWORK_CONNECT);
                // we are connected with the other device, request connection
                // info to find group owner IP
                //mManager.requestConnectionInfo(mChannel, mActivity);
            } else {
                // It's a disconnect
                Log.d(WifiP2PBroadCastReceiver.TAG, "Its a disconnect");
                wifiPeerListTransferActivity.setStatusView(Constant.NETWORK_DISCONNECT);

                //activity.resetData();
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.d(WifiP2PBroadCastReceiver.TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            // Respond to this device's wifi state changing


        } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {

            int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 10000);
            if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
                wifiPeerListTransferActivity.setStatusView(Constant.DISCOVERY_INITATITED);
            } else if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
                wifiPeerListTransferActivity.setStatusView(Constant.DISCOVERY_STOPPED);
            }


        }
    }
}
