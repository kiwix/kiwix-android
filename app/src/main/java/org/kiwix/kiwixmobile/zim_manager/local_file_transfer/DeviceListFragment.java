package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.ListFragment;

import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.LocalFileTransferActivity.showToast;

/**
 * Part of the local file sharing module, this fragment is responsible for displaying details of
 * the user device and the list of available peer devices, and once the user taps on a particular
 * peer device item, connecting to that device and initiating file transfer.
 *
 * File transfer involves two phases:
 * 1) Handshake with the selected peer device, using {@link PeerGroupHandshakeAsyncTask}
 * 2) After handshake, starting the files transfer using {@link SenderDeviceAsyncTask} on the sender
 *    device and {@link ReceiverDeviceAsyncTask} files receiving device
 *
 * The starting point for the module is {@link LocalFileTransferActivity}
 * */
public class DeviceListFragment extends ListFragment implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {

  public static final String TAG = "DeviceListFragment";
  public static int PEER_HANDSHAKE_PORT = 8009;
  public static int FILE_TRANSFER_PORT = 8008;

  private SharedPreferenceUtil sharedPreferenceUtil;

  private LocalFileTransferActivity localFileTransferActivity;  // Parent activity, starting point of the module
  private TransferProgressFragment transferProgressFragment;    // Sibling fragment, for displaying transfer progress

  private View deviceListFragmentRootView = null; // Root view corresponding to the DeviceListFragment

  private boolean fileSender = false; // Whether file sending device or not
  private ArrayList<Uri> fileUriList; // Uris of files to be shared, available only for the sender device
  private int totalFilesForTransfer = -1;
  private int filesSent = 0;          // Count of number of files transferred until now
  private ArrayList<FileItem> filesToSend = new ArrayList<>();

  private WifiP2pDevice userDevice;   // Represents the device on which the app is running
  private WifiP2pInfo groupInfo;      // Corresponds to the WiFi P2P group formed between the two devices
  private List<WifiP2pDevice> peerDevices = new ArrayList<WifiP2pDevice>();

  private WifiP2pDevice selectedPeerDevice = null;
  private InetAddress selectedPeerDeviceInetAddress;

  private InetAddress fileReceiverDeviceAddress;  // IP address of the file receiving device
  private boolean fileTransferStarted = false;

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    localFileTransferActivity = (LocalFileTransferActivity) getActivity();

    // As DeviceListFragment extends ListFragment for the purpose of displaying list of peers
    this.setListAdapter(new WifiPeerListAdapter(localFileTransferActivity, R.layout.row_peer_device, peerDevices));

    if(localFileTransferActivity.isFileSender()) {
      fileSender = true;
      fileUriList = localFileTransferActivity.getFileUriArrayList();
      totalFilesForTransfer = fileUriList.size();

      for(int i = 0; i < fileUriList.size(); i++) {
        filesToSend.add(new FileItem(getFileName(fileUriList.get(i)), FileItem.TO_BE_SENT));
      }

      displayTransferProgressFragment();
    }

  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    deviceListFragmentRootView = inflater.inflate(R.layout.fragment_device_list, null);
    return deviceListFragmentRootView;
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    /* Connection can only be initiated by user of the sender device, & only when transfer has not been started */
    if(!isFileSender() || fileTransferStarted)
      return;

