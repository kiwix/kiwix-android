/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
 *
 */
package org.kiwix.kiwixmobile.testutils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.test.core.app.canTakeScreenshot
import androidx.test.core.app.takeScreenshot
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.By.textContains
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Created by mhutti1 on 07/04/17.
 */
object TestUtils {
  private const val TAG = "TESTUTILS"
  @JvmField var TEST_PAUSE_MS = 3000
  var TEST_PAUSE_MS_FOR_SEARCH_TEST = 1000
  var TEST_PAUSE_MS_FOR_DOWNLOAD_TEST = 10000
  const val RETRY_COUNT_FOR_FLAKY_TEST = 3

  /*
    TEST_PAUSE_MS is used as such:
        BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    The number 250 is fairly arbitrary. I found 100 to be insufficient,
        and 250 seems to work on all devices I've tried.

    The sleep combats an intermittent issue caused by
        tests executing before the app/activity is ready.
    This isn't necessary on all devices (particularly more recent ones),
        however I'm unsure if
    it's speed related, or Android Version related.
   */

  private fun hasReadExternalStoragePermission(): Boolean =
    ContextCompat.checkSelfPermission(
      InstrumentationRegistry.getInstrumentation().targetContext,
      Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED

  private fun hasWriteExternalStoragePermission(): Boolean =
    ContextCompat.checkSelfPermission(
      InstrumentationRegistry.getInstrumentation().targetContext,
      Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED

  @RequiresApi(Build.VERSION_CODES.R)
  private fun hasManageExternalStoragePermission(): Boolean =
    Environment.isExternalStorageManager()

  @JvmStatic fun hasStoragePermission() = Build.VERSION.SDK_INT > Build.VERSION_CODES.M &&
    hasReadExternalStoragePermission() && hasWriteExternalStoragePermission()

  @JvmStatic fun allowStoragePermissionsIfNeeded() {
    if (!hasStoragePermission()) {
      val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
      val allowPermissions =
        device.findObject(
          UiSelector().clickable(true)
            .checkable(false).index(1)
        )
      if (allowPermissions.exists()) {
        try {
          allowPermissions.click()
        } catch (e: UiObjectNotFoundException) {
          Log.w(TAG, "Unable to find allow permission dialog", e)
        }
      }
    }
  }

  @JvmStatic fun captureAndSaveScreenshot(name: String?) {
    val screenshotDir = File(
      Environment.getExternalStorageDirectory().toString() +
        "/Android/data/KIWIXTEST/Screenshots"
    )
    if (!screenshotDir.exists()) {
      if (!screenshotDir.mkdirs()) {
        return
      }
    }
    val timestamp = SimpleDateFormat("ddMMyyyy_HHmm").format(Date())
    val fileName = "TEST_${timestamp}_$name.png"
    val outFile = File(screenshotDir.path + File.separator + fileName)
    if (!canTakeScreenshot()) return
    val screenshot = takeScreenshot()
    var fos: OutputStream? = null
    try {
      fos = FileOutputStream(outFile)
      screenshot.compress(Bitmap.CompressFormat.PNG, 90, fos)
    } catch (e: FileNotFoundException) {
      Log.w(TAG, "Unable to create file $outFile", e)
    } finally {
      fos?.close()
    }
  }

  @JvmStatic fun withContent(content: String): Matcher<Any?> {
    return object : BoundedMatcher<Any?, Any?>(Any::class.java) {
      public override fun matchesSafely(myObj: Any?): Boolean {
        if (myObj !is LibraryNetworkEntity.Book) {
          return false
        }
        return if (myObj.url != null) {
          myObj.url?.contains(content) == true
        } else {
          myObj.file?.path?.contains(content) == true
        }
      }

      override fun describeTo(description: Description) {
        description.appendText("with content '$content'")
      }
    }
  }

  @JvmStatic fun getResourceString(id: Int): String {
    val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    return targetContext.resources.getString(id)
  }

  @JvmStatic
  fun isSystemUINotRespondingDialogVisible(uiDevice: UiDevice) =
    uiDevice.findObject(textContains("System UI isn't responding")) != null ||
      uiDevice.findObject(textContains("Process system isn't responding")) != null ||
      uiDevice.findObject(textContains("Launcher isn't responding")) != null ||
      uiDevice.findObject(By.clazz("android.app.Dialog")) != null

  @JvmStatic
  fun closeSystemDialogs(context: Context?, uiDevice: UiDevice) {
    // Close any system dialogs visible on Android versions below 12 by broadcasting
    context?.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
    // Press the back button as most dialogs can be closed by doing so
    uiDevice.pressBack()
    try {
      // Click on the button of system dialog (Especially applicable to non-closable dialogs)
      val waitButton = getSystemDialogButton(uiDevice)
      if (waitButton?.exists() == true) {
        uiDevice.click(waitButton.bounds.centerX(), waitButton.bounds.centerY())
      }
    } catch (ignore: Exception) {
      Log.d(
        TAG,
        "Couldn't click on Wait/OK button, probably no system dialog is " +
          "visible with Wait/OK button \n$ignore"
      )
    }
  }

  private fun getSystemDialogButton(uiDevice: UiDevice): UiObject? {
    // All possible button text based on different Android versions.
    val possibleButtonTextList = arrayOf("Wait", "WAIT", "OK", "Ok")
    return possibleButtonTextList
      .asSequence()
      .map { uiDevice.findObject(UiSelector().textContains(it)) }
      .firstOrNull(UiObject::exists)
  }
}
