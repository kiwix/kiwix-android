/*
 * Copyright 2016 Isaac Hutt <mhutti1@gmail.com>
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

package eu.mhutti1.utils.storage;

import android.content.Context;
import android.os.Environment;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;

public class StorageDeviceUtils {

  public static ArrayList<StorageDevice> getStorageDevices(Context context, boolean writable) {
    ArrayList<StorageDevice> storageDevices = new ArrayList<>();

    // Add as many possible mount points as we know about

    // Only add this device if its very likely that we have missed a users sd card
    if (Environment.isExternalStorageEmulated()) {
      // This is our internal storage directory
      storageDevices.add(new StorageDevice(
          generalisePath(Environment.getExternalStorageDirectory().getPath(), writable), true));
    } else {
      // This is an external storage directory
      storageDevices.add(new StorageDevice(
          generalisePath(Environment.getExternalStorageDirectory().getPath(), writable), false));
    }

    // These are possible manufacturer sdcard mount points

    String[] paths = ExternalPaths.getPossiblePaths();

    for (String path : paths) {
      if (path.endsWith("*")) {
        File root = new File(path.substring(0, path.length() - 1));
        File[] directories = root.listFiles(file -> file.isDirectory());
        if (directories != null) {
          for (File dir : directories) {
            storageDevices.add(new StorageDevice(dir, false));
          }
        }
      } else {
        storageDevices.add(new StorageDevice(path, false));
      }
    }

    // Iterate through any sdcards manufacturers may have specified
    for (File file : ContextCompat.getExternalFilesDirs(context, "")) {
      if (file != null) {
        storageDevices.add(new StorageDevice(generalisePath(file.getPath(), writable), false));
      }
    }

    // Check all devices exist, we can write to them if required and they are not duplicates
    return checkStorageValid(writable, storageDevices);
  }

  // Remove app specific path from directories so that we can search them from the top
  private static String generalisePath(String path, boolean writable) {
    if (writable) {
      return path;
    }
    int endIndex = path.lastIndexOf("/Android/data/");
    if (endIndex != -1) {
      return path.substring(0, endIndex);
    }
    return path;
  }

  private static ArrayList<StorageDevice> checkStorageValid(boolean writable,
      ArrayList<StorageDevice> storageDevices) {
    ArrayList<StorageDevice> activeDevices = new ArrayList<>();
    ArrayList<StorageDevice> devicePaths = new ArrayList<>();
    for (StorageDevice device : storageDevices) {
      if (existsAndIsDirAndWritableIfRequiredAndNotDuplicate(writable, devicePaths, device)) {
        activeDevices.add(device);
        devicePaths.add(device);
      }
    }
    return activeDevices;
  }

  private static boolean existsAndIsDirAndWritableIfRequiredAndNotDuplicate(boolean writable,
      ArrayList<StorageDevice> devicePaths, StorageDevice device) {
    final File devicePath = device.getPath();
    return devicePath.exists()
        && devicePath.isDirectory()
        && (canWrite(devicePath) || !writable)
        && !device.isDuplicate()
        && !devicePaths.contains(device);
  }

  // Amazingly file.canWrite() does not always return the correct value
  private static boolean canWrite(File file) {
    final String filePath = file + "/test.txt";
    try {
      RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "rw");
      FileChannel fileChannel = randomAccessFile.getChannel();
      FileLock fileLock = fileChannel.lock();
      fileLock.release();
      fileChannel.close();
      randomAccessFile.close();
      return true;
    } catch (Exception ex) {
      return false;
    } finally {
      new File(filePath).delete();
    }
  }
}

