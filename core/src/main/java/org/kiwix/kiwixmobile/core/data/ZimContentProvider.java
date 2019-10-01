/*
 * Copyright 2013
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

package org.kiwix.kiwixmobile.core.data;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.inject.Inject;
import org.kiwix.kiwixlib.JNIKiwix;
import org.kiwix.kiwixmobile.core.KiwixApplication;
import org.kiwix.kiwixmobile.core.zim_manager.ZimReaderContainer;

import static org.kiwix.kiwixmobile.core.utils.Constants.TAG_KIWIX;

public class ZimContentProvider extends AbstractContentProvider {

  @Inject
  public JNIKiwix jniKiwix;
  @Inject
  ZimReaderContainer zimReaderContainer;

  @Override
  public boolean onCreate() {
    KiwixApplication.getApplicationComponent().inject(this);
    setIcuDataDirectory();
    return true;
  }

  @Override
  public String getType(Uri uri) {
    return zimReaderContainer.readMimeType(uri);
  }

  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode) {
    return zimReaderContainer.load(uri);
  }

  private void setIcuDataDirectory() {
    String icuDirPath = loadICUData(getContext());
    if (icuDirPath != null) {
      Log.d(TAG_KIWIX, "Setting the ICU directory path to " + icuDirPath);
      jniKiwix.setDataDirectory(icuDirPath);
    }
  }

  private String loadICUData(Context context) {
    try {
      File icuDir = new File(context.getFilesDir(), "icu");
      if (!icuDir.exists()) {
        icuDir.mkdirs();
      }
      String[] icuFileNames = context.getAssets().list("icu");
      for (int i = 0; i < icuFileNames.length; i++) {
        String icuFileName = icuFileNames[i];
        File icuDataFile = new File(icuDir, icuFileName);
        if (!icuDataFile.exists()) {
          InputStream in = context.getAssets().open("icu/" + icuFileName);
          OutputStream out = new FileOutputStream(icuDataFile);
          byte[] buf = new byte[1024];
          int len;
          while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
          }
          in.close();
          out.flush();
          out.close();
        }
      }
      return icuDir.getAbsolutePath();
    } catch (Exception e) {
      Log.w(TAG_KIWIX, "Error copying icu data file", e);
      //TODO: Consider surfacing to user
      return null;
    }
  }

  private static String getFulltextIndexPath(String file) {
    String[] names = { file, file };

    /* File might be a ZIM chunk like foobar.zimaa */
    if (!names[0].substring(names[0].length() - 3).equals("zim")) {
      names[0] = names[0].substring(0, names[0].length() - 2);
    }

    /* Try to find a *.idx fulltext file/directory beside the ZIM
     * file. Returns <zimfile>.zim.idx or <zimfile>.zimaa.idx. */
    for (String name : names) {
      File f = new File(name + ".idx");
      if (f.exists() && f.isDirectory()) {
        return f.getPath();
      }
    }

    /* If no separate fulltext index file found then returns the ZIM
     * file path itself (embedded fulltext index) */
    return file;
  }
}
