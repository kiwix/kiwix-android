package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;

import java.util.ArrayList;

import javax.inject.Inject;

/**
 * Created by @Aditya-Sood as a part of GSoC 2019.
 *
 * This activity is the starting point for the module used for sharing zims between devices.
 *
 * The module is used for transferring ZIM files from one device to another, from within the
 * app. Two devices are connected to each other using WiFi Direct, followed by file transfer.
 *
 * The module uses this activity along with {@link DeviceListFragment} to manage connection
 * and file transfer between the devices.
 * */
public class LocalFileTransferActivity extends AppCompatActivity implements WifiP2pManager.ChannelListener, DeviceListFragment.DeviceActionListener {

  public static final String TAG = "LocalFileTransferActvty"; // Not a typo, 'Log' tags have a length upper limit of 25 characters
  public static final int REQUEST_ENABLE_WIFI_P2P = 1;
  public static final int REQUEST_ENABLE_LOCATION_SERVICES = 2;
  private static final int PERMISSION_REQUEST_CODE_COARSE_LOCATION = 1;
  private static final int PERMISSION_REQUEST_CODE_STORAGE_WRITE_ACCESS = 2;

  @Inject
  SharedPreferenceUtil sharedPreferenceUtil;

  private ArrayList<Uri> fileUriArrayList;  // For sender device, stores Uris of files to be transferred
  private Boolean fileSendingDevice = false;// Whether the device is the file sender or not


  /* Variables related to the WiFi P2P API */
  private boolean wifiP2pEnabled = false; // Whether WiFi has been enabled or not
  private boolean retryChannel = false;   // Whether channel has retried connecting previously

  private WifiP2pManager manager;         // Overall manager of Wifi p2p connections for the module
  private WifiP2pManager.Channel channel; // Connects the module to device's underlying Wifi p2p framework

