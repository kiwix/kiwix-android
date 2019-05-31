package org.kiwix.kiwixmobile.wifi_hotspot;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import java.lang.reflect.Method;

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

  //To get write permission settings, we use this method.
  public void showWritePermissionSettings(boolean force) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (force || !Settings.System.canWrite(this.context)) {
        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + this.context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.context.startActivity(intent);
      }
    }
  }

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
          //hotspotReservation = reservation;
          hotspotReservation = reservation;
          currentConfig = reservation.getWifiConfiguration();
          Toast.makeText(context, "THE PASSWORD IS: "
              + currentConfig.preSharedKey
              + " \n SSID is : "
              + currentConfig.SSID, Toast.LENGTH_LONG).show();
          Log.v("DANG", "THE PASSWORD IS: "
              + currentConfig.preSharedKey
              + " \n SSID is : "
              + currentConfig.SSID);
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
      oreoenabled = false;
    }
  }

  // This method returns the current state of the Wifi access point
  public WIFI_AP_STATE_ENUMS getWifiApState() {
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
  public WifiConfiguration getWifiApConfiguration() {
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
}
