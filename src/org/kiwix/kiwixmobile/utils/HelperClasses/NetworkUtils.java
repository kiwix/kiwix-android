package org.kiwix.kiwixmobile.utils.HelperClasses;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.util.UUID;

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
}
