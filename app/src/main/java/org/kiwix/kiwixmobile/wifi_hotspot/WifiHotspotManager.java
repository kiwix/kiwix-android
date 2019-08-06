package org.kiwix.kiwixmobile.wifi_hotspot;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.webserver.ServerStateListener;
import org.kiwix.kiwixmobile.webserver.WebServerHelper;

import static org.kiwix.kiwixmobile.utils.StyleUtils.dialogStyle;

/**
 * WifiHotstopManager class makes use of the Android's WifiManager and WifiConfiguration class
 * to implement the wifi hotspot feature.
 * Created by Adeel Zafar on 28/5/2019.
 */

public class WifiHotspotManager {
  private WifiManager wifiManager;
  Context context;
  WifiManager.LocalOnlyHotspotReservation hotspotReservation;
  boolean oreoenabled = false;
  WifiConfiguration currentConfig;
  ServerStateListener serverStateListener;

  public WifiHotspotManager(Context context) {
    this.context = context;
    wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
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
          serverStateListener = (ServerStateListener) context;
          serverStateListener.serverStopped();
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

  public void hotspotDetailsDialog() {

    AlertDialog.Builder builder = new AlertDialog.Builder(context, dialogStyle());
    WebServerHelper webServerHelper = new WebServerHelper(context);
      builder.setPositiveButton(android.R.string.ok, (dialog, id) -> {
        webServerHelper.startServerDialog();
      });

    builder.setTitle(context.getString(R.string.hotspot_turned_on));
      builder.setMessage(
          context.getString(R.string.hotspot_details_message) + "\n" + context.getString(
              R.string.hotspot_ssid_label) + " " + currentConfig.SSID + "\n" + context.getString(
              R.string.hotspot_pass_label) + " " + currentConfig.preSharedKey);

    builder.setCancelable(false);
    AlertDialog dialog = builder.create();
    dialog.show();
  }
}
