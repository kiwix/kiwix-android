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

import android.os.Build;
import android.os.StatFs;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

public class StorageDevice {

  // File object containing device path
  private final File mFile;

  private final boolean mInternal;

  private boolean mDuplicate = true;

  public StorageDevice(String path, boolean internal) {
    mFile = new File(path);
    mInternal = internal;
    if (mFile.exists()) {
      createLocationCode();
    }
  }

  public StorageDevice(File file, boolean internal) {
    mFile = file;
    mInternal = internal;
    if (mFile.exists()) {
      createLocationCode();
    }
  }

  // Get device path
  public String getName() {
    return mFile.getPath();
  }

  // Get available space on device
  public String getSize() {
    return bytesToHuman(getAvailableBytes());
  }

  private Long getAvailableBytes() {
    StatFs statFs = new StatFs(mFile.getPath());
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      return statFs.getBlockSizeLong() * statFs.getAvailableBlocksLong();
    } else {
      return (long) statFs.getBlockSize() * (long) statFs.getAvailableBlocks();
    }
  }

  public String getTotalSize() {
    return bytesToHuman(getTotalBytes());
  }

  // Get total space on device
  private Long getTotalBytes() {
    StatFs statFs = new StatFs((mFile.getPath()));
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      return statFs.getBlockSizeLong() * statFs.getBlockCountLong();
    } else {
      return (long) statFs.getBlockSize() * (long) statFs.getBlockCount();
    }
  }

  // Convert bytes to human readable form
  private static String bytesToHuman(long size) {
    long Kb = 1 * 1024;
    long Mb = Kb * 1024;
    long Gb = Mb * 1024;
    long Tb = Gb * 1024;
    long Pb = Tb * 1024;
    long Eb = Pb * 1024;

    if (size < Kb) return floatForm(size) + " byte";
    if (size >= Kb && size < Mb) return floatForm((double) size / Kb) + " KB";
    if (size >= Mb && size < Gb) return floatForm((double) size / Mb) + " MB";
    if (size >= Gb && size < Tb) return floatForm((double) size / Gb) + " GB";
    if (size >= Tb && size < Pb) return floatForm((double) size / Tb) + " TB";
    if (size >= Pb && size < Eb) return floatForm((double) size / Pb) + " PB";
    if (size >= Eb) return floatForm((double) size / Eb) + " EB";

    return "???";
  }

  public boolean isInternal() {
    return mInternal;
  }

  public File getPath() {
    return mFile;
  }

  private static String floatForm(double d) {
    return new DecimalFormat("#.#").format(d);
  }

  // Create unique file to identify duplicate devices.
  private void createLocationCode() {
    if (!getLocationCodeFromFolder(mFile)) {
      File locationCode = new File(mFile.getPath(), ".storageLocationMarker");
      try {
        locationCode.createNewFile();
        FileWriter fw = new FileWriter(locationCode);
        fw.write(mFile.getPath());
        fw.close();
      } catch (IOException e) {
        Log.d("android-storage-devices", "Unable to create marker file, duplicates may be listed");
      }
    }
  }

  // Check if there is already a device code in our path
  private boolean getLocationCodeFromFolder(File folder) {
    File locationCode = new File(folder.getPath(), ".storageLocationMarker");
    if (locationCode.exists()) {
      try ( BufferedReader br = new BufferedReader(new FileReader(locationCode))){
        if (br.readLine().equals(mFile.getPath())) {
          mDuplicate = false;
        } else {
          mDuplicate = true;
          return true;
        }
      } catch (Exception e) {
        return true;
      }
    }
    String path = folder.getPath();
    String parent = path.substring(0, path.lastIndexOf("/"));
    if (parent.equals("")) {
      mDuplicate = false;
      return false;
    }
    return getLocationCodeFromFolder(new File(parent));
  }

  public boolean isDuplicate() {
    return mDuplicate;
  }
}
