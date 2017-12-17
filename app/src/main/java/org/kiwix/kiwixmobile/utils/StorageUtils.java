package org.kiwix.kiwixmobile.utils;

public class StorageUtils {

  public static String getFileNameFromUrl(String url) {
    String filename = NetworkUtils.getFileNameFromUrl(url);
    filename = filename.replace(".meta4","");
    return filename;
  }
}
