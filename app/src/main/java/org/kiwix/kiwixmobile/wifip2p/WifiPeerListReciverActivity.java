package org.kiwix.kiwixmobile.wifip2p;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
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
import org.kiwix.kiwixmobile.wifip2p.broadcast.WifiP2PReciverBroadcastReceiver;
import org.kiwix.kiwixmobile.wifip2p.threads.FileReciverAsyncTask;
import org.kiwix.kiwixmobile.wifip2p.wifip2pclasses.Constant;
import org.kiwix.kiwixmobile.wifip2p.wifip2pclasses.MyPeerListener;
import org.kiwix.kiwixmobile.wifip2p.wifip2pclasses.MyReceiverPeerListener;

import java.util.ArrayList;

import pl.bclogic.pulsator4droid.library.PulsatorLayout;

public class WifiPeerListReciverActivity extends AppCompatActivity {

    private static String TAG="WIFIPEERLISTRECIVERACTIVITY";

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    WifiP2PReciverBroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    WifiP2pDevice device;

    PulsatorLayout pulsatorLayout;
    LinearLayout layout;

    WifiP2pDevice[] deviceListItems;

    static boolean  stateDiscovery = false;
    static boolean stateWifi = false;
    public static boolean stateConnection = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_peer_list_reciver);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WifiP2PReciverBroadcastReceiver(mManager, mChannel, this);
        pulsatorLayout=findViewById(R.id.pulsator);
        layout=findViewById(R.id.layoutpeerlist);
        pulsatorLayout.start();
        discoverPeers();

        pulsatorLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                discoverPeers();
            }
        });


    }

    private void setUpIntentFilter() {
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    private void discoverPeers()
    {
        Log.d(WifiPeerListTransferActivity.TAG,"discoverPeers()");

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            setDeviceList(new ArrayList<WifiP2pDevice>());
            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    stateDiscovery = true;
                    Log.d(WifiPeerListTransferActivity.TAG, "peer discovery started");
                    makeToast("peer discovery started");
                    MyReceiverPeerListener myPeerListener = new MyReceiverPeerListener(WifiPeerListReciverActivity.this);
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
        }else
        {
            makeToast("Your device not supported the wifi data transfer feature...");
        }
    }

    public void setDeviceList(ArrayList<WifiP2pDevice> deviceDetails) {

        deviceListItems = new WifiP2pDevice[deviceDetails.size()];
        String[] deviceNames = new String[deviceDetails.size()];
        for(int i=0 ;i< deviceDetails.size(); i++){
            //now creating the dynamic custom button for showing the peer list
            deviceNames[i] = deviceDetails.get(i).deviceName;
            deviceListItems[i] = deviceDetails.get(i);
            //creating a custom button for showing the peer list device
//            Button button=new Button(WifiPeerListTransferActivity.this);
//            button.setId(i+1);
//            button.setText(deviceDetails.get(i).deviceName);
//            button.setPadding(5,0,5,0);
//            button.setBackgroundResource(R.drawable.dynamic_custom_btn);
//            layout.addView(button);
//            button.setOnClickListener(this);

        }

        // listViewDevices.setAdapter(mAdapter);
    }



    public void setStatusView(int status) {

        switch (status)
        {
            case Constant.DISCOVERY_INITATITED:
                stateDiscovery = true;
                Log.d(TAG,"Discovery initiated");
                break;
            case Constant.DISCOVERY_STOPPED:
                stateDiscovery = false;
                Log.d(TAG,"Discovery stopped");
                break;
            case Constant.P2P_WIFI_DISABLED:
                stateWifi = false;
                Log.d(TAG,"P2P wifi disabled");
                break;
            case Constant.P2P_WIFI_ENABLED:
                stateWifi = true;
                Log.d(TAG,"P2P wifi enabled");
                break;
            case Constant.NETWORK_CONNECT:
                stateConnection = true;
                Snackbar.make(layout,"Peer Device Connected",Snackbar.LENGTH_LONG).show();
                //new FileReciverAsyncTask(8585,getApplicationContext()).execute();
                startActivity(new Intent(WifiPeerListReciverActivity.this,RecevierDataActivity.class));
                Log.d(TAG,"Connected");
                break;
            case Constant.NETWORK_DISCONNECT:
                stateConnection = false;
                Log.d(TAG,"Device Disconnected");
                Snackbar.make(layout,"Peer Device Disconnected",Snackbar.LENGTH_LONG).show();
                break;
            default:
                Log.d(WifiPeerListTransferActivity.TAG,"Unknown status");
                break;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void stopPeerDiscover() {

        //wifi p2p support from
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
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
        }
        else
        {
            makeToast("Your device not supported the Data Transfer feature....");
        }

    }

    public void makeToast(String msg) {
        Toast.makeText(WifiPeerListReciverActivity.this,msg,Toast.LENGTH_SHORT).show();
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
    public void onBackPressed() {
        super.onBackPressed();
        new AlertDialog.Builder(WifiPeerListReciverActivity.this).setTitle(R.string.warnig).setMessage(R.string.warnmessage)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(stateConnection)
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
