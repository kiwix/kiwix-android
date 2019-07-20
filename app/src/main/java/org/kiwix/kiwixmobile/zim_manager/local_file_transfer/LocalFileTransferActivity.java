package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.List;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.utils.AlertDialogShower;
import org.kiwix.kiwixmobile.utils.KiwixDialog;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;

import java.util.ArrayList;

import javax.inject.Inject;

import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.TO_BE_SENT;

/**
 * Created by @Aditya-Sood as a part of GSoC 2019.
 *
 * This activity is the starting point for the module used for sharing zims between devices.
 *
 * The module is used for transferring ZIM files from one device to another, from within the
 * app. Two devices are connected to each other using WiFi Direct, followed by file transfer.
 *
 * File transfer involves two phases:
 * 1) Handshake with the selected peer device, using {@link PeerGroupHandshakeAsyncTask}
 * 2) After handshake, starting the files transfer using {@link SenderDeviceAsyncTask} on the sender
 * device and {@link ReceiverDeviceAsyncTask} files receiving device
 */
@SuppressLint("GoogleAppIndexingApiWarning")
public class LocalFileTransferActivity extends AppCompatActivity implements
    WifiDirectManager.Callbacks {

  // Not a typo, 'Log' tags have a length upper limit of 25 characters
  public static final String TAG = "LocalFileTransferActvty";
  public static final int REQUEST_ENABLE_WIFI_P2P = 1;
  public static final int REQUEST_ENABLE_LOCATION_SERVICES = 2;
  private static final int PERMISSION_REQUEST_CODE_COARSE_LOCATION = 1;
  private static final int PERMISSION_REQUEST_CODE_STORAGE_WRITE_ACCESS = 2;

  public static int PEER_HANDSHAKE_PORT = 8009;
  public static int FILE_TRANSFER_PORT = 8008;

  @Inject SharedPreferenceUtil sharedPreferenceUtil;
  @Inject AlertDialogShower alertDialogShower;

  @BindView(R.id.toolbar_local_file_transfer) Toolbar actionBar;
  @BindView(R.id.text_view_device_name) TextView deviceName;
  @BindView(R.id.progress_bar_searching_peers) ProgressBar searchingPeersProgressBar;
  @BindView(R.id.list_peer_devices) ListView listViewPeerDevices;
  @BindView(R.id.text_view_empty_peer_list) TextView textViewPeerDevices;
  @BindView(R.id.recycler_view_transfer_files) RecyclerView filesRecyclerView;

  private ArrayList<Uri> fileUriArrayList; // For sender device, stores uris of the files
  public @NonNull Boolean fileSendingDevice = false;// Whether the device is the file sender or not

  public @NonNull WifiDirectManager wifiDirectManager = new WifiDirectManager(this);

  private int totalFilesForTransfer = -1;
  private int filesSent = 0;          // Count of number of files transferred until now
  private ArrayList<FileItem> filesToSend = new ArrayList<>();
  private FileListAdapter fileListAdapter;

  private List<WifiP2pDevice> peerDevices = new ArrayList<WifiP2pDevice>();
  private InetAddress selectedPeerDeviceInetAddress;
  private InetAddress fileReceiverDeviceAddress;  // IP address of the file receiving device
  private boolean fileTransferStarted = false;

  private PeerGroupHandshakeAsyncTask peerGroupHandshakeAsyncTask;
  private SenderDeviceAsyncTask[] senderDeviceAsyncTaskArray;
  private ReceiverDeviceAsyncTask receiverDeviceAsyncTask;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    KiwixApplication.getApplicationComponent().activityComponent()
        .activity(this)
        .build()
        .inject(this);
    setTheme(sharedPreferenceUtil.nightMode() ? R.style.Theme_AppCompat_DayNight_NoActionBar : R.style.AppTheme);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_local_file_transfer);
    ButterKnife.bind(this);

    /*
     * Presence of file Uris decides whether the device with the activity open is a sender or receiver:
     * - On the sender device, this activity is started from the app chooser post selection
     * of files to share in the Library
     * - On the receiver device, the activity is started directly from within the 'Get Content'
     * activity, without any file Uris
     * */
    Intent filesIntent = getIntent();
    fileUriArrayList = filesIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
    if (fileUriArrayList != null && fileUriArrayList.size() > 0) {
      setDeviceAsFileSender();
    }

    setSupportActionBar(actionBar);
    actionBar.setNavigationIcon(R.drawable.ic_close_white_24dp);
    actionBar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        wifiDirectManager.closeLocalFileTransferActivity();
      }
    });

    wifiDirectManager.initialiseWifiDirectManager();

    listViewPeerDevices.setAdapter(
        new WifiPeerListAdapter(this, R.layout.row_peer_device, peerDevices));

    if (fileSendingDevice) {
      totalFilesForTransfer = fileUriArrayList.size();

      for (int i = 0; i < fileUriArrayList.size(); i++) {
        filesToSend.add(new FileItem(getFileName(fileUriArrayList.get(i)), TO_BE_SENT));
      }

      displayFileTransferProgress();
    }
  }

  @OnItemClick(R.id.list_peer_devices)
  void onItemClick(int position) {
    /* Connection can only be initiated by user of the sender device, & only when transfer has not been started */
    if (!fileSendingDevice || fileTransferStarted) {
      return;
    }

    WifiP2pDevice senderSelectedPeerDevice =
        (WifiP2pDevice) listViewPeerDevices.getAdapter().getItem(position);
    wifiDirectManager.setSenderSelectedPeerDevice(senderSelectedPeerDevice);
    alertDialogShower.show(
        new KiwixDialog.FileTransferConfirmation(senderSelectedPeerDevice.deviceName),
        new Function0<Unit>() {
          @Override public Unit invoke() {
            wifiDirectManager.connect();
            showToast(LocalFileTransferActivity.this, R.string.performing_handshake,
                Toast.LENGTH_LONG);
            return Unit.INSTANCE;
          }
        });
  }

  @Override
  public boolean onCreateOptionsMenu(@NonNull Menu menu) {
    getMenuInflater().inflate(R.menu.wifi_file_share_items, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    if (item.getItemId() == R.id.menu_item_search_devices) {

      /* Permissions essential for this module */
      if (!checkCoarseLocationAccessPermission()) return true;

      if (!checkExternalStorageWritePermission()) return true;

      /* Initiate discovery */
      if (!wifiDirectManager.isWifiP2pEnabled()) {
        requestEnableWifiP2pServices();
        return true;
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isLocationServicesEnabled()) {
        requestEnableLocationServices();
        return true;
      }

      onInitiateDiscovery();
      wifiDirectManager.discoverPeerDevices();

      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  /* Helper methods used in the activity */
  public void setDeviceAsFileSender() {
    fileSendingDevice = true;
  }

  public boolean isFileSender() {
    return fileSendingDevice;
  }

  public @NonNull ArrayList<Uri> getFileUriArrayList() {
    return fileUriArrayList;
  }

  public void updateUserDevice(@Nullable WifiP2pDevice userDevice) { // Update UI with user device's details
    wifiDirectManager.setUserDevice(userDevice);

    if (userDevice != null) {
      deviceName.setText(userDevice.deviceName);
      Log.d(TAG, getDeviceStatus(userDevice.status));
    }
  }

  public void clearPeers() {
    peerDevices.clear();
    ((WifiPeerListAdapter) listViewPeerDevices.getAdapter()).notifyDataSetChanged();
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

  private void displayFileTransferProgress() {
    fileListAdapter = new FileListAdapter(filesToSend);
    filesRecyclerView.setAdapter(fileListAdapter);
    filesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
  }

  public void onInitiateDiscovery() { // Setup UI for searching peers
    searchingPeersProgressBar.setVisibility(View.VISIBLE);
    listViewPeerDevices.setVisibility(View.INVISIBLE);
    textViewPeerDevices.setVisibility(View.INVISIBLE);
  }

  public void setClientAddress(@Nullable InetAddress clientAddress) {
    if (clientAddress == null) {
      // null is returned only in case of a failed handshake
      showToast(this, R.string.device_not_cooperating, Toast.LENGTH_LONG);
      wifiDirectManager.closeLocalFileTransferActivity();
      return;
    }

    // If control reaches here, means handshake was successful
    selectedPeerDeviceInetAddress = clientAddress;
    startFileTransfer();
  }

  private void startFileTransfer() {
    fileTransferStarted = true;

    if (wifiDirectManager.isGroupFormed() && !fileSendingDevice) {
      displayFileTransferProgress();

      receiverDeviceAsyncTask = new ReceiverDeviceAsyncTask(this);
      receiverDeviceAsyncTask.execute();
    } else if (wifiDirectManager.isGroupFormed()) { // && fileSendingDevice
      {
        Log.d(LocalFileTransferActivity.TAG, "Starting file transfer");

        fileReceiverDeviceAddress =
            (wifiDirectManager.isGroupOwner()) ? selectedPeerDeviceInetAddress
                : wifiDirectManager.getGroupOwnerAddress();

        // Hack for allowing slower receiver devices to setup server before sender device requests to connect
        showToast(this, R.string.preparing_files, Toast.LENGTH_LONG);
        for (int i = 0; i < 20000000; i++) ;

        senderDeviceAsyncTaskArray = new SenderDeviceAsyncTask[totalFilesForTransfer];
        for (int i = 0; i < totalFilesForTransfer; i++) {
          senderDeviceAsyncTaskArray[i] = new SenderDeviceAsyncTask(this, i);
          senderDeviceAsyncTaskArray[i].execute(fileUriArrayList.get(i));
        }
      }
    }
  }

  public int getTotalFilesForTransfer() {
    return totalFilesForTransfer;
  }

  public void setTotalFilesForTransfer(int totalFilesForTransfer) {
    this.totalFilesForTransfer = totalFilesForTransfer;
  }

  public @NonNull ArrayList<FileItem> getFileItems() {
    return filesToSend;
  }

  public void setFileItems(@NonNull ArrayList<FileItem> fileItems) {
    this.filesToSend = fileItems;
  }

  public void incrementTotalFilesSent() {
    this.filesSent++;
  }

  public boolean allFilesSent() {
    return (filesSent == totalFilesForTransfer);
  }

  public @NonNull String getZimStorageRootPath() {
    return (sharedPreferenceUtil.getPrefStorage() + "/Kiwix/");
  }

  public @NonNull InetAddress getFileReceiverDeviceAddress() {
    return fileReceiverDeviceAddress;
  }

  public static void copyToOutputStream(@NonNull InputStream inputStream, @NonNull OutputStream outputStream)
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
  }

  public void changeStatus(int itemIndex, @FileItem.FileStatus int status) {
    filesToSend.get(itemIndex).setFileStatus(status);
    fileListAdapter.notifyItemChanged(itemIndex);
  }

  /* From WifiDirectManager.Callbacks interface */
  @Override
  public void updatePeerDevicesList(@NonNull WifiP2pDeviceList peers) {
    searchingPeersProgressBar.setVisibility(View.GONE);
    listViewPeerDevices.setVisibility(View.VISIBLE);

    peerDevices.clear();
    peerDevices.addAll(peers.getDeviceList());
    ((WifiPeerListAdapter) listViewPeerDevices.getAdapter()).notifyDataSetChanged();

    if (peerDevices.size() == 0) {
      Log.d(LocalFileTransferActivity.TAG, "No devices found");
    }
  }

  @Override
  public void performHandshakeWithSelectedPeerDevice(@NonNull WifiP2pInfo groupInfo) {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "Starting handshake");
    }
    peerGroupHandshakeAsyncTask = new PeerGroupHandshakeAsyncTask(this, groupInfo);
    peerGroupHandshakeAsyncTask.execute();
  }

  /* Helper methods used for checking permissions and states of services */
  private boolean checkCoarseLocationAccessPermission() { // Required by Android to detect wifi-p2p peers
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

      if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
          != PackageManager.PERMISSION_GRANTED) {

        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
          alertDialogShower.show(KiwixDialog.LocationPermissionRationale.INSTANCE,
              new Function0<Unit>() {
                @Override public Unit invoke() {
                  requestPermissions(new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
                      PERMISSION_REQUEST_CODE_COARSE_LOCATION);
                  return Unit.INSTANCE;
                }
              });
        } else {
          requestPermissions(new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
              PERMISSION_REQUEST_CODE_COARSE_LOCATION);
        }

        return false;
      }
    }

    return true; // Control reaches here: Either permission granted at install time, or at the time of request
  }

  private boolean checkExternalStorageWritePermission() { // To access and store the zims
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

      if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
          != PackageManager.PERMISSION_GRANTED) {

        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
          alertDialogShower.show(KiwixDialog.StoragePermissionRationale.INSTANCE,
              new Function0<Unit>() {
                @Override public Unit invoke() {
                  requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                      PERMISSION_REQUEST_CODE_STORAGE_WRITE_ACCESS);
                  return Unit.INSTANCE;
                }
              });
        } else {
          requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
              PERMISSION_REQUEST_CODE_STORAGE_WRITE_ACCESS);
        }

        return false;
      }
    }

    return true; // Control reaches here: Either permission granted at install time, or at the time of request
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    switch (requestCode) {
      case PERMISSION_REQUEST_CODE_COARSE_LOCATION: {
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
          Log.e(TAG, "Location permission not granted");

          showToast(this, R.string.permission_refused_location, Toast.LENGTH_LONG);
          wifiDirectManager.closeLocalFileTransferActivity();
          break;
        }
      }

      case PERMISSION_REQUEST_CODE_STORAGE_WRITE_ACCESS: {
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
          Log.e(TAG, "Storage write permission not granted");

          showToast(this, R.string.permission_refused_storage, Toast.LENGTH_LONG);
          wifiDirectManager.closeLocalFileTransferActivity();
          break;
        }
      }
    }
  }

  private boolean isLocationServicesEnabled() {
    LocationManager locationManager =
        (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
    boolean gps_enabled = false;
    boolean network_enabled = false;

    try {
      gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    try {
      network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return (gps_enabled || network_enabled);
  }

  private void requestEnableLocationServices() {
    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
    Fragment prev =
        getSupportFragmentManager().findFragmentByTag(RequestEnableLocationServicesDialog.TAG);

    if (prev == null) {
      RequestEnableLocationServicesDialog dialogFragment =
          new RequestEnableLocationServicesDialog();
      dialogFragment.show(fragmentTransaction, RequestEnableLocationServicesDialog.TAG);
    }
  }

  private void requestEnableWifiP2pServices() {
    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
    Fragment prev =
        getSupportFragmentManager().findFragmentByTag(RequestEnableWifiP2pServicesDialog.TAG);

    if (prev == null) {
      RequestEnableWifiP2pServicesDialog dialogFragment = new RequestEnableWifiP2pServicesDialog();
      dialogFragment.show(fragmentTransaction, RequestEnableWifiP2pServicesDialog.TAG);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    switch (requestCode) {
      case REQUEST_ENABLE_LOCATION_SERVICES: {
        LocationManager locationManager =
            (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        if (!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
          // If neither provider is enabled
          showToast(this, R.string.permission_refused_location, Toast.LENGTH_LONG);
        }
        break;
      }

      case REQUEST_ENABLE_WIFI_P2P: {
        if (!wifiDirectManager.isWifiP2pEnabled()) {
          showToast(this, R.string.discovery_needs_wifi, Toast.LENGTH_LONG);
        }
        break;
      }
    }
  }

  /* Miscellaneous helper methods */
  static void showToast(Context context, int stringResource, int duration) {
    showToast(context, context.getString(stringResource), duration);
  }

  static void showToast(Context context, String text, int duration) {
    Toast.makeText(context, text, duration).show();
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

  @Override
  public void onResume() {
    super.onResume();
    wifiDirectManager.registerWifiDirectBroadcastRecevier();
  }

  @Override
  public void onPause() {
    super.onPause();
    wifiDirectManager.unregisterWifiDirectBroadcastRecevier();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    cancelAsyncTasks();
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    wifiDirectManager.closeLocalFileTransferActivity();
  }
}
