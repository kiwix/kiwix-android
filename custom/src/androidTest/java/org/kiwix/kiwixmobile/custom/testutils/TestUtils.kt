/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.custom.testutils

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.io.File

object TestUtils {
  private const val TAG = "TESTUTILS"
  var TEST_PAUSE_MS_FOR_SEARCH_TEST = 1000

  @JvmStatic
  fun isSystemUINotRespondingDialogVisible(uiDevice: UiDevice) =
    uiDevice.findObject(By.textContains("System UI isn't responding")) != null ||
      uiDevice.findObject(By.textContains("Process system isn't responding")) != null ||
      uiDevice.findObject(By.textContains("Launcher isn't responding")) != null ||
      uiDevice.findObject(By.textContains("Wait")) != null ||
      uiDevice.findObject(By.textContains("WAIT")) != null ||
      uiDevice.findObject(By.textContains("OK")) != null ||
      uiDevice.findObject(By.textContains("Ok")) != null ||
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

  @JvmStatic
  fun testFlakyView(
    action: () -> Unit,
    retryCount: Int = 5
  ) {
    try {
      action()
    } catch (ignore: Throwable) {
      if (retryCount > 0) {
        testFlakyView(action, retryCount - 1)
      } else {
        throw ignore // No more retries, rethrow the exception
      }
    }
  }

  @JvmStatic
  fun deleteTemporaryFilesOfTestCases(context: Context) {
    ContextCompat.getExternalFilesDirs(context, null).filterNotNull()
      .map(::deleteAllFilesInDirectory)
    ContextWrapper(context).externalMediaDirs.filterNotNull()
      .map(::deleteAllFilesInDirectory)
  }

  private fun deleteAllFilesInDirectory(directory: File) {
    if (directory.isDirectory) {
      directory.listFiles()?.forEach { file ->
        if (file.isDirectory) {
          // Recursively delete files in subdirectories
          deleteAllFilesInDirectory(file)
        }
        file.delete()
      }
    }
  }
}
