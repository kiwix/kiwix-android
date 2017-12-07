package org.kiwix.kiwixmobile.testutils;

import android.Manifest;
import android.app.LauncherActivity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.v4.content.ContextCompat;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

/**
 * Created by mhutti1 on 07/04/17.
 */

public class TestUtils {
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
}
