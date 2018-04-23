package org.kiwix.kiwixmobile.wifip2p.wifip2pclasses;

import android.annotation.SuppressLint;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import org.kiwix.kiwixmobile.wifip2p.WifiPeerListReciverActivity;
import org.kiwix.kiwixmobile.wifip2p.WifiPeerListTransferActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rishabh Rawat on 3/5/2018.
 */

public class MyReceiverPeerListener implements WifiP2pManager.PeerListListener {
    public static final String TAG = "Rishabh My Peer Listener";

    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    public WifiPeerListReciverActivity activity;

    @SuppressLint("LongLogTag")
    public MyReceiverPeerListener(WifiPeerListReciverActivity activity) {
        this.activity = activity;
        Log.d(MyPeerListener.TAG,"MyPeerListener object created");

    }


    @SuppressLint("LongLogTag")
    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {

        ArrayList<WifiP2pDevice> deviceDetails = new ArrayList<>();

        Log.d(MyReceiverPeerListener.TAG, "OnPeerAvailable()");
        if(wifiP2pDeviceList != null ) {

            if(wifiP2pDeviceList.getDeviceList().size() == 0) {
                Log.d(MyReceiverPeerListener.TAG, "wifiP2pDeviceList size is zero");
                return;
            }

            for (WifiP2pDevice device : wifiP2pDeviceList.getDeviceList()) {
                deviceDetails.add(device);
                Log.d(MyPeerListener.TAG, "Found device :" + device.deviceName + " " + device.deviceAddress);
            }
            if(activity != null) {
                activity.setDeviceList(deviceDetails);
            }

        }
        else {
            Log.d(MyPeerListener.TAG, "wifiP2pDeviceList is null");
        }

    }
}
