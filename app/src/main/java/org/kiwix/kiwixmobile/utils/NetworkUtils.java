/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kiwix.kiwixmobile.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;
import java.util.UUID;
import org.kiwix.kiwixmobile.R;

import static org.kiwix.kiwixmobile.utils.Constants.TAG_KIWIX;

public class NetworkUtils {
  /**
   * check availability of any network
   * @return true if a network is ready to be used
   */
  public static boolean isNetworkAvailable(Context context) {
    ConnectivityManager connectivity = (ConnectivityManager) context
      .getSystemService(Context.CONNECTIVITY_SERVICE);
    if (connectivity == null) {
      return false;
    } else {
      NetworkInfo[] networkInfos = connectivity.getAllNetworkInfo();
      if (networkInfos != null) {
        for (NetworkInfo networkInfo : networkInfos) {
          if (isNetworkConnectionOK(networkInfo)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  static boolean isNetworkConnectionOK(NetworkInfo networkInfo) {
    return networkInfo.getState() == NetworkInfo.State.CONNECTED;
  }


  /**
   * check if network of type WIFI is connected
   * @param context
   * @return true if WIFI is connected
   */
  //TODO method isWiFi should be renamed to isWifiConnected to express the state which is checked (postponed to refactoring deprecated android.net.* usage)
  public static boolean isWiFi(Context context) {
    ConnectivityManager connectivity = (ConnectivityManager) context
      .getSystemService(Context.CONNECTIVITY_SERVICE);
    if (connectivity == null) {
      return false;
    }

    if (Build.VERSION.SDK_INT >= 23) {
      NetworkInfo networkInfo = connectivity.getActiveNetworkInfo();
      if (networkInfo == null) {
        return false;
      }
      return networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected();
    } else {
      NetworkInfo wifi = connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
      return wifi != null && wifi.isConnected();
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
      details = url.substring(url.lastIndexOf("/") + 1);
      int beginIndex = details.indexOf("_", details.indexOf("_") + 1) + 1;
      int endIndex = details.lastIndexOf("_");
      if (beginIndex < 0 || endIndex > details.length() || beginIndex > endIndex) {
        return "";
      }
      details = details.substring(beginIndex, endIndex);
      details = details.replaceAll("_", " ");
      details = details.replaceAll("all", "");
      details = details.replaceAll("nopic", context.getString(R.string.zim_nopic));
      details = details.replaceAll("novid", context.getString(R.string.zim_novid));
      details = details.replaceAll("simple", context.getString(R.string.zim_simple));
      details = details.trim().replaceAll(" +", " ");
      return details;
    } catch (Exception e) {
      Log.d(TAG_KIWIX, "Context invalid url: " + url, e);
      return "";
    }
  }
}
