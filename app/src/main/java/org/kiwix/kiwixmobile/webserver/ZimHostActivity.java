package org.kiwix.kiwixmobile.webserver;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import butterknife.BindView;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.Task;
import java.lang.reflect.Method;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseActivity;
import org.kiwix.kiwixmobile.wifi_hotspot.HotspotService;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.ZimFileSelectFragment;

import static org.kiwix.kiwixmobile.utils.StyleUtils.dialogStyle;
import static org.kiwix.kiwixmobile.webserver.WebServerHelper.getAddress;
import static org.kiwix.kiwixmobile.webserver.WebServerHelper.isServerStarted;

public class ZimHostActivity extends BaseActivity implements
    ServerStateListener {

  @BindView(R.id.startServerButton)
  Button startServerButton;
  @BindView(R.id.server_textView)
  TextView serverTextView;

  public static final String ACTION_TURN_ON_AFTER_O = "Turn_on_hotspot_after_oreo";
  public static final String ACTION_TURN_OFF_AFTER_O = "Turn_off_hotspot_after_oreo";
  public static final String ACTION_IS_HOTSPOT_ENABLED = "Is_hotspot_enabled";
  public static final String ACTION_START_SERVER = "start_server";
  public static final String ACTION_STOP_SERVER = "stop_server";
  private final String IP_STATE_KEY = "ip_state_key";
  private static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 102;
  private static final int LOCATION_SETTINGS_PERMISSION_RESULT = 101;
  private Intent serviceIntent;
  private Task<LocationSettingsResponse> task;
  HotspotService hotspotService;
  String ip;
  boolean bound;
  String TAG = ZimHostActivity.this.getClass().getSimpleName();
  ServiceConnection serviceConnection;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_zim_host);

    setUpToolbar();

    if (savedInstanceState != null) {
      serverTextView.setText(
          getString(R.string.server_started_message) + " " + savedInstanceState.getString(
              IP_STATE_KEY));
      startServerButton.setText(getString(R.string.stop_server_label));
      startServerButton.setBackgroundColor(getResources().getColor(R.color.stopServer));
    }

    serviceConnection = new ServiceConnection() {

      @Override
      public void onServiceConnected(ComponentName className, IBinder service) {
        HotspotService.HotspotBinder binder = (HotspotService.HotspotBinder) service;
        hotspotService = binder.getService();
        bound = true;
        hotspotService.registerCallBack(ZimHostActivity.this);
      }

      @Override
      public void onServiceDisconnected(ComponentName arg0) {
        bound = false;
      }
    };

    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    ZimFileSelectFragment fragment = new ZimFileSelectFragment();
    fragmentTransaction.add(R.id.frameLayoutServer, fragment);
    fragmentTransaction.commit();

    serviceIntent = new Intent(this, HotspotService.class);

    startServerButton.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          toggleHotspot();
        } else {
          //TO DO: show Dialog() + within that add check mobile Data check later.
          //if (isMobileDataEnabled(context)) {
          //  mobileDataDialog();
          //} else {
          if (isServerStarted) {
            startService(ACTION_STOP_SERVER);
          } else {
            startHotspotDialog();
          }
          //}
        }
      }
    });
  }

  @Override protected void onStart() {
    super.onStart();
    bindService();
  }

  @Override protected void onStop() {
    super.onStop();
    unbindService();
  }

  private void bindService() {

    bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
  }

  private void unbindService() {
    if (bound) {
      hotspotService.registerCallBack(null); // unregister
      unbindService(serviceConnection);
      bound = false;
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  void toggleHotspot() {
    //Check if location permissions are granted
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {

      startService(ACTION_IS_HOTSPOT_ENABLED); //If hotspot is already enabled, turn it off
    } else {
      //Ask location permission if not granted
      ActivityCompat.requestPermissions(this,
          new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
          MY_PERMISSIONS_ACCESS_FINE_LOCATION);
    }
  }

  @Override protected void onResume() {
    super.onResume();
    if (isServerStarted) {
      ip = getAddress();
      ip = ip.replaceAll("\n", "");
      serverTextView.setText(
          getString(R.string.server_started_message) + " " + ip);
      startServerButton.setText(getString(R.string.stop_server_label));
      startServerButton.setBackgroundColor(getResources().getColor(R.color.stopServer));
    }
  }

  // This method checks if mobile data is enabled in user's device.
  static boolean isMobileDataEnabled(Context context) {
    ConnectivityManager cm =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    try {
      Class cmClass = Class.forName(cm.getClass().getName());
      Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
      method.setAccessible(true);
      return (Boolean) method.invoke(cm);
    } catch (Exception e) {
      Log.e("ZimHostActivity", e.toString());
    }
    return false;
  }

  //This method sends the user to data usage summary settings activity
  private void openDataUsageActivity() {
    Intent intent = new Intent();
    intent.setComponent(new ComponentName("com.android.settings",
        "com.android.settings.Settings$DataUsageSummaryActivity"));
    startActivity(intent);
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    switch (requestCode) {
      case MY_PERMISSIONS_ACCESS_FINE_LOCATION: {
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            toggleHotspot();
          }
        }
        break;
      }
      default:
        break;
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    switch (requestCode) {
      //Checking the result code for LocationSettings resolution
      case LOCATION_SETTINGS_PERMISSION_RESULT:
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (resultCode) {
          case Activity.RESULT_OK:
            // All required changes were successfully made
            Log.v(TAG, states.isLocationPresent() + "");
            startService(ACTION_TURN_ON_AFTER_O);
            break;
          case Activity.RESULT_CANCELED:
            // The user was asked to change settings, but chose not to
            Log.v(TAG, "Canceled");
            break;
          default:
            break;
        }
        break;
      default:
        break;
    }
  }

  @Override protected void onDestroy() {
    super.onDestroy();
  }

  private void setUpToolbar() {
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setTitle(getString(R.string.menu_host_books));
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    toolbar.setNavigationOnClickListener(v -> onBackPressed());
  }

  void setupLocationServices() {
    LocationRequest mLocationRequest = new LocationRequest();
    mLocationRequest.setInterval(10);
    mLocationRequest.setSmallestDisplacement(10);
    mLocationRequest.setFastestInterval(10);
    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    LocationSettingsRequest.Builder builder = new
        LocationSettingsRequest.Builder();
    builder.addLocationRequest(mLocationRequest);

    task = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

    locationSettingsResponseBuilder();
  }

  private void locationSettingsResponseBuilder() {
    task.addOnCompleteListener(task -> {
      try {
        LocationSettingsResponse response = task.getResult(ApiException.class);
        // All location settings are satisfied. The client can initialize location
        // requests here.

        startService(ACTION_TURN_ON_AFTER_O);

        //}
      } catch (ApiException exception) {
        switch (exception.getStatusCode()) {
          case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
            // Location settings are not satisfied. But could be fixed by showing the
            // user a dialog.
            try {
              // Cast to a resolvable exception.
              ResolvableApiException resolvable = (ResolvableApiException) exception;
              // Show the dialog by calling startResolutionForResult(),
              // and check the result in onActivityResult().
              resolvable.startResolutionForResult(
                  ZimHostActivity.this,
                  LOCATION_SETTINGS_PERMISSION_RESULT);
            } catch (IntentSender.SendIntentException e) {
              // Ignore the error.
            } catch (ClassCastException e) {
              // Ignore, should be an impossible error.
            }
            break;
          case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
            // Location settings are not satisfied. However, we have no way to fix the
            // settings so we won't show the dialog.
            break;
          default:
            break;
        }
      }
    });
  }

  //Advice user to turn on hotspot manually for API<26
  void startHotspotDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this, dialogStyle());

    builder.setPositiveButton(getString(R.string.go_to_settings_label), (dialog, id) -> {
      setupWifiSettingsIntent();
    });

    builder.setNeutralButton(getString(R.string.hotspot_dialog_neutral_button), (dialog, id) -> {
      //TO DO: START SERVER WITHIN THE SERVICE.
      //Adding a handler because sometimes hotspot can take time to turn on.
      //TO DO: Add a progress dialog instead of handler
      ProgressDialog progressDialog =
          ProgressDialog.show(this, getString(R.string.progress_dialog_starting_server), "",
              true);
      progressDialog.show();
      final Handler handler = new Handler();
      handler.postDelayed(new Runnable() {
        @Override
        public void run() {
          progressDialog.dismiss();
          startService(ACTION_START_SERVER);
        }
      }, 7000);
    });

    builder.setTitle(getString(R.string.hotspot_dialog_title));
    builder.setMessage(
        getString(R.string.hotspot_dialog_message)
    );
    AlertDialog dialog = builder.create();
    dialog.show();
  }

  void startService(String ACTION) {
    serviceIntent.setAction(ACTION);
    this.startService(serviceIntent);
  }

  void mobileDataDialog() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this, dialogStyle());

      builder.setPositiveButton(this.getString(R.string.yes),
          (dialog, id) -> openDataUsageActivity());
      builder.setNegativeButton((android.R.string.no), (dialog, id) -> {
        startHotspotDialog();
      });
      builder.setTitle(this.getString(R.string.mobile_data_enabled));
      builder.setMessage(
          this.getString(R.string.mobile_data_message) + "\n" + this.getString(
              R.string.mobile_data_message_confirmation)
      );
      builder.setCancelable(false);
      AlertDialog dialog = builder.create();
      dialog.show();
    }
  }

  @Override public void serverStarted(String ip) {
    ip = ip.replaceAll("\n", "");
    this.ip = ip;
    serverTextView.setText(getString(R.string.server_started_message) + " " + ip);
    startServerButton.setText(getString(R.string.stop_server_label));
    startServerButton.setBackgroundColor(getResources().getColor(R.color.stopServer));
    isServerStarted = true;
  }

  @Override public void serverStopped() {
    serverTextView.setText(getString(R.string.server_textview_default_message));
    startServerButton.setText(getString(R.string.start_server_label));
    startServerButton.setBackgroundColor(getResources().getColor(R.color.greenTick));
    isServerStarted = false;
  }

  @Override public void hotspotTurnedOn(WifiConfiguration wifiConfiguration) {

    //Show an alert dialog for hotspot details
    AlertDialog.Builder builder = new AlertDialog.Builder(this, dialogStyle());

    ProgressDialog progressDialog =
        ProgressDialog.show(this, getString(R.string.progress_dialog_starting_server), "",
            true);
    progressDialog.show();

    builder.setPositiveButton(android.R.string.ok, (dialog, id) -> {
      final Handler handler = new Handler();
      handler.postDelayed(new Runnable() {
        @Override
        public void run() {
          progressDialog.dismiss();
          startService(ACTION_START_SERVER);
          //webServerHelper.startServerHelper();
        }
      }, 2000);
    });

    builder.setTitle(this.getString(R.string.hotspot_turned_on));
    builder.setMessage(
        this.getString(R.string.hotspot_details_message) + "\n" + this.getString(
            R.string.hotspot_ssid_label) + " " + wifiConfiguration.SSID + "\n" + this.getString(
            R.string.hotspot_pass_label) + " " + wifiConfiguration.preSharedKey);

    builder.setCancelable(false);
    AlertDialog dialog = builder.create();
    dialog.show();
  }

  private void setupWifiSettingsIntent() {
    final Intent intent = new Intent(Intent.ACTION_MAIN, null);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    final ComponentName cn =
        new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
    intent.setComponent(cn);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  @Override public void hotspotFailed() {
    //Show a dialog to turn off default hotspot

    AlertDialog.Builder builder = new AlertDialog.Builder(this, dialogStyle());

    builder.setPositiveButton(getString(R.string.go_to_wifi_settings_label), (dialog, id) -> {
      //Open wifi settings intent
      setupWifiSettingsIntent();
    });

    builder.setTitle(this.getString(R.string.hotspot_failed_title));
    builder.setMessage(
        this.getString(R.string.hotspot_failed_message));

    AlertDialog dialog = builder.create();
    dialog.show();
  }

  @Override public void hotspotState(Boolean state) {
    if (state) //if hotspot is already enabled, turn it off.
    {
      startService(ACTION_TURN_OFF_AFTER_O);
    } else //If hotspot is not already enabled, then turn it on.
    {
      setupLocationServices();
    }
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (isServerStarted) {
      outState.putString(IP_STATE_KEY, ip);
    }
  }
}
