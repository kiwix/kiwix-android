package org.kiwix.kiwixmobile.wifip2p;

import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.github.lzyzsd.circleprogress.ArcProgress;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.wifip2p.broadcast.WifiTxRxBroadcastRecevier;
import org.kiwix.kiwixmobile.wifip2p.threads.FileReciverAsyncTask;
import org.kiwix.kiwixmobile.wifip2p.threads.FileTransmitterAsyncTask;
import org.kiwix.kiwixmobile.wifip2p.wifip2pclasses.Constant;

public class RecevierDataActivity extends AppCompatActivity {

    static  String TAG="RecevierDataActivity";
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiTxRxBroadcastRecevier mReceiver;
    private IntentFilter mIntentFilter;
    private ConstraintLayout layout;
    private ArcProgress arcProgress;
    private FileTransmitterAsyncTask task;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;


    static boolean stateDiscovery = false;
    static boolean stateWifi = false;
    public static boolean stateConnection = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recevier_data);

        powerManager=(PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock=powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Screen Wake Lock");
        wakeLock.acquire();

        arcProgress=findViewById(R.id.arc_progress);
        layout=findViewById(R.id.recevilayout);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        try {
            mReceiver = new WifiTxRxBroadcastRecevier(mManager, mChannel,"org.kiwix.kiwixmobile.wifip2p.RecevierDataActivity");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.d("Rishabh","class not found exception");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Log.d("Rishabh","illegalstateexception");
        } catch (InstantiationException e) {
            e.printStackTrace();
            Log.d("Rishabh","Instantiationexception");
        }

        //start the async task
        new FileReciverAsyncTask(9090,RecevierDataActivity.this,arcProgress);

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
                canceltask();
                break;
            case Constant.P2P_WIFI_ENABLED:
                stateWifi = true;
                Log.d(TAG, "P2P wifi enabled");
                break;
            case Constant.NETWORK_CONNECT:
                stateConnection = true;
                Snackbar.make(layout, "Peer Device Connected", Snackbar.LENGTH_LONG).show();
                Log.d(TAG, "Connected");
                break;
            case Constant.NETWORK_DISCONNECT:
                stateConnection = false;
                Log.d(TAG, "Device Disconnected");
                Snackbar.make(layout, "Peer Device Disconnected", Snackbar.LENGTH_LONG).show();
                //if device disconnected the stop the data transfer
                canceltask();
                break;
            default:
                Log.d(WifiPeerListTransferActivity.TAG, "Unknown status");
                break;
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
        new AlertDialog.Builder(RecevierDataActivity.this).setTitle(R.string.warnig).setMessage(R.string.warnmessage1)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (task!=null)
                            task.cancel(true);
                        finish();
                    }
                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        }).show();
    }

    private void canceltask()
    {

        if (task != null) {
            if (task.getStatus() == AsyncTask.Status.RUNNING) {
                task.cancel(true);

                if (task.isCancelled()) {
                    arcProgress.setBackgroundColor(Color.RED);
                    layout.setBackgroundColor(Color.RED);
                    arcProgress.setBottomText("Failed");
                    arcProgress.setProgress(0);
                    wakeLock.release();
                }
            }
        }

    }

}
