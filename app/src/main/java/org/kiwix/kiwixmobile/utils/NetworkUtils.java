package org.kiwix.kiwixmobile.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;
import java.util.UUID;
import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;

public class NetworkUtils {

  public static boolean isNetworkAvailable(Context context) {
    ConnectivityManager connectivity = (ConnectivityManager) context
        .getSystemService(Context.CONNECTIVITY_SERVICE);
    if (connectivity == null) {
      return false;
    } else {
      NetworkInfo[] info = connectivity.getAllNetworkInfo();
      if (info != null) {
        for (int i = 0; i < info.length; i++) {
          if (info[i].getState() == NetworkInfo.State.CONNECTED
              || info[i].getState() == NetworkInfo.State.CONNECTING) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public static boolean isWiFi(Context context) {
    ConnectivityManager connectivity = (ConnectivityManager) context
            .getSystemService(Context.CONNECTIVITY_SERVICE);
    if (connectivity == null)
      return false;

    if (Build.VERSION.SDK_INT >= 23) {
      NetworkInfo network = connectivity.getActiveNetworkInfo();
      return network.getType() == ConnectivityManager.TYPE_WIFI;
    } else {
      NetworkInfo wifi = connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
      return wifi.isConnected();
    }
  }

  public static String getFileNameFromUrl(String url) {

    int index = url.lastIndexOf('?');
    String filename;
    if (index > 1) {
      filename = url.substring(url.lastIndexOf('/') + 1, index);
    } else {
      filename = url.substring(url.lastIndexOf('/') + 1);
    }

    if ("".equals(filename.trim())) {
      filename = String.valueOf(UUID.randomUUID());
    }

    return filename;
  }

  public static String parseURL(Context context, String url) {
    String details;
    try {
      details = url.substring(url.lastIndexOf("/") + 1, url.length() - 10);
      details = details.substring(details.indexOf("_", details.indexOf("_") + 1) + 1, details.lastIndexOf("_"));
      details = details.replaceAll("_", " ");
      details = details.replaceAll("all", "");
      details = details.replaceAll("nopic", context.getString(R.string.zim_nopic));
      details = details.replaceAll("simple", context.getString(R.string.zim_simple));
      details = details.trim().replaceAll(" +", " ");
      return details;
    } catch (Exception e) {
      Log.d(KiwixMobileActivity.TAG_KIWIX, "Context invalid url");
      return "";
    }
  }
}