    selectedPeerDevice = (WifiP2pDevice) getListAdapter().getItem(position);
    new AlertDialog.Builder(localFileTransferActivity)
        .setMessage(getString(R.string.transfer_to) + " " + selectedPeerDevice.deviceName + "?")
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            ((DeviceActionListener) localFileTransferActivity).connect(selectedPeerDevice);
          }
        })
        .setNegativeButton(android.R.string.no, null)
        .show();
  }

  private void displayTransferProgressFragment() {
    transferProgressFragment = new TransferProgressFragment(filesToSend);
    FragmentManager fragmentManager = localFileTransferActivity.getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.add(R.id.container_fragment_transfer_progress, transferProgressFragment)
        .commit();
  }

  public void updateUserDevice(WifiP2pDevice device) { // Update UI with user device's details
    this.userDevice = device;

    if(userDevice != null) {
      TextView deviceName = deviceListFragmentRootView.findViewById(R.id.text_view_device_name);
      TextView deviceStatus = deviceListFragmentRootView.findViewById(R.id.text_view_device_status);

      if(deviceName != null)  deviceName.setText(userDevice.deviceName);
      if(deviceStatus != null)    deviceStatus.setText(getDeviceStatus(userDevice.status));
    }
  }

  public String getDeviceStatus(int status) {

    if(BuildConfig.DEBUG) Log.d(TAG, "Peer Status: " + status);
    switch (status) {
      case WifiP2pDevice.AVAILABLE : return getString(R.string.available);
      case WifiP2pDevice.INVITED   : return getString(R.string.invited);
      case WifiP2pDevice.CONNECTED : return getString(R.string.connected);
      case WifiP2pDevice.FAILED    : return getString(R.string.failed);
      case WifiP2pDevice.UNAVAILABLE:return getString(R.string.unavailable);

      default: return getString(R.string.unknown);
    }
  }

  /* From WifiP2pManager.PeerListListener callback-interface */
  @Override
  public void onPeersAvailable(WifiP2pDeviceList peers) {

    ProgressBar searchingPeersProgressBar = deviceListFragmentRootView.findViewById(R.id.progress_bar_searching_peers);
    searchingPeersProgressBar.setVisibility(View.GONE);
    FrameLayout frameLayoutPeerDevices = deviceListFragmentRootView.findViewById(R.id.frame_layout_peer_devices);
    frameLayoutPeerDevices.setVisibility(View.VISIBLE);

    peerDevices.clear();
    peerDevices.addAll(peers.getDeviceList());
    ((WifiPeerListAdapter) getListAdapter()).notifyDataSetChanged();

    if(peerDevices.size() == 0) {
      Log.d(LocalFileTransferActivity.TAG, "No devices found");
    }
  }

  public void clearPeers() {
    peerDevices.clear();
    ((WifiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
  }

  public void onInitiateDiscovery() { // Setup UI for searching peers
    ProgressBar searchingPeersProgressBar = deviceListFragmentRootView.findViewById(R.id.progress_bar_searching_peers);
    searchingPeersProgressBar.setVisibility(View.VISIBLE);
    FrameLayout frameLayoutPeerDevices = deviceListFragmentRootView.findViewById(R.id.frame_layout_peer_devices);
    frameLayoutPeerDevices.setVisibility(View.INVISIBLE);
  }

  /* From WifiP2pManager.ConnectionInfoListener callback-interface */
  @Override
  public void onConnectionInfoAvailable(WifiP2pInfo info) {
    /* Devices have successfully connected, and 'info' holds information about the wifi p2p group formed */
    groupInfo = info;
    // Start handshake between the devices
    new PeerGroupHandshakeAsyncTask(this, groupInfo).execute();
  }

  public void setClientAddress(InetAddress clientAddress) {
    if(clientAddress == null) {
      // null is returned only in case of a failed handshake
      showToast(localFileTransferActivity, R.string.device_not_cooperating, Toast.LENGTH_LONG);
      localFileTransferActivity.closeLocalFileTransferActivity();
      return;
    }

    // If control reaches here, means handshake was successful
    selectedPeerDeviceInetAddress = clientAddress;
    startFileTransfer();
  }

  private void startFileTransfer() {
    fileTransferStarted = true;

    if(groupInfo.groupFormed && !fileSender) {
      displayTransferProgressFragment();

      new ReceiverDeviceAsyncTask(this, transferProgressFragment).execute();
      showToast(localFileTransferActivity, R.string.preparing_to_receive, Toast.LENGTH_SHORT);

    } else if(groupInfo.groupFormed) {
      {
        Log.d(LocalFileTransferActivity.TAG, "Starting file transfer");

        showToast(localFileTransferActivity, R.string.starting_transfer, Toast.LENGTH_SHORT);

        if(groupInfo.isGroupOwner)  fileReceiverDeviceAddress = selectedPeerDeviceInetAddress;
        else                        fileReceiverDeviceAddress = groupInfo.groupOwnerAddress;

        //TODO: Fix this
        for(int i = 0; i < 20000000; i++);

        for(int i = 0; i < totalFilesForTransfer; i++) {
          new SenderDeviceAsyncTask(this, transferProgressFragment, i).execute(fileUriList.get(i));
        }
      }
    }
  }

  /* Helper methods */

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

  public void setSharedPreferenceUtil(SharedPreferenceUtil sharedPreferenceUtil) {
    this.sharedPreferenceUtil = sharedPreferenceUtil;
  }

  public String getZimStorageRootPath() {
    return (sharedPreferenceUtil.getPrefStorage() + "/Kiwix/");
  }

  public InetAddress getFileReceiverDeviceAddress() {
    return fileReceiverDeviceAddress;
  }


  public static String getFileName(Uri fileUri) {
    String fileUriString = fileUri.toString();
    // Returns text after location of last slash in the file path
    return fileUriString.substring(fileUriString.lastIndexOf('/')+1);
  }

  public static void copyToOutputStream(InputStream inputStream, OutputStream outputStream) throws IOException {
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


  private class WifiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

    private List<WifiP2pDevice> listItems;

    public WifiPeerListAdapter(@NonNull Context context, int resource, List<WifiP2pDevice> objects) {
      super(context, resource, objects);
      this.listItems = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

      View rowView = convertView;
      if(rowView == null) {
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        rowView = layoutInflater.inflate(R.layout.row_peer_device, parent, false);
      }

      WifiP2pDevice device = listItems.get(position);

      if(device != null) {
        TextView deviceName = rowView.findViewById(R.id.row_device_name);
        TextView deviceStatus = rowView.findViewById(R.id.row_device_status);

        if(deviceName != null)      deviceName.setText(device.deviceName);
        if(deviceStatus != null)    deviceStatus.setText(getDeviceStatus(device.status));
      }

      return rowView;
    }
  }

  public interface DeviceActionListener {
    void cancelSearch();

    void connect(@NonNull WifiP2pDevice peerDevice);

    void closeLocalFileTransferActivity();
  }
}
