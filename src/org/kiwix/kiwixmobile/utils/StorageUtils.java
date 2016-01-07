package org.kiwix.kiwixmobile.utils;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

public class StorageUtils {

  private static final String SDCARD_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath();

  public static final String FILE_ROOT = SDCARD_ROOT + "/kiwix/";

  private static final long LOW_STORAGE_THRESHOLD = 1024 * 1024 * 10;

  public static boolean isSdCardWritable() {
    return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
  }

  public static long getAvailableStorage() {

    String storageDirectory;
    storageDirectory = Environment.getExternalStorageDirectory().toString();

    try {
      StatFs stat = new StatFs(storageDirectory);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        return (stat.getAvailableBlocksLong() * stat.getBlockSizeLong());
      }
      return ((long) stat.getAvailableBlocks() * (long) stat.getBlockSize());
    } catch (RuntimeException ex) {
      return 0;
    }
  }

  public static boolean checkAvailableStorage() {
    return getAvailableStorage() >= LOW_STORAGE_THRESHOLD;
  }

  public static boolean isSDCardAvailable() {
    return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
  }

  public static boolean createKiwixRootDir() throws IOException {
    File file = new File(FILE_ROOT);
    return (!file.exists() || !file.isDirectory()) && file.mkdir();
  }

  public static String size(long size) {
    if (size / (1024 * 1024) > 0) {
      float tmpSize = (float) (size) / (float) (1024 * 1024);
      DecimalFormat df = new DecimalFormat("#.##");
      return "" + df.format(tmpSize) + "MB";
    } else if (size / 1024 > 0) {
      return "" + (size / (1024)) + "KB";
    } else {
      return "" + size + "B";
    }
  }

  public static boolean delete(File path) {
    boolean result = true;
    if (path.exists()) {
      if (path.isDirectory()) {
        for (File child : path.listFiles()) {
          result &= delete(child);
        }
        result &= path.delete(); // Delete empty directory.
      }
      if (path.isFile()) {
        result &= path.delete();
      }
      if (!result) {
        Log.e(null, "Delete failed;");
      }
      return result;
    } else {
      Log.e(null, "File does not exist.");
      return false;
    }
  }
}
