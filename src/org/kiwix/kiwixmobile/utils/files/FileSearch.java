/*
 * Copyright 2013 Rashiq Ahmad <rashiq.z@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.kiwix.kiwixmobile.utils.files;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import org.kiwix.kiwixmobile.ZimContentProvider;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import eu.mhutti1.utils.storage.StorageDevice;
import eu.mhutti1.utils.storage.StorageDeviceUtils;

public class FileSearch {

  public static final String TAG_KIWIX = "kiwix";

  // Array of zim file extensions
  public static final String[] zimFiles = {"zim", "zimaa"};


  private final Context context;
  private final ResultListener listener;

  private boolean fileSystemScanCompleted = false;
  private boolean mediaStoreScanCompleted = false;

  public FileSearch(Context ctx, ResultListener listener) {
    this.context = ctx;
    this.listener = listener;
  }

  public void scan() {

    new Thread(new Runnable() {
      @Override
      public void run() {
        scanFileSystem();
        fileSystemScanCompleted = true;
        checkCompleted();
      }
    }).start();

    new Thread(new Runnable() {
      @Override
      public void run() {
        scanMediaStore();
        mediaStoreScanCompleted = true;
        checkCompleted();
      }
    }).start();
  }

  private synchronized void checkCompleted() {
    if (mediaStoreScanCompleted && fileSystemScanCompleted)
      listener.onScanCompleted();
  }

  public void scanMediaStore() {
    ContentResolver contentResolver = context.getContentResolver();
    Uri uri = MediaStore.Files.getContentUri("external");

    String[] projection = {MediaStore.MediaColumns.DATA};
    String selection = MediaStore.MediaColumns.DATA + " like ? or "+MediaStore.MediaColumns.DATA +" like ? ";

    Cursor query = contentResolver.query(uri, projection, selection, new String[]{"%."+zimFiles[0], "%."+zimFiles[1]}, null);

    try {
      while (query.moveToNext()) {
        File file = new File(query.getString(0));

        if (file.canRead())
          onFileFound(file.getAbsolutePath());
      }
    } finally {
      query.close();
    }
  }

  // Scan through the file system and find all the files with .zim and .zimaa extensions
  public void scanFileSystem() {
    FilenameFilter[] filter = new FilenameFilter[zimFiles.length];

    // Search all external directories that we can find.
    String[] tempRoots = new String[StorageDeviceUtils.getStorageDevices((Activity) context, false).size() + 1];
    int j = 0;
    tempRoots[j] = "/mnt";
    for (StorageDevice storageDevice : StorageDeviceUtils.getStorageDevices((Activity) context, false)) {
      j++;
      tempRoots[j] = storageDevice.getName();
    }

    final String[] additionalRoots = tempRoots;

    int i = 0;
    for (final String extension : zimFiles) {
      filter[i] = new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.endsWith("." + extension);
        }
      };
      i++;
    }

    String dirNamePrimary = new File(
            Environment.getExternalStorageDirectory().getAbsolutePath()).toString();
    //        addFilesToFileList(dirNamePrimary, filter, fileList);

    for (final String dirName : additionalRoots) {
      if (dirNamePrimary.equals(dirName)) {
        // We already got this directory from getExternalStorageDirectory().
        continue;
      }
      File f = new File(dirName);
      if (f.isDirectory()) {
        scanDirectory(dirName, filter);
      } else {
        Log.i(TAG_KIWIX, "Skipping missing directory " + dirName);
      }
    }
  }

  public ArrayList<LibraryNetworkEntity.Book> sortDataModel(ArrayList<LibraryNetworkEntity.Book> data) {

    // Sorting the data in alphabetical order
    Collections.sort(data, new Comparator<LibraryNetworkEntity.Book>() {
      @Override
      public int compare(LibraryNetworkEntity.Book a, LibraryNetworkEntity.Book b) {
        return a.getTitle().compareToIgnoreCase(b.getTitle());
      }
    });

    return data;
  }

  // Iterate through the file system
  private Collection<File> listFiles(File directory, FilenameFilter[] filter, int recurse) {

    Vector<File> files = new Vector<>();

    File[] entries = directory.listFiles();

    if (entries != null) {
      for (File entry : entries) {
        for (FilenameFilter filefilter : filter) {
          if (filter == null || filefilter.accept(directory, entry.getName())) {
            files.add(entry);
          }
        }
        if ((recurse <= -1) || (recurse > 0 && entry.isDirectory())) {
          recurse--;
          files.addAll(listFiles(entry, filter, recurse));
          recurse++;
        }
      }
    }
    return files;
  }

  private File[] listFilesAsArray(File directory, FilenameFilter[] filter, int recurse) {
    Collection<File> files = listFiles(directory, filter, recurse);

    File[] arr = new File[files.size()];
    return files.toArray(arr);
  }

  public static synchronized LibraryNetworkEntity.Book fileToBook(String filePath) {
    LibraryNetworkEntity.Book book = null;

    if (ZimContentProvider.zimFileName != null) {
      ZimContentProvider.originalFileName = ZimContentProvider.zimFileName;
    }
    // Check a file isn't being opened and temporally use content provider to access details
    // This is not a great solution as we shouldn't need to fully open our ZIM files to get their metadata
    if (ZimContentProvider.canIterate) {
      if (ZimContentProvider.setZimFile(filePath) != null) {
        book = new LibraryNetworkEntity.Book();
        book.title = ZimContentProvider.getZimFileTitle();
        book.id = ZimContentProvider.getId();
        book.file = new File(filePath);
        book.size = String.valueOf(ZimContentProvider.getFileSize());
        book.favicon = ZimContentProvider.getFavicon();
        book.creator = ZimContentProvider.getCreator();
        book.publisher = ZimContentProvider.getPublisher();
        book.date = ZimContentProvider.getDate();
        book.description = ZimContentProvider.getDescription();
        book.language = ZimContentProvider.getLanguage();
      }
    }
    // Return content provider to its previous state
    if (!ZimContentProvider.originalFileName.equals("")) {
      ZimContentProvider.setZimFile(ZimContentProvider.originalFileName);
    }
    ZimContentProvider.originalFileName = "";

    return book;
  }

  // Fill fileList with files found in the specific directory
  private void scanDirectory(String directory, FilenameFilter[] filter) {
    Log.d(TAG_KIWIX, "Searching directory " + directory);
    File[] foundFiles = listFilesAsArray(new File(directory), filter, -1);
    for (File f : foundFiles) {
      Log.d(TAG_KIWIX, "Found " + f.getAbsolutePath());
      onFileFound(f.getAbsolutePath());
    }
  }

  // Remove the file path and the extension and return a file name for the given file path
  private String getTitleFromFilePath(String path) {
    String title = new File(path).getName();
    if (title.charAt(title.length() - 1) == "m".charAt(0)) {
      title = title.replaceFirst("[.][^.]+$", "");
    } else {
      title = title.replaceFirst("[.][^.]+$", "");
      title = title.replaceFirst("[.][^.]+$", "");
    }
    return title;
  }

  public void onFileFound(String filePath) {
    LibraryNetworkEntity.Book book = fileToBook(filePath);

    if (book != null)
      listener.onBookFound(book);
  }

  public interface ResultListener {
    void onBookFound(LibraryNetworkEntity.Book book);

    void onScanCompleted();
  }
}
