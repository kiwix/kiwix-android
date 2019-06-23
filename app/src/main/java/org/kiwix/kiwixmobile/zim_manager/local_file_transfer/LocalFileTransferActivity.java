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
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.inject.Inject;

public class LocalFileTransferActivity extends AppCompatActivity implements WifiP2pManager.ChannelListener, DeviceListFragment.DeviceActionListener {

  /*TODO
  *   - Fix activity closure upon file transfer (successful or otherwise)
  *   - Handle multiple selected files
  *   */

  public static final String TAG = "LocalFileTransferActvty"; // Not a typo, Tags have a length upper limit of 25 characters
  public static Uri filePath = null; // = Environment.getExternalStorageDirectory() + "/Kiwix/temp.txt";///psiram_en_all_2018-09.zim";//Notes/Granblue Fantasy Wiki/Main Page.txt";
  private final int PERMISSION_REQUEST_CODE_COARSE_LOCATION = 1;

  @Inject
  SharedPreferenceUtil sharedPreferenceUtil;

  private boolean isWifiP2pEnabled = false;
  private boolean retryChannel = false;

  private ArrayList<Uri> fileURIArrayList;

  private WifiP2pManager manager;
  private final IntentFilter intentFilter = new IntentFilter();
  private WifiP2pManager.Channel channel;
  private BroadcastReceiver receiver = null;
  private Boolean fileSendingDevice = false; // True if intent has file uri
  // TODO: Set to true if activity opening intent has the file URI

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_local_file_transfer);
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Protect AsyncTask from orientation changes
    KiwixApplication.getApplicationComponent().inject(this);

        /*setContentView(R.layout.activity_local_file_transfer);

    TextView fileUriListView = findViewById(R.id.text_view_file_uris);

    Intent filesIntent = getIntent();
    ArrayList<Uri> fileURIArrayList = filesIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

    String uriList = "Selected File URIs:\n\n";
    if(fileURIArrayList != null && fileURIArrayList.size() > 0) {
      for(int i = 0; i < fileURIArrayList.size(); i++) {
        uriList += fileURIArrayList.get(i) + "\n\n";
      }
    }

    fileUriListView.setText(uriList);*/

    Intent filesIntent = getIntent();
    fileURIArrayList = filesIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

    if(fileURIArrayList != null && fileURIArrayList.size() > 0) {
      //filePath = fileURIArrayList.get(0);
      setFileSender();
    }

    Toolbar actionBar = findViewById(R.id.toolbar_local_file_transfer);
    setSupportActionBar(actionBar);

    // Intents that the broadcast receiver will be responding to
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    //TODO: Start WiFi
    manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
    channel = manager.initialize(this, getMainLooper(), null);

    // TODO: Add manager.removeGroup(channel, null); to remove previous groups

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

      if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
        showNeutralDialog("Location permission is required to locate peer devices\n\nUser location is not being tracked by the app");
        //TODO: Close activity
      }

      requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE_COARSE_LOCATION);
    }

    requestExternalStorageWritePermission();
  }

  private boolean requestExternalStorageWritePermission() {
    if(Build.VERSION.SDK_INT >= 23) { // For Marshmallow & higher API levels

      if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
        return true;

      } else {
        if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
          /* shouldShowRequestPermissionRationale() returns false when:
           *  1) User has previously checked on "Don't ask me again", and/or
           *  2) Permission has been disabled on device
           */
          Toast.makeText(getApplicationContext(), "Required for file access", Toast.LENGTH_LONG).show();
        }

        requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
      }

    } else { // For Android versions below Marshmallow 6.0 (API 23)
      return true; // As already requested at install time
    }

    return false;
  }

  public ArrayList<Uri> getFileURIArrayList() {
    return fileURIArrayList;
  }

  public boolean isWifiP2pEnabled() {
    return isWifiP2pEnabled;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.wifi_file_share_items, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if(item.getItemId() == R.id.menu_item_search_devices) {
      // Initiate discovery
      //TODO
      if(!isWifiP2pEnabled) {
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
    } else if(item.getItemId() == R.id.menu_item_disconnect) {
      if(manager != null) {
        // TODO: 'cancelDisconnect', for removing the indefinite progress bar
        //removeGroupDetails();
        disconnect();
      }


      return true;
    }
    else {
      return super.onOptionsItemSelected(item);
    }
  }

  private void removeGroupDetails() {
    try {
      Method deletePersistentGroup = manager.getClass().getMethod("deletePersistentGroup", WifiP2pManager.Channel.class, int.class, WifiP2pManager.ActionListener.class);
      for(int netId = 0; netId < 32; netId++) {
        deletePersistentGroup.invoke(manager, channel, netId, new WifiP2pManager.ActionListener() {
          @Override
          public void onSuccess() {
            Log.d(TAG, "WiFi Direct Group successfully deleted");
          }

          @Override
          public void onFailure(int reason) {
            Log.d(TAG, "Group deletion failed: "+getErrorMessage(reason));
          }
        });
      }

    } catch (Exception e) {
      e.printStackTrace();
      Log.d(TAG, "Error removing group details: " + e.getMessage());
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

        /*if(!gps_enabled && !network_enabled) {
            // notify user

        }*/

    return (gps_enabled || network_enabled);
  }

  private void requestEnableLocationServices() {

    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
    Fragment prev = getSupportFragmentManager().findFragmentByTag("LocationDialog");
    if(prev != null) {
      fragmentTransaction.remove(prev); // To prevent multiple instances of the DialogFragment
    }
    fragmentTransaction.addToBackStack(null);

    RequestEnableLocationServicesDialog dialogFragment = new RequestEnableLocationServicesDialog();
    // For DialogFragments, show() handles the fragment commit and display
    dialogFragment.show(fragmentTransaction, "LocationDialog");

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
              startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 1);
            }
          })
          .setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              //TODO: Close activity

            }
          });

      return builder.create();
    }
  }

  private void requestEnableWifiP2pServices() {

        /*new AlertDialog.Builder(this)
                .setMessage("Enable WiFi P2P from system settings")
                .setPositiveButton("Open WiFi Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        paramDialogInterface.cancel();
                        startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();*/

    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
    Fragment prev = getSupportFragmentManager().findFragmentByTag("WifiP2pDialog");
    if(prev != null) {
      fragmentTransaction.remove(prev); // To prevent multiple instances of the DialogFragment
    }
    fragmentTransaction.addToBackStack(null);

    RequestEnableWifiP2pServicesDialog dialogFragment = new RequestEnableWifiP2pServicesDialog();
    // For DialogFragments, show() handles the fragment commit and display
    dialogFragment.show(fragmentTransaction, "WifiP2pDialog");
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
              startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            }
          })
          .setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              //TODO: Close activity
            }
          });

      return builder.create();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    switch (requestCode) {
      case 1: {
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
          return;
        } else {
                    /*new AlertDialog.Builder(this)
                            .setMessage("Cannot discover peers without location services")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //((MainActivity) getApplicationContext()).finish();
                                }
                            })
                            .show();*/
          showNeutralDialog("Cannot discover peers without location services");
          //TODO: Close activity
        }

      }
    }
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

  @Override
  public void onResume() {
    super.onResume();

        /*if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isLocationServicesEnabled()) {
            requestEnableLocationServices();
        }*/
    //TODO
    receiver = new WifiDirectBroadcastReceiver(manager, channel, this);
    registerReceiver(receiver, intentFilter);
  }

  @Override
  public void onPause() {
    super.onPause();

    //TODO
    unregisterReceiver(receiver);
  }

  public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
    this.isWifiP2pEnabled = isWifiP2pEnabled;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    switch (requestCode) {
      case PERMISSION_REQUEST_CODE_COARSE_LOCATION : {
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
          Log.e(TAG, "Location permission not granted");

          showNeutralDialog("Cannot locate peer devices without location permissions");
          //TODO: Close activity

          break;
        }
      }

      case 0: break;
    }
  }

  public void resetPeers() {
    DeviceListFragment deviceListFragment = (DeviceListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_device_list);
    if(deviceListFragment != null) {
      deviceListFragment.clearPeers();
    }
  }

  @Override
  public void onChannelDisconnected() {
    //TODO
    if(manager != null && !retryChannel) {
      Toast.makeText(this, "Channel lost, trying again", Toast.LENGTH_LONG).show();
      resetData();
      retryChannel = true;
      manager.initialize(this, getMainLooper(), this);

    } else {
      Toast.makeText(this, "Severe! Try Disable/Re-enable WiFi P2P", Toast.LENGTH_LONG).show();
    }
  }

  public void resetData() {
    DeviceListFragment deviceListFragment = (DeviceListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_device_list);
    if(deviceListFragment != null) {
      deviceListFragment.clearPeers();
    }
  }

  private void showNeutralDialog(String dialogMessage) {
    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
    Fragment prev = getSupportFragmentManager().findFragmentByTag("NeutralDialog");
    if(prev != null) {
      fragmentTransaction.remove(prev); // To prevent multiple instances of the DialogFragment
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
  public void changeDeviceName(String deviceNewName) {
    try {
      Method method = manager.getClass().getMethod("setDeviceName", WifiP2pManager.Channel.class, String.class, WifiP2pManager.ActionListener.class);
      method.invoke(manager, channel, deviceNewName, new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
          Toast.makeText(LocalFileTransferActivity.this, "Name successfully changed", Toast.LENGTH_LONG).show();
          resetPeers();
        }

        @Override
        public void onFailure(int reason) {
          Toast.makeText(LocalFileTransferActivity.this, "Request failed: " + reason, Toast.LENGTH_SHORT).show();
          Log.d(TAG, "Name change failed: " + getErrorMessage(reason));
        }
      });

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public boolean isFileSender() {
    return fileSendingDevice;
  }

  public void setFileSender() {
    fileSendingDevice = true;
  }

    /*@Override
    public void showDetails(WifiP2pDevice device) {

    }*/

  @Override
  public void cancelDisconnect() {
    //TODO
  }

  @Override
  public void connect(final WifiP2pDevice peerDevice) {
    WifiP2pConfig config = new WifiP2pConfig();
    config.deviceAddress = peerDevice.deviceAddress;
    config.wps.setup = WpsInfo.PBC;

    // If self sender, then receiver will be group owner
    if(isFileSender())
      config.groupOwnerIntent = 0; // Sets inclination for own device. This way other device has got to be the owner.
    // Maybe reset the previous wifi direct group data, which is causing a fixed group owner

        /*else
            config.groupOwnerIntent = 15;*/

    //TODO: Show a progress bar between starting & completion of connection

    manager.connect(channel, config, new WifiP2pManager.ActionListener() {
      @Override
      public void onSuccess() {
        //Toast.makeText(MainActivity.this, "Connected to " + peerDevice.deviceName, Toast.LENGTH_SHORT).show();
        // UI updated from broadcast receiver
      }

      @Override
      public void onFailure(int reason) {
        Toast.makeText(LocalFileTransferActivity.this, "Connection failed: " + getErrorMessage(reason), Toast.LENGTH_LONG).show();
      }
    });
  }

  @Override
  public void disconnect() {
    fileSendingDevice = false;

    //TODO
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

    this.finish();
  }
}
