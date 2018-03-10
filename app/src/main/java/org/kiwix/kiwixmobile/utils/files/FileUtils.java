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

import android.app.Application;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.util.Log;

import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import static org.kiwix.kiwixmobile.utils.Constants.TAG_KIWIX;

public class FileUtils {

  // the name of the file to store the bundle in.
  private static final String BUNDLE_STORAGE = "SAVED_WEB_VIEW.parcel";

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

  public static synchronized void deleteZimFile(String path) {
    if (path.substring(path.length() - 5).equals(".part")) {
      path = path.substring(0, path.length() - 5);
    }
    Log.i("kiwix", "Deleting file: " + path);
    File file = new File(path);
    if (!file.getPath().substring(file.getPath().length() - 3).equals("zim")) {
      fileloop:
      for (char alphabetFirst = 'a'; alphabetFirst <= 'z'; alphabetFirst++) {
        for (char alphabetSecond = 'a'; alphabetSecond <= 'z'; alphabetSecond++) {
          String chunkPath = path.substring(0, path.length() - 2) + alphabetFirst + alphabetSecond;
          File fileChunk = new File(chunkPath);
          if (fileChunk.exists()) {
            fileChunk.delete();
          } else if (!deleteZimFileParts(chunkPath)) {
            break fileloop;
          }
        }
      }
    } else {
      file.delete();
      deleteZimFileParts(path);
    }
  }

  private static synchronized boolean deleteZimFileParts(String path) {
    File file = new File(path + ".part");
    if (file.exists()) {
      file.delete();
      return true;
    }
    return false;
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

  private static List<File> getAllZimParts(Book book) {
    List<File> files = new ArrayList<>();
    if(book.file.getPath().endsWith(".zim") || book.file.getPath().endsWith(".zim.part")) {
      if(book.file.exists()) {
        files.add(book.file);
      } else {
        files.add(new File(book.file + ".part"));
      }
      return files;
    }
    String path = book.file.getPath();
    for(char alphabetFirst = 'a'; alphabetFirst <= 'z'; alphabetFirst++) {
      for(char alphabetSecond = 'a'; alphabetSecond <= 'z'; alphabetSecond++) {
        path = path.substring(0, path.length() - 2) + alphabetFirst + alphabetSecond;
        if(new File(path).exists()) {
          files.add(new File(path));
        } else if(new File(path + ".part").exists()) {
          files.add(new File(path + ".part"));
        } else {
          return files;
        }
      }
    }
    return files;
  }

  private static ArrayList<String> readCsv(String csv) {

    String[] csvArray = csv.split(",");
    return new ArrayList<>(Arrays.asList(csvArray));
  }

  public static boolean hasPart(File file) {
    file = new File(getFileName(file.getPath()));
    if (file.getPath().endsWith(".zim")) {
      return false;
    }
    if (file.getPath().endsWith(".part")) {
      return true;
    }
    String path = file.getPath();

    for (char alphabetFirst = 'a'; alphabetFirst <= 'z'; alphabetFirst++) {
      for (char alphabetSecond = 'a'; alphabetSecond <= 'z'; alphabetSecond++) {
        String chunkPath = path.substring(0, path.length() - 2) + alphabetFirst + alphabetSecond;
        File fileChunk = new File(chunkPath + ".part");
        if (fileChunk.exists()) {
          return true;
        } else if (!new File(chunkPath).exists()) {
          return false;
        }
      }
    }
    return false;
  }

  public static String getFileName (String fileName) {
    if (new File(fileName).exists()) {
      return fileName;
    } else if (new File(fileName + ".part").exists()) {
      return fileName + ".part";
    } else {
      return fileName + "aa";
    }
  }

  public static long getCurrentSize(Book book) {
    long size = 0;
    List<File> files = getAllZimParts(book);
    for (File file : files) {
      size += file.length();
    }
    return size;
  }

  /**
   * Reads a bundle from the file with the specified
   * name in the persistent storage files directory.
   * This method is a blocking operation and should be used
   * in a io thread.
   *
   * @param app  the application needed to obtain the files directory.
   * @return a valid Bundle loaded using the system class loader
   * or null if the method was unable to read the Bundle from storage.
   */
  @NonNull
  public static Bundle readBundleFromStorage(@NonNull Application app) {
    File inputFile = new File(app.getFilesDir(), BUNDLE_STORAGE);
    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(inputFile);
      Parcel parcel = Parcel.obtain();
      byte[] data = new byte[(int) inputStream.getChannel().size()];

      //noinspection ResultOfMethodCallIgnored
      inputStream.read(data, 0, data.length);
      parcel.unmarshall(data, 0, data.length);
      parcel.setDataPosition(0);
      Bundle out = parcel.readBundle(ClassLoader.getSystemClassLoader());
      out.putAll(out);
      parcel.recycle();
      return out;
    } catch (FileNotFoundException e) {
      Log.e(TAG_KIWIX, "Unable to read bundle from storage");
    } catch (IOException e) {
      Log.e(TAG_KIWIX, "Unable to read bundle from storage", e);
    } finally {
      if (inputStream != null) {
        //noinspection ResultOfMethodCallIgnored
        inputFile.delete();
        try {
          inputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return new Bundle();
  }

  /**
   * Writes a bundle to persistent storage in the files directory
   * using the specified file name. This method is runs blocking operations
   * on io thread.
   *
   * @param app    the application needed to obtain the file directory.
   * @param writeBundle the bundle to store in persistent storage.
   */
  public static void writeBundleToStorage(final @NonNull Application app, Bundle writeBundle) {
    Observable.just(writeBundle)
            .observeOn(Schedulers.io())
            .subscribe(bundle -> {
              File outputFile = new File(app.getFilesDir(), BUNDLE_STORAGE);
              FileOutputStream outputStream = null;
              try {
                outputStream = new FileOutputStream(outputFile);
                Parcel parcel = Parcel.obtain();
                parcel.writeBundle(bundle);
                outputStream.write(parcel.marshall());
                outputStream.flush();
                parcel.recycle();
              } catch (IOException e) {
                Log.e(TAG_KIWIX, "Unable to write bundle to storage");
              } finally {
                if (outputStream != null) {
                  try {
                    outputStream.close();
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                }
              }
            });
  }

  /**
   * Use this method to delete the bundle with the specified name.
   * This is a blocking call and should be used within a io
   * thread.
   *
   * @param app  the application object needed to get the file.
   */
  public static void deleteBundleInStorage(final @NonNull Application app) {
    File outputFile = new File(app.getFilesDir(), BUNDLE_STORAGE);
    if (outputFile.exists()) {
      //noinspection ResultOfMethodCallIgnored
      outputFile.delete();
    }
  }

}
