package org.kiwix.kiwixmobile.testutils;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;

import android.os.Build;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

/**
 * Created by mhutti1 on 07/04/17.
 */

public class TestUtils {

  public static void preGrantStorage() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      getInstrumentation().getUiAutomation().executeShellCommand(
          "pm grant " + getTargetContext().getPackageName()
              + " android.permission.READ_EXTERNAL_STORAGE");
      getInstrumentation().getUiAutomation().executeShellCommand(
          "pm grant " + getTargetContext().getPackageName()
              + " android.permission.WRITE_EXTERNAL_STORAGE");
    }
  }

  public static void allowPermissionsIfNeeded() {
    if (Build.VERSION.SDK_INT >= 23) {
      UiDevice device = UiDevice.getInstance(getInstrumentation());
      UiObject allowPermissions = device.findObject(new UiSelector().text("Allow"));
      if (allowPermissions.exists()) {
        try {
          allowPermissions.click();
        } catch (UiObjectNotFoundException e) {
        }
      }
    }
  }
}
