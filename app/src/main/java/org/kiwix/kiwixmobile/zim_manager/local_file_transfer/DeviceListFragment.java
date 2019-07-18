package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.ListFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.utils.AlertDialogShower;
import org.kiwix.kiwixmobile.utils.KiwixDialog;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.TO_BE_SENT;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.LocalFileTransferActivity.showToast;

/**
 * Part of the local file sharing module, this fragment is responsible for displaying details of
 * the user device and the list of available peer devices, and once the user taps on a particular
 * peer device item, connecting to that device and initiating file transfer.
 *
 * File transfer involves two phases:
 * 1) Handshake with the selected peer device, using {@link PeerGroupHandshakeAsyncTask}
 * 2) After handshake, starting the files transfer using {@link SenderDeviceAsyncTask} on the sender
 * device and {@link ReceiverDeviceAsyncTask} files receiving device
 *
 * The starting point for the module is {@link LocalFileTransferActivity}
 */
public class DeviceListFragment extends ListFragment {
    //implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {

  public static final String TAG = "DeviceListFragment";
  /*public static int PEER_HANDSHAKE_PORT = 8009;
  public static int FILE_TRANSFER_PORT = 8008;*/

  private SharedPreferenceUtil sharedPreferenceUtil;
  private AlertDialogShower alertDialogShower;

  private LocalFileTransferActivity localFileTransferActivity;
      // Parent activity, starting point of the module
  /*private TransferProgressFragment transferProgressFragment;*/
      // Sibling fragment, for displaying transfer progress

  /*// Views part of the DeviceListFragment
  @BindView(R.id.text_view_device_name) TextView deviceName;
  @BindView(R.id.progress_bar_searching_peers) ProgressBar searchingPeersProgressBar;
  @BindView(android.R.id.list) ListView listViewPeerDevices;
  @BindView(android.R.id.empty) TextView textViewPeerDevices;*/

  private Unbinder unbinder;

  private boolean fileSender = false; // Whether file sending device or not
  private ArrayList<Uri> fileUriList;
      // Uris of files to be shared, available only for the sender device
  /*private int totalFilesForTransfer = -1;
  private int filesSent = 0;          // Count of number of files transferred until now
  private ArrayList<FileItem> filesToSend = new ArrayList<>();

  private WifiP2pDevice userDevice;   // Represents the device on which the app is running
  private WifiP2pInfo groupInfo;
      // Corresponds to the WiFi P2P group formed between the two devices
  private List<WifiP2pDevice> peerDevices = new ArrayList<WifiP2pDevice>();

  private WifiP2pDevice selectedPeerDevice = null;
  private InetAddress selectedPeerDeviceInetAddress;

  private InetAddress fileReceiverDeviceAddress;  // IP address of the file receiving device
  private boolean fileTransferStarted = false;

  private PeerGroupHandshakeAsyncTask peerGroupHandshakeAsyncTask;
  private SenderDeviceAsyncTask senderDeviceAsyncTaskArray[];
  private ReceiverDeviceAsyncTask receiverDeviceAsyncTask;*/

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    localFileTransferActivity = (LocalFileTransferActivity) getActivity();

    // As DeviceListFragment extends ListFragment for the purpose of displaying list of peers
    /*this.setListAdapter(
        new WifiPeerListAdapter(localFileTransferActivity, R.layout.row_peer_device, peerDevices));*/

