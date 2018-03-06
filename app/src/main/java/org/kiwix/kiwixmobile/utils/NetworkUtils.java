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

import org.kiwix.kiwixmobile.R;

import java.util.UUID;

import static org.kiwix.kiwixmobile.utils.Constants.TAG_KIWIX;

public class NetworkUtils {

  public static boolean isNetworkAvailable(Context context) {
    ConnectivityManager connectivity = (ConnectivityManager) context
        .getSystemService(Context.CONNECTIVITY_SERVICE);
    if (connectivity == null) {
      return false;
    } else {
      NetworkInfo[] info = connectivity.getAllNetworkInfo();
      if (info != null) {
        for (NetworkInfo anInfo : info) {
          if (anInfo.getState() == NetworkInfo.State.CONNECTED
                  || anInfo.getState() == NetworkInfo.State.CONNECTING) {
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
