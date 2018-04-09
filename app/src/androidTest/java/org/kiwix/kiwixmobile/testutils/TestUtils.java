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
import android.app.Activity;
import android.app.LauncherActivity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.core.internal.deps.guava.collect.Iterables;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.v4.content.ContextCompat;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;

import java.util.concurrent.TimeoutException;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;

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

  public static Activity getCurrentActivity(ActivityTestRule mActivityTestRule) throws Throwable { // https://stackoverflow.com/a/41415288
    final Activity[] activity = new Activity[1];
    mActivityTestRule.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        java.util.Collection<Activity> activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
        activity[0] = Iterables.getOnlyElement(activities);
      }
    });
    return activity[0];
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


  public static ViewInteraction waitFor(final Matcher matcher, final int waitTimeoutMillis, final int retryRate) throws Throwable {
    try {
      return onView(matcher);
    } catch (Throwable e) {
      Thread.sleep(retryRate);
      if(waitTimeoutMillis > 0 && (waitTimeoutMillis - retryRate > 0)) {
        return waitFor(matcher, waitTimeoutMillis - retryRate, retryRate);
      } else {
        throw new TimeoutException("Timed out waiting for matcher: " + matcher.toString());
      }
    }
  }

  public static ViewInteraction waitFor(final Matcher matcher, final int waitTimeoutMillis) throws Throwable {
    return waitFor(matcher, waitTimeoutMillis, waitTimeoutMillis / 4);
  }

  public static ViewInteraction waitFor(final Matcher matcher) throws Throwable {
    return waitFor(matcher, 1000, 1000 / 4);
  }
}

