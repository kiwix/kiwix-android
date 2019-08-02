package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.net.InetAddress;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.utils.AlertDialogShower;
import org.kiwix.kiwixmobile.utils.KiwixDialog;

import static android.os.Looper.getMainLooper;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.LocalFileTransferActivity.showToast;


/**
 * Manager for the Wifi-P2p API, used in the local file transfer module
 * */
public class WifiDirectManager implements WifiP2pManager.ChannelListener, WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener,
    KiwixWifiP2pBroadcastReceiver.P2pEventListener {

  private static final String TAG = "WifiDirectManager";
  public static int FILE_TRANSFER_PORT = 8008;

  @NonNull LocalFileTransferActivity activity;

  /* Variables related to the WiFi P2P API */
  private boolean wifiP2pEnabled = false; // Whether WiFi has been enabled or not
  private boolean retryChannel = false;   // Whether channel has retried connecting previously

  private WifiP2pManager manager;         // Overall manager of Wifi p2p connections for the module
  private WifiP2pManager.Channel channel;
  // Connects the module to device's underlying Wifi p2p framework

  private BroadcastReceiver receiver = null; // For receiving the broadcasts given by above filter

  private WifiP2pDevice userDevice;   // Represents the device on which the app is running
  private WifiP2pInfo groupInfo;      // Corresponds to P2P group formed between the two devices

  private WifiP2pDevice senderSelectedPeerDevice = null;
  private AlertDialogShower alertDialogShower;

  public WifiDirectManager(@NonNull LocalFileTransferActivity activity) {
    this.activity = activity;
  }

  /* Initialisations for using the WiFi P2P API */
  public void createWifiDirectManager(@NonNull AlertDialogShower alertDialogShower) {
    this.alertDialogShower = alertDialogShower;

    manager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
    channel = manager.initialize(activity, getMainLooper(), null);
  }

  public void registerWifiDirectBroadcastRecevier() {
    receiver = new KiwixWifiP2pBroadcastReceiver(manager, channel, this);

    // For specifying broadcasts (of the P2P API) that the module needs to respond to
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

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
        String errorMessage = getErrorMessage(reason);
        Log.d(TAG, activity.getString(R.string.discovery_failed) + ": " + errorMessage);
        showToast(activity,
            activity.getString(R.string.discovery_failed),
            Toast.LENGTH_SHORT);
      }
    });
  }

  @Override
  public void setWifiP2pEnabled(boolean wifiP2pEnabled) {
    this.wifiP2pEnabled = wifiP2pEnabled;

    if(wifiP2pEnabled == false) {
      showToast(activity, R.string.discovery_needs_wifi, Toast.LENGTH_SHORT);
      activity.clearPeers();
    }
  }

  @Override
  public void onDisconnected() {
    activity.clearPeers();
  }

  @Override
  public void onDeviceChanged(@Nullable WifiP2pDevice userDevice) {
    // Update UI with wifi-direct details about the user device
    activity.updateUserDevice(userDevice);
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

  /* From WifiP2pManager.PeerListListener callback-interface */
  @Override
  public void onPeersAvailable(@NonNull WifiP2pDeviceList peers) {
    ((Callbacks) activity).updatePeerDevicesList(peers);
  }

  /* From WifiP2pManager.ConnectionInfoListener callback-interface */
  @Override
  public void onConnectionInfoAvailable(@NonNull WifiP2pInfo groupInfo) {
    /* Devices have successfully connected, and 'info' holds information about the wifi p2p group formed */
    setGroupInfo(groupInfo);
    ((Callbacks) activity).performHandshakeWithSelectedPeerDevice(groupInfo);
  }

  public void setUserDevice(@NonNull WifiP2pDevice userDevice) {
    this.userDevice = userDevice;
  }

  public void setGroupInfo(@NonNull WifiP2pInfo groupInfo) {
    this.groupInfo = groupInfo;
  }

  public boolean isGroupFormed() {
    return groupInfo.groupFormed;
  }

  public boolean isGroupOwner() {
    return groupInfo.isGroupOwner;
  }

  public @NonNull InetAddress getGroupOwnerAddress() {
    return groupInfo.groupOwnerAddress;
  }

  public void sendToDevice(@NonNull WifiP2pDevice senderSelectedPeerDevice) {
    this.senderSelectedPeerDevice = senderSelectedPeerDevice;

    alertDialogShower.show(
        new KiwixDialog.FileTransferConfirmation(senderSelectedPeerDevice.deviceName),
        new Function0<Unit>() {
          @Override public Unit invoke() {
            connect();
            showToast(activity, R.string.performing_handshake, Toast.LENGTH_LONG);
            return Unit.INSTANCE;
          }
        });
  }

  public void connect() {
    if(senderSelectedPeerDevice == null) {
      Log.d(TAG, "No device set as selected");
    }

    WifiP2pConfig config = new WifiP2pConfig();
    config.deviceAddress = senderSelectedPeerDevice.deviceAddress;
    config.wps.setup = WpsInfo.PBC;

    manager.connect(channel, config, new WifiP2pManager.ActionListener() {
      @Override
      public void onSuccess() {
        // UI updated from broadcast receiver
      }

      @Override
      public void onFailure(int reason) {
        String errorMessage = getErrorMessage(reason);
        Log.d(TAG, activity.getString(R.string.connection_failed) + ": " + errorMessage);
        showToast(activity, activity.getString(R.string.connection_failed),
            Toast.LENGTH_LONG);
      }
    });
  }

  // TODO: Shift async tasks to WDM and handle cleanup from here itself
  public void destroyWifiDirectManager() {
    activity.cancelAsyncTasks();

    if (!activity.isFileSender) {
      disconnect();
    } else {
      closeChannel();
    }
  }

  public void disconnect() {
    manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

      @Override
      public void onFailure(int reasonCode) {
        Log.d(TAG, "Disconnect failed. Reason: " + reasonCode);
        closeChannel();
      }

      @Override
      public void onSuccess() {
        Log.d(TAG, "Disconnect successful");
        closeChannel();
      }
    });
  }

  public void closeChannel() {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      channel.close();
    }
  }


  public @NonNull String getErrorMessage(int reason) {
    switch (reason) {
      case WifiP2pManager.ERROR:
        return "Internal error";
      case WifiP2pManager.BUSY:
        return "Framework busy, unable to service request";
      case WifiP2pManager.P2P_UNSUPPORTED:
        return "P2P unsupported on this device";

      default:
        return ("Unknown error code - " + reason);
    }
  }

  public interface Callbacks {
    void updatePeerDevicesList(@NonNull WifiP2pDeviceList peers);

    void performHandshakeWithSelectedPeerDevice(@NonNull WifiP2pInfo groupInfo);
  }
}
