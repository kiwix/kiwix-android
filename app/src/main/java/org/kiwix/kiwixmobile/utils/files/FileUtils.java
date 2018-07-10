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
package org.kiwix.kiwixmobile.utils.files;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;

import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.downloader.ChunkUtils;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.kiwix.kiwixmobile.downloader.ChunkUtils.deleteAllParts;
import static org.kiwix.kiwixmobile.utils.Constants.TAG_KIWIX;

public class FileUtils {

  public static File getFileCacheDir(Context context) {
    boolean external = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());

    if (external) {
      return context.getExternalCacheDir();
    } else {
      return context.getCacheDir();
    }
  }

  public static synchronized void deleteCachedFiles(Context context) {
    try {
      for (File file : getFileCacheDir(context).listFiles()) {
        file.delete();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static synchronized void deleteZimFile(File file) {
    deleteAllParts(file);
  }

  /**
   * Returns the file name (without full path) for an Expansion APK file from the given context.
   *
   * @param mainFile true for menu_main file, false for patch file
   * @return String the file name of the expansion file
   */
  public static String getExpansionAPKFileName(boolean mainFile) {
    return (mainFile ? "main." : "patch.") + BuildConfig.CONTENT_VERSION_CODE + "."
        + BuildConfig.APPLICATION_ID + ".obb";
  }

  /**
   * Returns the filename (where the file should be saved) from info about a download
   */
  static public String generateSaveFileName(String fileName) {
    return getSaveFilePath() + File.separator + fileName;
  }

  static public String getSaveFilePath() {
    String obbFolder = File.separator + "Android" + File.separator + "obb" + File.separator;
    File root = Environment.getExternalStorageDirectory();
    return root.toString() + obbFolder + BuildConfig.APPLICATION_ID;
  }

  /**
   * Helper function to ascertain the existence of a file and return true/false appropriately
   *
   * @param fileName             the name (sans path) of the file to query
   * @param fileSize             the size that the file must match
   * @param deleteFileOnMismatch if the file sizes do not match, delete the file
   * @return true if it does exist, false otherwise
   */
  static public boolean doesFileExist(String fileName, long fileSize,
                                      boolean deleteFileOnMismatch) {

    Log.d(TAG_KIWIX, "Looking for '" + fileName + "' with size=" + fileSize);

    // the file may have been delivered by Market --- let's make sure
    // it's the size we expect
    File fileForNewFile = new File(fileName);
    if (fileForNewFile.exists()) {
      if (fileForNewFile.length() == fileSize) {
        Log.d(TAG_KIWIX, "Correct file '" + fileName + "' found.");
        return true;
      } else {
        Log.d(TAG_KIWIX, "File '" + fileName + "' found but with wrong size=" + fileForNewFile.length());
      }

      if (deleteFileOnMismatch) {
        // delete the file --- we won't be able to resume
        // because we cannot confirm the integrity of the file
        fileForNewFile.delete();
      }
    } else {
      Log.d(TAG_KIWIX, "No file '" + fileName + "' found.");
    }
    return false;
  }

  static public String getLocalFilePathByUri(final Context ctx, final Uri uri) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(ctx, uri)) {
      if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
        String[] documentId = DocumentsContract.getDocumentId(uri).split(":");

        if (documentId[0].equals("primary"))
          return Environment.getExternalStorageDirectory() + "/" + documentId[1];

      } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
        String documentId = DocumentsContract.getDocumentId(uri);
        Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(documentId));

        return contentQuery(ctx, contentUri);
      }
    } else if ("content".equalsIgnoreCase(uri.getScheme())) {
      return contentQuery(ctx, uri);
    } else if ("file".equalsIgnoreCase(uri.getScheme())) {
      return uri.getPath();
    }

    return null;
  }

  static private String contentQuery(Context context, Uri uri) {
    Cursor cursor = null;

    try {
      cursor = context.getContentResolver().query(uri, new String[]{"_data"}, null, null, null);

      if (cursor != null && cursor.moveToFirst())
        return cursor.getString(cursor.getColumnIndexOrThrow("_data"));

    } finally {
      if (cursor != null)
        cursor.close();
    }

    return null;
  }

  public static ArrayList<String> readLocalesFromAssets(Context context) {

    String content = "";

    try {
      InputStream stream = context.getAssets().open("locales.txt");

      int size = stream.available();
      byte[] buffer = new byte[size];
      stream.read(buffer);
      stream.close();
      content = new String(buffer);
    } catch (IOException ignored) { }

    return readCsv(content);
  }



  private static ArrayList<String> readCsv(String csv) {

    String[] csvArray = csv.split(",");
    return new ArrayList<>(Arrays.asList(csvArray));
  }
}
