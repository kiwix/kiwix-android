package org.kiwix.kiwixmobile.wifip2p.broadcast;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import org.kiwix.kiwixmobile.wifip2p.wifip2pclasses.Constant;
import org.kiwix.kiwixmobile.wifip2p.wifip2pclasses.MyPeerListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Rishabh Rawat on 3/5/2018.
 */

public class WifiTxRxBroadcastRecevier extends BroadcastReceiver {

    public static final String TAG = "Wifitxrxbroadcastrecevier";

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private IntentFilter mIntentFilter;
    private String classname;
    private Class classtype;
    private Object object;

    public WifiTxRxBroadcastRecevier(WifiP2pManager mManager, WifiP2pManager.Channel mChannel, String classname) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        this.mManager = mManager;
        this.mChannel = mChannel;
        this.classname = classname;
        classtype = Class.forName(classname);
        object = classtype.newInstance();

    }


    @SuppressLint("LongLogTag")
    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.d(TAG, "WIFI P2P ENABLED");
                //wifiPeerListTransferActivity.setStatusView(Constant.P2P_WIFI_ENABLED);
                methodcall(Constant.P2P_WIFI_ENABLED);
            } else {
                Log.d(TAG, "WIFI P2P NOT ENABLED");
                //wifiPeerListTransferActivity.setStatusView(Constant.P2P_WIFI_DISABLED);
                methodcall(Constant.P2P_WIFI_DISABLED);
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.d(WifiP2PBroadCastReceiver.TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (mManager == null) {
                return;
            }
            NetworkInfo networkInfo = intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                // wifiPeerListTransferActivity.setStatusView(Constant.NETWORK_CONNECT);
                methodcall(Constant.NETWORK_CONNECT);
            } else {
                // It's a disconnect
                Log.d(WifiP2PBroadCastReceiver.TAG, "Its a disconnect");
                //wifiPeerListTransferActivity.setStatusView(Constant.NETWORK_DISCONNECT);
                methodcall(Constant.NETWORK_DISCONNECT);

                //activity.resetData();
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.d(WifiP2PBroadCastReceiver.TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            // Respond to this device's wifi state changing


        } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {

            int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 10000);
            if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
                //wifiPeerListTransferActivity.setStatusView(Constant.DISCOVERY_INITATITED);
                methodcall(Constant.DISCOVERY_INITATITED);
            } else if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
                // wifiPeerListTransferActivity.setStatusView(Constant.DISCOVERY_STOPPED);
                methodcall(Constant.DISCOVERY_STOPPED);
            }

        }

    }

    private void methodcall(int status) {
        try {
            Method method = classtype.getDeclaredMethod("setStatusView", Integer.class);
            method.invoke(object, status);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void setUpIntentFilter() {
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }


}
