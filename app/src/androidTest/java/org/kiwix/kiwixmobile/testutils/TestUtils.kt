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
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.runner.screenshot.Screenshot
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Created by mhutti1 on 07/04/17.
 */
object TestUtils {
  private const val TAG = "TESTUTILS"
  @JvmField var TEST_PAUSE_MS = 250
  var TEST_PAUSE_MS_FOR_SEARCH_TEST = 1000

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
  private fun hasStoragePermission(): Boolean {
    return ContextCompat.checkSelfPermission(
      InstrumentationRegistry.getInstrumentation().targetContext,
      Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
      InstrumentationRegistry.getInstrumentation().targetContext,
      Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
  }

  @JvmStatic fun allowPermissionsIfNeeded() {
    if (Build.VERSION.SDK_INT >= 23 && !hasStoragePermission()) {
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
    val screenshot = Screenshot.capture().bitmap ?: return
    try {
      val fos = FileOutputStream(outFile)
      screenshot.compress(Bitmap.CompressFormat.PNG, 90, fos)
      fos.close()
    } catch (e: FileNotFoundException) {
      Log.w(TAG, "Failed to save Screenshot", e)
    } catch (e: IOException) {
      Log.w(TAG, "Failed to save Screenshot", e)
    }
  }

  @JvmStatic fun withContent(content: String): Matcher<Any?> {
    return object : BoundedMatcher<Any?, Any?>(Any::class.java) {
      public override fun matchesSafely(myObj: Any?): Boolean {
        if (myObj !is LibraryNetworkEntity.Book) {
          return false
        }
        return if (myObj.getUrl() != null) {
          myObj.getUrl().contains(content)
        } else {
          myObj.file.path.contains(content)
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
}
