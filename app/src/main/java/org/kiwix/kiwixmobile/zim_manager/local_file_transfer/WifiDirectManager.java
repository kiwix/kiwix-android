package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import org.kiwix.kiwixmobile.R;

import static android.os.Looper.getMainLooper;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.LocalFileTransferActivity.showToast;


/**
 * Manager for the Wifi-P2p API, used in the local file transfer module
 * */
public class WifiDirectManager implements WifiP2pManager.ChannelListener {

  private static final String TAG = "WifiDirectManager";

  private LocalFileTransferActivity activity;

  /* Variables related to the WiFi P2P API */
  private boolean wifiP2pEnabled = false; // Whether WiFi has been enabled or not
  private boolean retryChannel = false;   // Whether channel has retried connecting previously

  private WifiP2pManager manager;         // Overall manager of Wifi p2p connections for the module
  private WifiP2pManager.Channel channel;
  // Connects the module to device's underlying Wifi p2p framework

  private final IntentFilter intentFilter = new IntentFilter();
  // For specifying broadcasts (of the P2P API) that the module needs to respond to
  private BroadcastReceiver receiver = null; // For receiving the broadcasts given by above filter

  public WifiDirectManager(@NonNull LocalFileTransferActivity activity) {
    this.activity = activity;
  }

  /* Initialisations for using the WiFi P2P API */
  public void initialiseWifiDirectManager() {
    // Intents that the broadcast receiver will be responding to
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    manager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
    channel = manager.initialize(activity, getMainLooper(), null);
  }

  public void registerWifiDirectBroadcastRecevier() {
    receiver = new WifiDirectBroadcastReceiver(manager, channel, activity);
    activity.registerReceiver(receiver, intentFilter);
  }

  public void unregisterWifiDirectBroadcastRecevier() {
    activity.unregisterReceiver(receiver);
  }

  public void discoverPeerDevices() {
    manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
      @Override
      public void onSuccess() {
        showToast(activity, R.string.discovery_initiated,
            Toast.LENGTH_SHORT);
      }

      @Override
      public void onFailure(int reason) {
        String errorMessage = activity.getErrorMessage(reason);
        Log.d(TAG, activity.getString(R.string.discovery_failed) + ": " + errorMessage);
        showToast(activity,
            activity.getString(R.string.discovery_failed),
            Toast.LENGTH_SHORT);
      }
    });
  }

  public void setWifiP2pEnabled(boolean wifiP2pEnabled) {
    this.wifiP2pEnabled = wifiP2pEnabled;
  }

  public boolean isWifiP2pEnabled() {
    return wifiP2pEnabled;
  }

  /* From WifiP2pManager.ChannelListener interface */
  @Override
  public void onChannelDisconnected() {
    // Upon disconnection, retry one more time
    if (manager != null && !retryChannel) {
      Log.d(TAG, "Channel lost, trying again");
      activity.clearPeers();
      retryChannel = true;
      manager.initialize(activity, getMainLooper(), this);
    } else {
      showToast(activity, R.string.severe_loss_error, Toast.LENGTH_LONG);
    }
  }

  public void connect(@NonNull final WifiP2pDevice peerDevice) {
    WifiP2pConfig config = new WifiP2pConfig();
    config.deviceAddress = peerDevice.deviceAddress;
    config.wps.setup = WpsInfo.PBC;

    manager.connect(channel, config, new WifiP2pManager.ActionListener() {
      @Override
      public void onSuccess() {
        // UI updated from broadcast receiver
      }

      @Override
      public void onFailure(int reason) {
        String errorMessage = activity.getErrorMessage(reason);
        Log.d(TAG, activity.getString(R.string.connection_failed) + ": " + errorMessage);
        showToast(activity, activity.getString(R.string.connection_failed),
            Toast.LENGTH_LONG);
      }
    });
  }

  public void closeLocalFileTransferActivity() {
    activity.cancelAsyncTasks();

    activity.fileSendingDevice = false;
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      channel.close();
    }
    disconnect();
  }

  public void disconnect() {
    manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

      @Override
      public void onFailure(int reasonCode) {
        Log.d(TAG, "Disconnect failed. Reason: " + reasonCode);
        closeActivity();
      }

      @Override
      public void onSuccess() {
        Log.d(TAG, "Disconnect successful");
        closeActivity();
      }
    });
  }

  public void closeActivity() {
    activity.finish();
  }
}
