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

import androidx.core.content.ContextCompat;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.runner.screenshot.Screenshot;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import static androidx.test.InstrumentationRegistry.getInstrumentation;

/**
 * Created by mhutti1 on 07/04/17.
 */

public class TestUtils {
  public static int TEST_PAUSE_MS = 250;
  private static final String TAG = "TESTUTILS";
  /*
    TEST_PAUSE_MS is used as such:
        BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    The number 250 is fairly arbitrary. I found 100 to be insufficient, and 250 seems to work on all
    devices I've tried.

    The sleep combats an intermittent issue caused by tests executing before the app/activity is ready.
    This isn't necessary on all devices (particularly more recent ones), however I'm unsure if
    it's speed related, or Android Version related.
   */

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
        } catch (UiObjectNotFoundException e) {
        }
      }
    }
  }

  public static void captureAndSaveScreenshot(String name) {
    File screenshotDir = new File(
        Environment.getExternalStorageDirectory() +
            "/Android/data/KIWIXTEST/Screenshots");

    if (!screenshotDir.exists()) {
      if (!screenshotDir.mkdirs()) {
        return;
      }
    }

    String timestamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
    String fileName = String.format("TEST_%s_%s.png", timestamp, name);

    File outFile = new File(screenshotDir.getPath() + File.separator + fileName);

    Bitmap screenshot = Screenshot.capture().getBitmap();

    if (screenshot == null) {
      return;
    }

    try {
      FileOutputStream fos = new FileOutputStream(outFile);
      screenshot.compress(Bitmap.CompressFormat.PNG, 90, fos);
      fos.close();
    } catch (FileNotFoundException e) {
      Log.w(TAG, "Failed to save Screenshot", e);
    } catch (IOException e) {
      Log.w(TAG, "Failed to save Screenshot", e);
    }
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

