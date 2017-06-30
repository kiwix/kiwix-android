package org.kiwix.kiwixmobile.utils;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.UUID;

public class StorageUtils {

  public static String getFileNameFromUrl(String url) {
    String filename = NetworkUtils.getFileNameFromUrl(url);
    filename = filename.replace(".meta4","");
    return filename;
  }
}
