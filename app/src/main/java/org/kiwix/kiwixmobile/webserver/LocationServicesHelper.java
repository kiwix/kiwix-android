package org.kiwix.kiwixmobile.webserver;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.util.Log;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.Task;
import javax.inject.Inject;

public class LocationServicesHelper {
  private Activity activity;
  private static final String TAG = "LocationServicesHelper";
  private LocationCallbacks locationCallbacks;
  public static final int LOCATION_SETTINGS_PERMISSION_RESULT = 101;

  @Inject
  public LocationServicesHelper(Activity activity, LocationCallbacks locationCallbacks) {
    this.activity = activity;
    this.locationCallbacks = locationCallbacks;
  }

  private Task<LocationSettingsResponse> task;

  public void setupLocationServices() {
    LocationRequest locationRequest = new LocationRequest();
    locationRequest.setInterval(10);
    locationRequest.setSmallestDisplacement(10);
    locationRequest.setFastestInterval(10);
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    LocationSettingsRequest.Builder builder = new
        LocationSettingsRequest.Builder();
    builder.addLocationRequest(locationRequest);

    task = com.google.android.gms.location.LocationServices.getSettingsClient(activity)
        .checkLocationSettings(builder.build());

    locationSettingsResponseBuilder();
  }

  public void locationSettingsResponseBuilder() {
    task.addOnCompleteListener(task -> {
      try {
        LocationSettingsResponse response = task.getResult(ApiException.class);
        // All location settings are satisfied. The client can initialize location
        // requests here.

        locationCallbacks.onLocationSet();
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
                  activity,
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

  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    //Checking the result code for LocationSettings resolution
    if (requestCode == LOCATION_SETTINGS_PERMISSION_RESULT) {
      final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
      switch (resultCode) {
        case Activity.RESULT_OK:
          // All required changes were successfully made
          Log.v(TAG, states.isLocationPresent() + "");
          locationCallbacks.onLocationSet();
          break;
        case Activity.RESULT_CANCELED:
          // The user was asked to change settings, but chose not to
          Log.v(TAG, "Canceled");
          break;
        default:
          break;
      }
    }
  }
}