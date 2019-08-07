package org.kiwix.kiwixmobile.webserver;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
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
import org.kiwix.kiwixmobile.wifi_hotspot.HotspotService;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.ZimFileSelectFragment;

import static org.kiwix.kiwixmobile.utils.StyleUtils.dialogStyle;
import static org.kiwix.kiwixmobile.webserver.WebServerHelper.isStarted;
import static org.kiwix.kiwixmobile.webserver.WebServerHelper.stopAndroidWebServer;
import static org.kiwix.kiwixmobile.wifi_hotspot.HotspotService.checkHotspotState;

public class ZimHostActivity extends AppCompatActivity implements
    ServerStateListener {

  Button startServerButton;
  TextView serverTextView;

  public static final String ACTION_TURN_ON_AFTER_O = "Turn_on_hotspot_after_oreo";
  public static final String ACTION_TURN_OFF_AFTER_O = "Turn_off_hotspot_after_oreo";
  private final String IP_STATE_KEY = "ip_state_key";
  private static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 102;
  private Intent serviceIntent;
  private Task<LocationSettingsResponse> task;
  boolean flag = false;
  String ip;
  String TAG = ZimHostActivity.this.getClass().getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_zim_host);

    setUpToolbar();

    startServerButton = (Button) findViewById(R.id.startServerButton);
    serverTextView = (TextView) findViewById(R.id.server_textView);

    if (savedInstanceState != null) {
      serverTextView.setText(
          getString(R.string.server_started_message) + " " + savedInstanceState.getString(
              IP_STATE_KEY));
      startServerButton.setText(getString(R.string.stop_server_label));
    }

    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    ZimFileSelectFragment fragment = new ZimFileSelectFragment();
    fragmentTransaction.add(R.id.frameLayoutServer, fragment);
    fragmentTransaction.commit();

    serviceIntent = new Intent(this, HotspotService.class);
    Context context = this;

    startServerButton.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          toggleHotspot();
        } else {
          //TO DO: show Dialog() + within that add check mobile Data check later.
          //if (isMobileDataEnabled(context)) {
          //  mobileDataDialog();
          //} else {
          if (flag) {
            serverStopped();
            isStarted = false;
          } else {
            startHotspotDialog();
          }
          //}
        }
      }
    });
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  void toggleHotspot() {
    //Check if location permissions are granted
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
      if (checkHotspotState(this)) //If hotspot is already enabled, turn it off
      {
        startService(ACTION_TURN_OFF_AFTER_O);
      } else //If hotspot is not already enabled, then turn it on.
      {
        setupLocationServices();
      }
    } else {
      //Ask location permission if not granted
      ActivityCompat.requestPermissions(this,
          new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
          MY_PERMISSIONS_ACCESS_FINE_LOCATION);
    }
  }

  // This method checks if mobile data is enabled in user's device.
  static boolean isMobileDataEnabled(Context context) {
    boolean enabled = false;
    ConnectivityManager cm =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    try {
      Class cmClass = Class.forName(cm.getClass().getName());
      Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
      method.setAccessible(true);
      enabled = (Boolean) method.invoke(cm);
    } catch (Exception e) {
      Log.e("ZimHostActivity", e.toString());
    }
    return enabled;
  }

  //This method sends the user to data usage summary settings activity
  private void disableMobileData() {
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

  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
      switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
          //Code for webserver
          if (isStarted) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("WARNING")
                .setMessage("You've already a server running")
                .setPositiveButton(getResources().getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int id) {
                        finish();
                      }
                    })
                .setNegativeButton(getResources().getString(android.R.string.cancel), null)
                .show();
          } else {
            finish();
          }
          return true;
      }
    }
    return false;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    switch (requestCode) {
      //Checking the result code for LocationSettings resolution
      case 101:
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (resultCode) {
          case Activity.RESULT_OK:
            // All required changes were successfully made
            Log.v("case 101", states.isLocationPresent() + "");
            startService(ACTION_TURN_ON_AFTER_O);
            break;
          case Activity.RESULT_CANCELED:
            // The user was asked to change settings, but chose not to
            Log.v("case 101", "Canceled");
            break;
          default:
            break;
        }
        break;
    }
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    stopAndroidWebServer();
    isStarted = false;
  }

  private void setUpToolbar() {
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setTitle(getString(R.string.menu_host_books));
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    toolbar.setNavigationOnClickListener(v -> onBackPressed());
  }

  private void setupLocationServices() {
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
                  101);
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
        }
      }
    });
  }

  //Advice user to turn on hotspot manually for API<26
  void startHotspotDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this, dialogStyle());

    builder.setPositiveButton(getString(R.string.hotspot_dialog_positive_button), (dialog, id) -> {
    });

    builder.setNeutralButton(getString(R.string.hotspot_dialog_neutral_button), (dialog, id) -> {
      //TO DO: START SERVER WITHIN THE SERVICE.
      WebServerHelper webServerHelper = new WebServerHelper(this);
      //Adding a handler because sometimes hotspot can take time to turn on.
      //TO DO: Add a progress dialog instead of handler
      final Handler handler = new Handler();
      handler.postDelayed(new Runnable() {
        @Override
        public void run() {
          webServerHelper.startServerHelper();
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

  private void startService(String ACTION) {
    serviceIntent.setAction(ACTION);
    this.startService(serviceIntent);
  }

  void mobileDataDialog() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this, dialogStyle());

      builder.setPositiveButton(this.getString(R.string.yes), (dialog, id) -> disableMobileData());
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
    flag = true;
  }

  @Override public void serverStopped() {
    serverTextView.setText(getString(R.string.server_textview_default_message));
    startServerButton.setText(getString(R.string.start_server_label));
    flag = false;
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (flag) {
      outState.putString(IP_STATE_KEY, ip);
    }
  }
}
