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
package org.kiwix.kiwixmobile.testutils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.runner.screenshot.Screenshot;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.v4.content.ContextCompat;
import android.util.Log;


import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

/**
 * Created by mhutti1 on 07/04/17.
 */

public class TestUtils {
  private static String TAG = "TESTUTILS";

  public static boolean hasStoragePermission() {
    return ContextCompat.checkSelfPermission(InstrumentationRegistry.getTargetContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(InstrumentationRegistry.getTargetContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
  }

  public static void allowPermissionsIfNeeded() {
    if (Build.VERSION.SDK_INT >= 23 && !hasStoragePermission()) {
      UiDevice device = UiDevice.getInstance(getInstrumentation());
      UiObject allowPermissions = device.findObject(new UiSelector().clickable(true).checkable(false).index(1));
      if (allowPermissions.exists()) {
        try {
          allowPermissions.click();
        } catch (UiObjectNotFoundException e) {}
      }
    }
  }

  public static void captureAndSaveScreenshot(String name){
    storeImage(
        getOutputMediaFile(name),
        Screenshot.capture().getBitmap()
    );
  }

  // https://stackoverflow.com/questions/15662258/how-to-save-a-bitmap-on-internal-storage#15662384
  private static void storeImage(File pictureFile, Bitmap image) {
    if (pictureFile == null) {
      Log.d(TAG,
              "Error creating media file, check storage permissions: ");// e.getMessage());
      return;
    }
    try {
      FileOutputStream fos = new FileOutputStream(pictureFile);
      image.compress(Bitmap.CompressFormat.PNG, 90, fos);
      fos.close();
    } catch (FileNotFoundException e) {
      Log.d(TAG, "File not found: " + e.getMessage());
    } catch (IOException e) {
      Log.d(TAG, "Error accessing file: " + e.getMessage());
    }
  }

  private static File getOutputMediaFile(String name){
    // To be safe, you should check that the SDCard is mounted
    // using Environment.getExternalStorageState() before doing this.
    File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
            + "/Android/data/"
            + "KIWIXTEST"
            + "/Files");

    // This location works best if you want the created images to be shared
    // between applications and persist after your app has been uninstalled.

    // Create the storage directory if it does not exist
    if (! mediaStorageDir.exists()){
      if (! mediaStorageDir.mkdirs()){
        return null;
      }
    }
    // Create a media file name
    String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
    File mediaFile;
    String mImageName = "MI_" + timeStamp + "_" + name + ".jpg";
    mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
    return mediaFile;
  }

  public static Matcher<Object> withContent(final String content) {
    return new BoundedMatcher<Object, Object>(Object.class) {
      @Override
      public boolean matchesSafely(Object myObj) {
        if (!(myObj instanceof Book)) {
          return false;
        }
        Book book = (Book) myObj;
        if (book.getUrl() != null) {
          return book.getUrl().contains(content);
        } else {
          return book.file.getPath().contains(content);
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("with content '" + content + "'");
      }
    };
  }

  public static String getResourceString(int id) {
    Context targetContext = InstrumentationRegistry.getTargetContext();
    return targetContext.getResources().getString(id);
  }

}

