package org.kiwix.kiwixmobile.wifip2p;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.wifip2p.broadcast.WifiP2PBroadCastReceiver;
import org.kiwix.kiwixmobile.wifip2p.threads.FileTransmitterAsyncTask;
import org.kiwix.kiwixmobile.wifip2p.wifip2pclasses.Constant;
import org.kiwix.kiwixmobile.wifip2p.wifip2pclasses.MyPeerListener;

import java.util.ArrayList;

import pl.bclogic.pulsator4droid.library.PulsatorLayout;

/**
 * Created by Rishabh Rawat on 3/3/2018.
 */


public class WifiPeerListTransferActivity extends AppCompatActivity implements View.OnClickListener, WifiP2pManager.ConnectionInfoListener {

    public static String TAG = "Rishabh WIFI PEERLIST TRANSFER ACTIVITY";
    private static String filePath;

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    WifiP2PBroadCastReceiver mReceiver;
    IntentFilter mIntentFilter;
    WifiP2pDevice device;

    static boolean stateDiscovery = false;
    static boolean stateWifi = false;
    public static boolean stateConnection = false;

    WifiP2pDevice[] deviceListItems;
    ArrayAdapter mAdapter;

    public static String IP = null;
    public static boolean IS_OWNER = false;

    PulsatorLayout pulsatorLayout;
    LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_peer_list_transfer);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WifiP2PBroadCastReceiver(mManager, mChannel, this);
        pulsatorLayout = findViewById(R.id.pulsator);
        layout = findViewById(R.id.layoutpeerlist);

        Intent intent = this.getIntent();
        filePath = intent.getStringExtra("FILEPATH");
        pulsatorLayout.start();
        discoverPeers();


        pulsatorLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cleanlayout();
                discoverPeers();
            }
        });
    }

    public void cleanlayout() {
        layout.removeAllViews();
    }

    private void setUpIntentFilter() {
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    private void discoverPeers() {
        Log.d(WifiPeerListTransferActivity.TAG, "discoverPeers()");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            setDeviceList(new ArrayList<WifiP2pDevice>());
            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    stateDiscovery = true;
                    Log.d(WifiPeerListTransferActivity.TAG, "peer discovery started");
                    makeToast("peer discovery started");
                    MyPeerListener myPeerListener = new MyPeerListener(WifiPeerListTransferActivity.this);
                    mManager.requestPeers(mChannel, myPeerListener);

                }

                @Override
                public void onFailure(int i) {
                    stateDiscovery = false;
                    if (i == WifiP2pManager.P2P_UNSUPPORTED) {
                        Log.d(WifiPeerListTransferActivity.TAG, " peer discovery failed :" + "P2P_UNSUPPORTED");
                        makeToast(" peer discovery failed :" + "P2P_UNSUPPORTED");

                    } else if (i == WifiP2pManager.ERROR) {
                        Log.d(WifiPeerListTransferActivity.TAG, " peer discovery failed :" + "ERROR");
                        makeToast(" peer discovery failed :" + "ERROR");

                    } else if (i == WifiP2pManager.BUSY) {
                        Log.d(WifiPeerListTransferActivity.TAG, " peer discovery failed :" + "BUSY");
                        makeToast(" peer discovery failed :" + "BUSY");
                    }
                }
            });
        } else {
            makeToast("Your device not supported the wifi data transfer feature...");
            nothavewififeature();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void stopPeerDiscover() {

        //wifi p2p support from
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    stateDiscovery = false;
                    Log.d(WifiPeerListTransferActivity.TAG, "Peer Discovery stopped");
                    makeToast("Peer Discovery stopped");
                    //buttonDiscoveryStop.setEnabled(false);

                }

                @Override
                public void onFailure(int i) {
                    Log.d(WifiPeerListTransferActivity.TAG, "Stopping Peer Discovery failed");
                    makeToast("Stopping Peer Discovery failed");
                    //buttonDiscoveryStop.setEnabled(true);

                }
            });
        } else {
            nothavewififeature();
            makeToast("Your device not supported the Data Transfer feature....");
        }

    }


    public void connect(final WifiP2pDevice device) {
        // Picking the first device found on the network.

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        Log.d(WifiPeerListTransferActivity.TAG, "Trying to connect : " + device.deviceName);
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(WifiPeerListTransferActivity.TAG, "Connected to :" + device.deviceName);
                Toast.makeText(getApplication(), "Connection successful with " + device.deviceName, Toast.LENGTH_SHORT).show();
                //setDeviceList(new ArrayList<WifiP2pDevice>());
                Snackbar.make(layout, "Connected with " + device.deviceName, Snackbar.LENGTH_LONG).show();


                //if the connection succssfull then transfer the data from the bundle intent which is file path
                Intent intent=new Intent(WifiPeerListTransferActivity.this,TransmitDataActivity.class);
                intent.putExtra("TXFILEPATH",filePath);
                intent.putExtra("IPADDRESS",IP);
                startActivity(intent);
            }

            @Override
            public void onFailure(int reason) {
                if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                    Log.d(WifiPeerListTransferActivity.TAG, "P2P_UNSUPPORTED");
                    makeToast("Failed establishing connection: " + "P2P_UNSUPPORTED");
                } else if (reason == WifiP2pManager.ERROR) {
                    Log.d(WifiPeerListTransferActivity.TAG, "Conneciton falied : ERROR");
                    makeToast("Failed establishing connection: " + "ERROR");

                } else if (reason == WifiP2pManager.BUSY) {
                    Log.d(WifiPeerListTransferActivity.TAG, "Conneciton falied : BUSY");
                    makeToast("Failed establishing connection: " + "BUSY");

                }
            }
        });
    }

    public void setDeviceList(ArrayList<WifiP2pDevice> deviceDetails) {

        deviceListItems = new WifiP2pDevice[deviceDetails.size()];
        String[] deviceNames = new String[deviceDetails.size()];
        for (int i = 0; i < deviceDetails.size(); i++) {
            //now creating the dynamic custom button for showing the peer list
            deviceNames[i] = deviceDetails.get(i).deviceName;
            deviceListItems[i] = deviceDetails.get(i);
            //creating a custom button for showing the peer list device
            Button button = new Button(WifiPeerListTransferActivity.this);
            button.setId(i + 1);
            button.setText(deviceDetails.get(i).deviceName);
            button.setPadding(5, 0, 5, 0);
            button.setBackgroundResource(R.drawable.dynamic_custom_btn);
            layout.addView(button);
            button.setOnClickListener(this);

        }
        mAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, android.R.id.text1, deviceNames);


        // listViewDevices.setAdapter(mAdapter);
    }

    public void setStatusView(int status) {

        switch (status) {
            case Constant.DISCOVERY_INITATITED:
                stateDiscovery = true;
                Log.d(TAG, "Discovery initiated");
                break;
            case Constant.DISCOVERY_STOPPED:
                stateDiscovery = false;
                Log.d(TAG, "Discovery stopped");
                break;
            case Constant.P2P_WIFI_DISABLED:
                stateWifi = false;
                Log.d(TAG, "P2P wifi disabled");
                break;
            case Constant.P2P_WIFI_ENABLED:
                stateWifi = true;
                Log.d(TAG, "P2P wifi enabled");
                break;
            case Constant.NETWORK_CONNECT:
                stateConnection = true;
                Snackbar.make(layout, "Peer Device Connected", Snackbar.LENGTH_LONG).show();
                Log.d(TAG, "Connected");
                cleanlayout();
                break;
            case Constant.NETWORK_DISCONNECT:
                stateConnection = false;
                Log.d(TAG, "Device Disconnected");
                cleanlayout();
                Snackbar.make(layout, "Peer Device Disconnected", Snackbar.LENGTH_LONG).show();
                break;
            default:
                Log.d(WifiPeerListTransferActivity.TAG, "Unknown status");
                break;
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        String hostAddress = wifiP2pInfo.groupOwnerAddress.getHostAddress();
        if (hostAddress == null) hostAddress = "host is null";

        //makeToast("Am I group owner : " + String.valueOf(wifiP2pInfo.isGroupOwner));
        //makeToast(hostAddress);
        Log.d(WifiPeerListTransferActivity.TAG, "wifiP2pInfo.groupOwnerAddress.getHostAddress() " + wifiP2pInfo.groupOwnerAddress.getHostAddress());
        IP = wifiP2pInfo.groupOwnerAddress.getHostAddress();
        IS_OWNER = wifiP2pInfo.isGroupOwner;

        makeToast("Configuration Completed");
    }

    public void makeToast(String msg) {
        Toast.makeText(WifiPeerListTransferActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpIntentFilter();
        registerReceiver(mReceiver, mIntentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        Toast.makeText(this, "Button call " + id, Toast.LENGTH_SHORT).show();

        //not working
//        for(int i=1;i<=deviceListItems.length-1;i++)
//        {
//            if(id==i)
//            {
//                Toast.makeText(this, "Button Click"+deviceListItems[i].deviceName, Toast.LENGTH_SHORT).show();
//                connect(deviceListItems[i]);
//
//            }
//        }

        if (id == 1) {
            connect(deviceListItems[0]);
        } else if (id == 2) {
            connect(deviceListItems[1]);
        } else if (id == 3) {
            connect(deviceListItems[2]);
        } else if (id == 4) {
            connect(deviceListItems[3]);
        } else if (id == 5) {
            connect(deviceListItems[4]);
        } else if (id == 6) {
            connect(deviceListItems[5]);
        } else if (id == 7) {
            connect(deviceListItems[6]);
        } else if (id == 8) {
            connect(deviceListItems[7]);
        } else if (id == 9) {
            connect(deviceListItems[8]);
        } else if (id == 10) {
            connect(deviceListItems[9]);
        } else if (id == 11) {
            connect(deviceListItems[10]);
        }
        cleanlayout();
    }

    private void nothavewififeature() {
        pulsatorLayout.stop();
        Snackbar.make(pulsatorLayout, "Your device not supported the Wifi P2P feature", Snackbar.LENGTH_INDEFINITE).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        new AlertDialog.Builder(WifiPeerListTransferActivity.this).setTitle(R.string.warnig).setMessage(R.string.warnmessage1)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (stateConnection)
                            stopPeerDiscover();

                        finish();
                    }
                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        }).show();
    }
}