  private final IntentFilter intentFilter = new IntentFilter(); // For specifying broadcasts (of the P2P API) that the module needs to respond to
  private BroadcastReceiver receiver = null; // For receiving the broadcasts given by above filter


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_local_file_transfer);
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Protect AsyncTask from orientation changes
    KiwixApplication.getApplicationComponent().inject(this);

    /*
    * Presence of file Uris decides whether the device with the activity open is a sender or receiver:
    * - On the sender device, this activity is started from the app chooser post selection
    * of files to share in the Library
    * - On the receiver device, the activity is started directly from within the 'Get Content'
    * activity, without any file Uris
    * */
    Intent filesIntent = getIntent();
    fileUriArrayList = filesIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
    if(fileUriArrayList != null && fileUriArrayList.size() > 0) {
      setDeviceAsFileSender();
    }

    Toolbar actionBar = findViewById(R.id.toolbar_local_file_transfer);
    setSupportActionBar(actionBar);
    actionBar.setNavigationIcon(R.drawable.ic_close_white_24dp);
    actionBar.setNavigationOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View v) {
        closeLocalFileTransferActivity();
      }
    });


    /* Initialisations for using the WiFi P2P API */

    // Intents that the broadcast receiver will be responding to
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
    channel = manager.initialize(this, getMainLooper(), null);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.wifi_file_share_items, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if(item.getItemId() == R.id.menu_item_search_devices) {

      /* Permissions essential for this module */
      if(!checkCoarseLocationAccessPermission()) return true;

      if(!checkExternalStorageWritePermission()) return true;

      // Initiate discovery
      if(!isWifiP2pEnabled()) {
        requestEnableWifiP2pServices();
        return true;
      }

      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isLocationServicesEnabled()) {
        requestEnableLocationServices();
        return true;
      }

      final DeviceListFragment deviceListFragment = (DeviceListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_device_list);
      deviceListFragment.onInitiateDiscovery();
      deviceListFragment.setSharedPreferenceUtil(sharedPreferenceUtil);
      manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
          Toast.makeText(LocalFileTransferActivity.this, "Discovery Initiated", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailure(int reason) {
          Toast.makeText(LocalFileTransferActivity.this, "Discovery Failed: " + getErrorMessage(reason), Toast.LENGTH_SHORT).show();
        }
      });
      return true;

    } else if(item.getItemId() == R.id.menu_item_cancel_search) {
      if(manager != null) {
        cancelSearch();
      }
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

  public void setWifiP2pEnabled(boolean wifiP2pEnabled) {
    this.wifiP2pEnabled = wifiP2pEnabled;
  }

  public boolean isWifiP2pEnabled() {
    return wifiP2pEnabled;
  }

  private String getErrorMessage(int reason) {
    String error = "";
    switch (reason) {
      case WifiP2pManager.ERROR:           error = "Internal error"; break;
      case WifiP2pManager.BUSY:            error = "Framework busy, unable to service request"; break;
      case WifiP2pManager.P2P_UNSUPPORTED: error = "P2P unsupported on this device"; break;

      default: error = "Unknown error code - "+reason; break;
    }

    return error;
  }

  public void resetPeers() {
    DeviceListFragment deviceListFragment = (DeviceListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_device_list);
    if(deviceListFragment != null) {
      deviceListFragment.clearPeers();
    }
  }

  public void resetData() {
    DeviceListFragment deviceListFragment = (DeviceListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_device_list);
    if(deviceListFragment != null) {
      deviceListFragment.clearPeers();
    }
  }


  /* From WifiP2pManager.ChannelListener interface */
  @Override
  public void onChannelDisconnected() {
    // Upon disconnection, retry one more time
    if(manager != null && !retryChannel) {
      Toast.makeText(this, "Channel lost, trying again", Toast.LENGTH_LONG).show();
      resetData();
      retryChannel = true;
      manager.initialize(this, getMainLooper(), this);

    } else {
      Toast.makeText(this, "Severe! Try Disable/Re-enable WiFi P2P", Toast.LENGTH_LONG).show();
    }
  }


  /* From DeviceListFragment.DeviceActionListener interface */
  @Override
  public void cancelSearch() {

    if (manager != null) {
      final DeviceListFragment fragment = (DeviceListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_device_list);

      if (fragment.getUserDevice() == null
          || fragment.getUserDevice().status == WifiP2pDevice.CONNECTED) {
        disconnect();

      } else if (fragment.getUserDevice().status == WifiP2pDevice.AVAILABLE
          || fragment.getUserDevice().status == WifiP2pDevice.INVITED) {

        manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {

          @Override
          public void onSuccess() {
            Toast.makeText(LocalFileTransferActivity.this, "Aborting connection",
                Toast.LENGTH_SHORT).show();
          }

          @Override
          public void onFailure(int reasonCode) {
            Toast.makeText(LocalFileTransferActivity.this,
                "Connect abort request failed. Reason : " + getErrorMessage(reasonCode),
                Toast.LENGTH_SHORT).show();
          }
        });
      }
    }
  }

  @Override
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
        Toast.makeText(LocalFileTransferActivity.this, "Connection failed: " + getErrorMessage(reason), Toast.LENGTH_LONG).show();
      }
    });
  }

  @Override
  public void closeLocalFileTransferActivity() {
    fileSendingDevice = false;
    disconnect();
    this.finish();
  }

  public void disconnect() {
    manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

      @Override
      public void onFailure(int reasonCode) {
        Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
      }

      @Override
      public void onSuccess() {
        Log.d(TAG, "Disconnect successful");
      }

    });
  }

  /* Helper methods used in the activity */
  private boolean checkCoarseLocationAccessPermission() { // Required by Android to detect wifi-p2p peers
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

      if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
          new AlertDialog.Builder(this)
              .setMessage("Location permission is required by Android to allow the app to detect peer devices")
              .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE_COARSE_LOCATION);
                }
              })
              .show();

        } else {
          requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE_COARSE_LOCATION);
        }

        return false;
      }
    }

    return true; // Control reaches here: Either permission granted at install time, or at the time of request
  }

  private boolean checkExternalStorageWritePermission() { // To access and store the zims
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

      if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

        if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
          new AlertDialog.Builder(this)
              .setMessage("Storage permissions required for accessing and storing ZIM files")
              .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE_STORAGE_WRITE_ACCESS);
                }
              })
              .show();

        } else {
          requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE_STORAGE_WRITE_ACCESS);
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

          Toast.makeText(this, "Cannot locate peer devices without location permissions", Toast.LENGTH_LONG).show();
          closeLocalFileTransferActivity();
          break;
        }
      }

      case PERMISSION_REQUEST_CODE_STORAGE_WRITE_ACCESS: {
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
          Log.e(TAG, "Storage write permission not granted");

          Toast.makeText(this, "Cannot access zim files without storage permission", Toast.LENGTH_LONG).show();
          closeLocalFileTransferActivity();
          break;
        }
      }
    }
  }

  private boolean isLocationServicesEnabled() {
    LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
    boolean gps_enabled = false;
    boolean network_enabled = false;

    try {
      gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    } catch(Exception ex) {ex.printStackTrace();}

    try {
      network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    } catch(Exception ex) {ex.printStackTrace();}

    return (gps_enabled || network_enabled);
  }

  private void requestEnableLocationServices() {
    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
    Fragment prev = getSupportFragmentManager().findFragmentByTag("LocationDialog");

    if(prev == null) {
      RequestEnableLocationServicesDialog dialogFragment = new RequestEnableLocationServicesDialog();
      dialogFragment.show(fragmentTransaction, "LocationDialog");
    }
  }

  public static class RequestEnableLocationServicesDialog extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage("Enable location to allow detection of peers")
          .setPositiveButton("Open Location Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
              paramDialogInterface.cancel();
              startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), LocalFileTransferActivity.REQUEST_ENABLE_LOCATION_SERVICES);
            }
          })
          .setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              Toast.makeText(getActivity(), "Cannot discover peers without location services", Toast.LENGTH_SHORT).show();
            }
          });

      return builder.create();
    }
  }

  private void requestEnableWifiP2pServices() {
    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
    Fragment prev = getSupportFragmentManager().findFragmentByTag("WifiP2pDialog");

    if(prev == null) {
      RequestEnableWifiP2pServicesDialog dialogFragment = new RequestEnableWifiP2pServicesDialog();
      dialogFragment.show(fragmentTransaction, "WifiP2pDialog");
    }
  }

  public static class RequestEnableWifiP2pServicesDialog extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage("Enable WiFi P2P from system settings")
          .setPositiveButton("Open WiFi Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
              paramDialogInterface.cancel();
              startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
          })
          .setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              Toast.makeText(getActivity(), "Cannot discover peers without WiFi ON", Toast.LENGTH_SHORT).show();
            }
          });

      return builder.create();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    switch (requestCode) {
      case REQUEST_ENABLE_LOCATION_SERVICES: {
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        if(!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
          // If neither provider is enabled
          Toast.makeText(this, "Cannot discover peers without location services", Toast.LENGTH_LONG).show();
        }
        break;
      }

      case REQUEST_ENABLE_WIFI_P2P: {
        if(!isWifiP2pEnabled()) {
          Toast.makeText(this, "Cannot discover peers without WiFi ON", Toast.LENGTH_LONG).show();
        }
        break;
      }
    }
  }

  private void showNeutralDialog(String dialogMessage) {
    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
    Fragment prev = getSupportFragmentManager().findFragmentByTag("NeutralDialog");
    if(prev != null) {
      fragmentTransaction.remove(prev); // To prevent multiple instances of the NeutralDialogs
    }
    fragmentTransaction.addToBackStack(null);

    NeutralDialog dialogFragment = new NeutralDialog(dialogMessage);
    // For DialogFragments, show() handles the fragment commit and display
    dialogFragment.show(fragmentTransaction, "NeutralDialog");
  }

  public static class NeutralDialog extends DialogFragment {

    private String dialogMessage = "";

    public NeutralDialog() {
      super();
    }

    public NeutralDialog(String message) {
      super();
      this.dialogMessage = message;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage(dialogMessage)
          .setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
          });

      return builder.create();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    receiver = new WifiDirectBroadcastReceiver(manager, channel, this);
    registerReceiver(receiver, intentFilter);
  }

  @Override
  public void onPause() {
    super.onPause();
    unregisterReceiver(receiver);
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    closeLocalFileTransferActivity();
  }
}
