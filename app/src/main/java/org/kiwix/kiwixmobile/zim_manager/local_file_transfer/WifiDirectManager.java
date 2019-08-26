package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import javax.inject.Inject;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.utils.AlertDialogShower;
import org.kiwix.kiwixmobile.utils.KiwixDialog;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;

import static android.os.Looper.getMainLooper;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.ERROR;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.LocalFileTransferActivity.showToast;

/**
 * Manager for the Wifi-P2p API, used in the local file transfer module
 */
@SuppressWarnings("MissingPermission")
public class WifiDirectManager
  implements WifiP2pManager.ChannelListener, WifiP2pManager.PeerListListener,
  WifiP2pManager.ConnectionInfoListener,
  KiwixWifiP2pBroadcastReceiver.P2pEventListener {

  private static final String TAG = "WifiDirectManager";
  public static int FILE_TRANSFER_PORT = 8008;

  private @NonNull Activity activity;
  private @NonNull Callbacks callbacks;

  private SharedPreferenceUtil sharedPreferenceUtil;
  private AlertDialogShower alertDialogShower;

  /* Variables related to the WiFi P2P API */
  private boolean isWifiP2pEnabled = false; // Whether WiFi has been enabled or not
  private boolean shouldRetry = true;       // Whether channel has retried connecting previously

  private WifiP2pManager manager;         // Overall manager of Wifi p2p connections for the module
  private WifiP2pManager.Channel channel; // Interface to the device's underlying wifi-p2p framework

  private BroadcastReceiver receiver = null; // For receiving the broadcasts given by above filter

  private WifiP2pInfo groupInfo; // Corresponds to P2P group formed between the two devices

  private WifiP2pDevice senderSelectedPeerDevice = null;

  private PeerGroupHandshakeAsyncTask peerGroupHandshakeAsyncTask;
  private SenderDeviceAsyncTask senderDeviceAsyncTask;
  private ReceiverDeviceAsyncTask receiverDeviceAsyncTask;

  private InetAddress selectedPeerDeviceInetAddress;
  private InetAddress fileReceiverDeviceAddress;  // IP address of the file receiving device

  private ArrayList<FileItem> filesForTransfer;

  private boolean isFileSender = false;    // Whether the device is the file sender or not
  private boolean hasSenderStartedConnection = false;

  @Inject
  public WifiDirectManager(@NonNull Activity activity,
    @NonNull SharedPreferenceUtil sharedPreferenceUtil,
    @NonNull AlertDialogShower alertDialogShower) {
    this.activity = activity;
    this.callbacks = (Callbacks) activity;
    this.sharedPreferenceUtil = sharedPreferenceUtil;
    this.alertDialogShower = alertDialogShower;
  }

  /* Initialisations for using the WiFi P2P API */
  public void startWifiDirectManager(@Nullable ArrayList<FileItem> filesForTransfer) {
    this.filesForTransfer = filesForTransfer;
    this.isFileSender = (filesForTransfer != null && filesForTransfer.size() > 0);

    manager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
    channel = manager.initialize(activity, getMainLooper(), null);
    registerWifiDirectBroadcastReceiver();
  }

  public void registerWifiDirectBroadcastReceiver() {
    receiver = new KiwixWifiP2pBroadcastReceiver(this);

    // For specifying broadcasts (of the P2P API) that the module needs to respond to
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    activity.registerReceiver(receiver, intentFilter);
  }

  public void unregisterWifiDirectBroadcastReceiver() {
    activity.unregisterReceiver(receiver);
  }

  public void discoverPeerDevices() {
    manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
      @Override
      public void onSuccess() {
        displayToast(R.string.discovery_initiated, Toast.LENGTH_SHORT);
      }

      @Override
      public void onFailure(int reason) {
        String errorMessage = getErrorMessage(reason);
        Log.d(TAG, activity.getString(R.string.discovery_failed) + ": " + errorMessage);
        displayToast(R.string.discovery_failed, Toast.LENGTH_SHORT);
      }
    });
  }

  /* From KiwixWifiP2pBroadcastReceiver.P2pEventListener callback-interface*/
  @Override
  public void onWifiP2pStateChanged(boolean isEnabled) {
    this.isWifiP2pEnabled = isEnabled;

    if (!isWifiP2pEnabled) {
      displayToast(R.string.discovery_needs_wifi, Toast.LENGTH_SHORT);
      callbacks.onConnectionToPeersLost();
    }

    Log.d(TAG, "WiFi P2P state changed - " + isWifiP2pEnabled);
  }

  @Override
  public void onPeersChanged() {
    /* List of available peers has changed, so request & use the new list through
     * PeerListListener.requestPeers() callback */
    manager.requestPeers(channel, this);
    Log.d(TAG, "P2P peers changed");
  }

  @Override
  public void onConnectionChanged(boolean isConnected) {
    if (isConnected) {
      // Request connection info about the wifi p2p group formed upon connection
      manager.requestConnectionInfo(channel, this);
    } else {
      // Not connected after connection change -> Disconnected
      callbacks.onConnectionToPeersLost();
    }
  }

  @Override
  public void onDeviceChanged(@Nullable WifiP2pDevice userDevice) {
    // Update UI with wifi-direct details about the user device
    callbacks.onUserDeviceDetailsAvailable(userDevice);
  }

  /* From WifiP2pManager.ChannelListener interface */
  @Override
  public void onChannelDisconnected() {
    // Upon disconnection, retry one more time
    if (shouldRetry) {
      Log.d(TAG, "Channel lost, trying again");
      callbacks.onConnectionToPeersLost();
      shouldRetry = false;
      manager.initialize(activity, getMainLooper(), this);
    } else {
      displayToast(R.string.severe_loss_error, Toast.LENGTH_LONG);
    }
  }

  /* From WifiP2pManager.PeerListListener callback-interface */
  @Override
  public void onPeersAvailable(@NonNull WifiP2pDeviceList peers) {
    callbacks.updateListOfAvailablePeers(peers);
  }

  /* From WifiP2pManager.ConnectionInfoListener callback-interface */
  @Override
  public void onConnectionInfoAvailable(@NonNull WifiP2pInfo groupInfo) {
    /* Devices have successfully connected, and 'info' holds information about the wifi p2p group formed */
    this.groupInfo = groupInfo;
    performHandshakeWithSelectedPeerDevice();
  }

  /* Helper methods */
  public boolean isWifiP2pEnabled() {
    return isWifiP2pEnabled;
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
    /* Connection can only be initiated by user of the sender device, & only when transfer has not been started */
    if (!isFileSender || hasSenderStartedConnection) {
      return;
    }

    this.senderSelectedPeerDevice = senderSelectedPeerDevice;

    alertDialogShower.show(
      new KiwixDialog.FileTransferConfirmation(senderSelectedPeerDevice.deviceName),
      new Function0<Unit>() {
        @Override public Unit invoke() {
          hasSenderStartedConnection = true;
          connect();
          displayToast(R.string.performing_handshake, Toast.LENGTH_LONG);
          return Unit.INSTANCE;
        }
      });
  }

  public void connect() {
    if (senderSelectedPeerDevice == null) {
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
        displayToast(R.string.connection_failed, Toast.LENGTH_LONG);
      }
    });
  }

  public void performHandshakeWithSelectedPeerDevice() {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "Starting handshake");
    }
    peerGroupHandshakeAsyncTask = new PeerGroupHandshakeAsyncTask(this);
    peerGroupHandshakeAsyncTask.execute();
  }

  public boolean isFileSender() {
    return isFileSender;
  }

  public int getTotalFilesForTransfer() {
    return filesForTransfer.size();
  }

  public @NonNull ArrayList<FileItem> getFilesForTransfer() {
    return filesForTransfer;
  }

  public void setFilesForTransfer(@NonNull ArrayList<FileItem> fileItems) {
    this.filesForTransfer = fileItems;
  }

  public @NonNull String getZimStorageRootPath() {
    return (sharedPreferenceUtil.getPrefStorage() + "/Kiwix/");
  }

  public @NonNull InetAddress getFileReceiverDeviceAddress() {
    return fileReceiverDeviceAddress;
  }

  public static void copyToOutputStream(@NonNull InputStream inputStream,
    @NonNull OutputStream outputStream) throws IOException {
    byte[] bufferForBytes = new byte[1024];
    int bytesRead;

    Log.d(TAG, "Copying to OutputStream...");
    while ((bytesRead = inputStream.read(bufferForBytes)) != -1) {
      outputStream.write(bufferForBytes, 0, bytesRead);
    }

    outputStream.close();
    inputStream.close();
    Log.d(LocalFileTransferActivity.TAG, "Both streams closed");
  }

  public void setClientAddress(@Nullable InetAddress clientAddress) {
    if (clientAddress == null) {
      // null is returned only in case of a failed handshake
      displayToast(R.string.device_not_cooperating, Toast.LENGTH_LONG);
      callbacks.onFileTransferComplete();
      return;
    }

    // If control reaches here, means handshake was successful
    selectedPeerDeviceInetAddress = clientAddress;
    startFileTransfer();
  }

  private void startFileTransfer() {
    if (isGroupFormed()) {
      if (isFileSender) {
        Log.d(LocalFileTransferActivity.TAG, "Starting file transfer");

        fileReceiverDeviceAddress =
          (isGroupOwner()) ? selectedPeerDeviceInetAddress : getGroupOwnerAddress();

        displayToast(R.string.preparing_files, Toast.LENGTH_LONG);
        senderDeviceAsyncTask = new SenderDeviceAsyncTask(this, activity);
        senderDeviceAsyncTask.execute(filesForTransfer.toArray(new FileItem[0]));
      } else {
        callbacks.onFilesForTransferAvailable(filesForTransfer);

        receiverDeviceAsyncTask = new ReceiverDeviceAsyncTask(this);
        receiverDeviceAsyncTask.execute();
      }
    }
  }

  public void changeStatus(int itemIndex, @FileItem.FileStatus int status) {
    filesForTransfer.get(itemIndex).setFileStatus(status);
    callbacks.onFileStatusChanged(itemIndex);

    if (status == ERROR) {
      displayToast(R.string.error_transferring, filesForTransfer.get(itemIndex).getFileName(),
        Toast.LENGTH_SHORT);
    }
  }

  private void cancelAsyncTasks(AsyncTask... tasks) {
    for (AsyncTask asyncTask : tasks) {
      if (asyncTask != null) {
        asyncTask.cancel(true);
      }
    }
  }

  public void stopWifiDirectManager() {
    cancelAsyncTasks(peerGroupHandshakeAsyncTask, senderDeviceAsyncTask, receiverDeviceAsyncTask);

    if (isFileSender) {
      closeChannel();
    } else {
      disconnect();
    }

    unregisterWifiDirectBroadcastReceiver();
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

  private void closeChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
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

  public static @NonNull String getDeviceStatus(int status) {

    if (BuildConfig.DEBUG) Log.d(TAG, "Peer Status: " + status);
    switch (status) {
      case WifiP2pDevice.AVAILABLE:
        return "Available";
      case WifiP2pDevice.INVITED:
        return "Invited";
      case WifiP2pDevice.CONNECTED:
        return "Connected";
      case WifiP2pDevice.FAILED:
        return "Failed";
      case WifiP2pDevice.UNAVAILABLE:
        return "Unavailable";

      default:
        return "Unknown";
    }
  }

  public static @NonNull String getFileName(@NonNull Uri fileUri) {
    String fileUriString = fileUri.toString();
    // Returns text after location of last slash in the file path
    return fileUriString.substring(fileUriString.lastIndexOf('/') + 1);
  }

  public void displayToast(int stringResourceId, @NonNull String templateValue, int duration) {
    showToast(activity, activity.getString(stringResourceId, templateValue), duration);
  }

  public void displayToast(int stringResourceId, int duration) {
    showToast(activity, stringResourceId, duration);
  }

  public void onFileTransferAsyncTaskComplete(boolean wereAllFilesTransferred) {
    if (wereAllFilesTransferred) {
      displayToast(R.string.file_transfer_complete, Toast.LENGTH_LONG);
    } else {
      displayToast(R.string.error_during_transfer, Toast.LENGTH_LONG);
    }
    callbacks.onFileTransferComplete();
  }

  public interface Callbacks {
    void onUserDeviceDetailsAvailable(@Nullable WifiP2pDevice userDevice);

    void onConnectionToPeersLost();

    void updateListOfAvailablePeers(@NonNull WifiP2pDeviceList peers);

    void onFilesForTransferAvailable(@NonNull ArrayList<FileItem> filesForTransfer);

    void onFileStatusChanged(int itemIndex);

    void onFileTransferComplete();
  }
}