    /*if (localFileTransferActivity.isFileSender()) {
      fileSender = true;
      fileUriList = localFileTransferActivity.getFileUriArrayList();
      totalFilesForTransfer = fileUriList.size();

      for (int i = 0; i < fileUriList.size(); i++) {
        filesToSend.add(new FileItem(getFileName(fileUriList.get(i)), TO_BE_SENT));
      }

      displayTransferProgressFragment();
    }*/
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_device_list, null);
    unbinder = ButterKnife.bind(this, view);

    return view;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (unbinder != null) unbinder.unbind();
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    /* Connection can only be initiated by user of the sender device, & only when transfer has not been started *//*
    if (!isFileSender() || fileTransferStarted) {
      return;
    }

    selectedPeerDevice = (WifiP2pDevice) getListAdapter().getItem(position);
    alertDialogShower.show(new KiwixDialog.FileTransferConfirmation(selectedPeerDevice),
        new Function0<Unit>() {
          @Override public Unit invoke() {
            ((DeviceActionListener) localFileTransferActivity.wifiDirectManager).connect(selectedPeerDevice);
            showToast(localFileTransferActivity, R.string.performing_handshake, Toast.LENGTH_LONG);
            return Unit.INSTANCE;
          }
        });*/
  }

  /*private void displayTransferProgressFragment() {
    transferProgressFragment = TransferProgressFragment.newInstance(filesToSend);
    FragmentManager fragmentManager = localFileTransferActivity.getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.add(R.id.container_fragment_transfer_progress, transferProgressFragment)
        .commit();
  }*/

  /*public void updateUserDevice(WifiP2pDevice device) { // Update UI with user device's details
    this.userDevice = device;

    if (userDevice != null) {
      deviceName.setText(userDevice.deviceName);
      Log.d(TAG, getDeviceStatus(userDevice.status));
    }
  }*/

  /*public static String getDeviceStatus(int status) {

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
  }*/

  /* From WifiP2pManager.PeerListListener callback-interface *//*
  @Override
  public void onPeersAvailable(WifiP2pDeviceList peers) {
    searchingPeersProgressBar.setVisibility(View.GONE);
    listViewPeerDevices.setVisibility(View.VISIBLE);

    peerDevices.clear();
    peerDevices.addAll(peers.getDeviceList());
    ((WifiPeerListAdapter) getListAdapter()).notifyDataSetChanged();

    if (peerDevices.size() == 0) {
      Log.d(LocalFileTransferActivity.TAG, "No devices found");
    }
  }*/

  /*public void clearPeers() {
    peerDevices.clear();
    ((WifiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
  }

  void cancelAsyncTasks() {
    if (peerGroupHandshakeAsyncTask != null) {
      peerGroupHandshakeAsyncTask.cancel(true);
    }

    if (senderDeviceAsyncTaskArray != null) {
      for (SenderDeviceAsyncTask task : senderDeviceAsyncTaskArray) {
        task.cancel(true);
      }
    } else if (receiverDeviceAsyncTask != null) {
      receiverDeviceAsyncTask.cancel(true);
    }
  }

  public void onInitiateDiscovery() { // Setup UI for searching peers
    searchingPeersProgressBar.setVisibility(View.VISIBLE);
    listViewPeerDevices.setVisibility(View.INVISIBLE);
    textViewPeerDevices.setVisibility(View.INVISIBLE);
  }

  *//* From WifiP2pManager.ConnectionInfoListener callback-interface *//*
  @Override
  public void onConnectionInfoAvailable(WifiP2pInfo info) {
    *//* Devices have successfully connected, and 'info' holds information about the wifi p2p group formed *//*
    groupInfo = info;
    // Start handshake between the devices
    peerGroupHandshakeAsyncTask = new PeerGroupHandshakeAsyncTask(this, groupInfo);
    peerGroupHandshakeAsyncTask.execute();
  }

  public void setClientAddress(InetAddress clientAddress) {
    if (clientAddress == null) {
      // null is returned only in case of a failed handshake
      showToast(localFileTransferActivity, R.string.device_not_cooperating, Toast.LENGTH_LONG);
      localFileTransferActivity.wifiDirectManager.closeLocalFileTransferActivity();
      return;
    }

    // If control reaches here, means handshake was successful
    selectedPeerDeviceInetAddress = clientAddress;
    startFileTransfer();
  }

  private void startFileTransfer() {
    fileTransferStarted = true;

    if (groupInfo.groupFormed && !fileSender) {
      displayTransferProgressFragment();

      receiverDeviceAsyncTask = new ReceiverDeviceAsyncTask(this, transferProgressFragment);
      receiverDeviceAsyncTask.execute();
    } else if (groupInfo.groupFormed) {
      {
        Log.d(LocalFileTransferActivity.TAG, "Starting file transfer");

        fileReceiverDeviceAddress =
            (groupInfo.isGroupOwner) ? selectedPeerDeviceInetAddress : groupInfo.groupOwnerAddress;

        // Hack for allowing slower receiver devices to setup server before sender device requests to connect
        showToast(localFileTransferActivity, R.string.preparing_files, Toast.LENGTH_LONG);
        for (int i = 0; i < 20000000; i++) ;

        senderDeviceAsyncTaskArray = new SenderDeviceAsyncTask[totalFilesForTransfer];
        for (int i = 0; i < totalFilesForTransfer; i++) {
          senderDeviceAsyncTaskArray[i] =
              new SenderDeviceAsyncTask(this, transferProgressFragment, i);
          senderDeviceAsyncTaskArray[i].execute(fileUriList.get(i));
        }
      }
    }
  }

  *//* Helper methods *//*

  public WifiP2pDevice getUserDevice() {
    return userDevice;
  }

  public boolean isFileSender() {
    return fileSender;
  }

  public int getTotalFilesForTransfer() {
    return totalFilesForTransfer;
  }

  public void setTotalFilesForTransfer(int totalFilesForTransfer) {
    this.totalFilesForTransfer = totalFilesForTransfer;
  }

  public ArrayList<FileItem> getFileItems() {
    return filesToSend;
  }

  public void setFileItems(ArrayList<FileItem> fileItems) {
    this.filesToSend = fileItems;
  }

  public ArrayList<Uri> getFileUriList() {
    return fileUriList;
  }

  public void incrementTotalFilesSent() {
    this.filesSent++;
  }

  public boolean allFilesSent() {
    return (filesSent == totalFilesForTransfer);
  }

  public void performFieldInjection(SharedPreferenceUtil sharedPreferenceUtil,
      AlertDialogShower alertDialogShower) {
    this.sharedPreferenceUtil = sharedPreferenceUtil;
    this.alertDialogShower = alertDialogShower;
  }

  public String getZimStorageRootPath() {
    return (sharedPreferenceUtil.getPrefStorage() + "/Kiwix/");
  }

  public InetAddress getFileReceiverDeviceAddress() {
    return fileReceiverDeviceAddress;
  }

  *//*public static String getFileName(Uri fileUri) {
    String fileUriString = fileUri.toString();
    // Returns text after location of last slash in the file path
    return fileUriString.substring(fileUriString.lastIndexOf('/') + 1);
  }*//*

  public static void copyToOutputStream(InputStream inputStream, OutputStream outputStream)
      throws IOException {
    byte[] bufferForBytes = new byte[1024];
    int bytesRead;

    Log.d(TAG, "Copying to OutputStream...");
    while ((bytesRead = inputStream.read(bufferForBytes)) != -1) {
      outputStream.write(bufferForBytes, 0, bytesRead);
    }

    outputStream.close();
    inputStream.close();
    Log.d(LocalFileTransferActivity.TAG, "Both streams closed");
  }*/

  public interface DeviceActionListener {
    void connect(@NonNull WifiP2pDevice peerDevice);

    void closeLocalFileTransferActivity();
  }
}
