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

import android.os.Environment;
import android.util.Log;

import org.kiwix.kiwixmobile.ZimContentProvider;
import org.kiwix.kiwixmobile.library.LibraryAdapter;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class FileSearch {

  public static final String TAG_KIWIX = "kiwix";

  // Array of zim file extensions
  public static final String[] zimFiles = { "zim", "zimaa" };

  // Scan through the file system and find all the files with .zim and .zimaa extensions
  public ArrayList<LibraryNetworkEntity.Book> findFiles() {
    final List<String> fileList = new ArrayList<>();
    FilenameFilter[] filter = new FilenameFilter[zimFiles.length];

    // Android doesn't provide an easy way to enumerate additional sdcards
    // present on the device besides the primary one. If enumerating these
    // paths proves insufficient, the alternatives used by some projects
    // is to read and parse contents of /proc/mounts.
    final String[] additionalRoots = {
        "/mnt"
    };

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
        addFilesToFileList(dirName, filter, fileList);
      } else {
        Log.i(TAG_KIWIX, "Skipping missing directory " + dirName);
      }
    }

    return createDataForAdapter(fileList);
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

  // Create an ArrayList with our DataModel
  private ArrayList<LibraryNetworkEntity.Book> createDataForAdapter(List<String> list) {

    ArrayList<LibraryNetworkEntity.Book> data = new ArrayList<>();
    for (String file : list) {
      if (ZimContentProvider.setZimFile(file) != null) {
        LibraryNetworkEntity.Book b = new LibraryNetworkEntity.Book();
        b.title = ZimContentProvider.getZimFileTitle();
        b.id = ZimContentProvider.getId();
        b.file = new File(file);
        b.size = String.valueOf(b.file.length() / 1024);
        b.favicon = ZimContentProvider.getFavicon();
        b.creator = ZimContentProvider.getCreator();
        b.publisher = ZimContentProvider.getPublisher();
        b.date = ZimContentProvider.getDate();
        b.description = ZimContentProvider.getDescription();
        b.language = ZimContentProvider.getLanguage();
        data.add(b);
      }
    }

    data = sortDataModel(data);

    return data;
  }

  // Fill fileList with files found in the specific directory
  private void addFilesToFileList(String directory, FilenameFilter[] filter,
      List<String> fileList) {
    Log.d(TAG_KIWIX, "Searching directory " + directory);
    File[] foundFiles = listFilesAsArray(new File(directory), filter, -1);
    for (File f : foundFiles) {
      Log.d(TAG_KIWIX, "Found " + f.getAbsolutePath());
      fileList.add(f.getAbsolutePath());
    }
  }

  // Remove the file path and the extension and return a file name for the given file path
  private String getTitleFromFilePath(String path) {
    String title = new File(path).getName();
    if (title.charAt(title.length() - 1) == "m".charAt(0)) {
      title = title.replaceFirst("[.][^.]+$", "");
    } else {
      title =  title.replaceFirst("[.][^.]+$", "");
      title =  title.replaceFirst("[.][^.]+$", "");
    }
    return title;
  }
}
