package org.kiwix.kiwixmobile.wifi_hotspot;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import java.lang.reflect.Method;
import org.kiwix.kiwixmobile.R;

import static org.kiwix.kiwixmobile.utils.StyleUtils.dialogStyle;
import static org.kiwix.kiwixmobile.webserver.WebServerHelper.startServerDialog;

/**
 * WifiHotstopManager class makes use of the Android's WifiManager and WifiConfiguration class
 * to implement the wifi hotspot feature.
 * Created by Adeel Zafar on 28/5/2019.
 */

public class WifiHotspotManager {
  private WifiManager wifiManager;
  private Context context;
  private WifiManager.LocalOnlyHotspotReservation hotspotReservation;
  private boolean oreoenabled = false;
  private WifiConfiguration currentConfig;

  public WifiHotspotManager(Context context) {
    this.context = context;
    wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
  }

  // This method enables/disables the wifi access point
  // It is used for API<26
  public boolean setWifiEnabled(WifiConfiguration wifiConfig, boolean enabled) {
    try {
      if (enabled) { //disables wifi hotspot if it's already enabled
        wifiManager.setWifiEnabled(false);
      }

      Method method = wifiManager.getClass()
          .getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
      return (Boolean) method.invoke(wifiManager, wifiConfig, enabled);
    } catch (Exception e) {
      Log.e(this.getClass().toString(), "", e);
      return false;
    }
  }

  //Workaround to turn on hotspot for Oreo versions
  @RequiresApi(api = Build.VERSION_CODES.O)
  public void turnOnHotspot() {
    if (!oreoenabled) {
      wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

        @Override
        public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
          super.onStarted(reservation);
          hotspotReservation = reservation;
          currentConfig = hotspotReservation.getWifiConfiguration();

          Log.v("DANG", "THE PASSWORD IS: "
              + currentConfig.preSharedKey
              + " \n SSID is : "
              + currentConfig.SSID);

          hotspotDetailsDialog();

          oreoenabled = true;
        }

        @Override
        public void onStopped() {
          super.onStopped();
          Log.v("DANG", "Local Hotspot Stopped");
        }

        @Override
        public void onFailed(int reason) {
          super.onFailed(reason);
          Log.v("DANG", "Local Hotspot failed to start");
        }
      }, new Handler());
    }
  }

  //Workaround to turn off hotspot for Oreo versions
  @RequiresApi(api = Build.VERSION_CODES.O)
  public void turnOffHotspot() {
    if (hotspotReservation != null) {
      hotspotReservation.close();
      hotspotReservation = null;
      oreoenabled = false;
      Log.v("DANG", "Turned off hotspot");
    }
  }

  //This method checks the state of the hostpot for devices>=Oreo
  @RequiresApi(api = Build.VERSION_CODES.O)
  public boolean checkHotspotState() {
    return hotspotReservation != null;
  }

  // This method returns the current state of the Wifi access point
  private WIFI_AP_STATE_ENUMS getWifiApState() {
    try {
      Method method = wifiManager.getClass().getMethod("getWifiApState");

      int tmp = ((Integer) method.invoke(wifiManager));

      // Fix for Android 4
      if (tmp >= 10) {
        tmp = tmp - 10;
      }

      return WIFI_AP_STATE_ENUMS.class.getEnumConstants()[tmp];
    } catch (Exception e) {
      Log.e(this.getClass().toString(), "", e);
      return WIFI_AP_STATE_ENUMS.WIFI_AP_STATE_FAILED;
    }
  }

  //This method returns if wifi access point is enabled or not
  public boolean isWifiApEnabled() {
    return getWifiApState() == WIFI_AP_STATE_ENUMS.WIFI_AP_STATE_ENABLED;
  }

  //This method is to get the wifi ap configuration
  private WifiConfiguration getWifiApConfiguration() {
    try {
      Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
      return (WifiConfiguration) method.invoke(wifiManager);
    } catch (Exception e) {
      Log.e(this.getClass().toString(), "", e);
      return null;
    }
  }

  //This method is to set the wifi ap configuration
  public boolean setWifiApConfiguration(WifiConfiguration wifiConfig) {
    try {
      Method method =
          wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
      return (Boolean) method.invoke(wifiManager, wifiConfig);
    } catch (Exception e) {
      Log.e(this.getClass().toString(), "", e);
      return false;
    }
  }

  public void hotspotDetailsDialog() {

    AlertDialog.Builder builder = new AlertDialog.Builder(context, dialogStyle());

    builder.setPositiveButton(android.R.string.ok, (dialog, id) -> {
      startServerDialog(context);
    });
    builder.setTitle(context.getString(R.string.hotspot_turned_on));
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      builder.setMessage(
          context.getString(R.string.hotspot_details_message) + "\n" + context.getString(
              R.string.hotspot_ssid_label) + " " + currentConfig.SSID + "\n" + context.getString(
              R.string.hotspot_pass_label) + " " + currentConfig.preSharedKey);
    } else {
      currentConfig = getWifiApConfiguration();
      builder.setMessage(
          context.getString(R.string.hotspot_details_message) + "\n" + context.getString(
              R.string.hotspot_ssid_label) + " " + currentConfig.SSID + "\n" + context.getString(
              R.string.hotspot_pass_label) + " " + currentConfig.preSharedKey);
    }
    AlertDialog dialog = builder.create();
    dialog.show();
  }
}
