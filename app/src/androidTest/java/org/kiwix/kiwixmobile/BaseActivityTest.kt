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

package org.kiwix.kiwixmobile

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import okhttp3.internal.trimSubstring
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.di.components.DaggerTestComponent
import org.kiwix.kiwixmobile.core.di.components.TestComponent
import org.kiwix.kiwixmobile.main.KiwixMainActivity

@RunWith(AndroidJUnit4::class)
abstract class BaseActivityTest {
  @get:Rule
  open var activityRule = ActivityTestRule(KiwixMainActivity::class.java)

  @get:Rule
  var readPermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(permission.READ_EXTERNAL_STORAGE)

  @get:Rule
  var writePermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(permission.WRITE_EXTERNAL_STORAGE)

  val context: Context by lazy {
    getInstrumentation().targetContext.applicationContext
  }
  private lateinit var uiDevice: UiDevice

  @BeforeClass
  @Test
  fun setUpBeforeClass() {
    uiDevice = UiDevice.getInstance(getInstrumentation())
    registerANRWatcher()
  }

  private fun registerANRWatcher() {
    uiDevice.registerWatcher("ANR") {
      val anrDialog = uiDevice.findObject(
        UiSelector()
          .packageName("android")
          .textContains(anrText)
      )
      checkForAnrDialogToClose(anrDialog)
    }
  }

  private fun closeAnrWithWait(anrDialog: UiObject): Boolean {
    Log.i(TAG, "ANR dialog detected!")
    try {
      uiDevice.findObject(
        UiSelector().text("Wait").className("android.widget.Button").packageName(
          "android"
        )
      ).click()
      val anrDialogText = anrDialog.text
      val appName: String = anrDialogText.trimSubstring(0, anrDialogText.length - anrText.length)
      Log.i(TAG, "Application $appName is not responding!")
    } catch (e: UiObjectNotFoundException) {
      Log.i(TAG, "Detected ANR, but window disappeared!")
    }
    Log.i(TAG, "ANR dialog closed: pressed on wait!")
    return true
  }

  private fun checkForAnrDialogToClose(anrDialog: UiObject): Boolean =
    anrDialog.exists() && closeAnrWithWait(anrDialog)

  protected inline fun <reified T : Activity> activityTestRule(
    noinline beforeActivityAction: (() -> Unit)? = null
  ) =
    object : ActivityTestRule<T>(T::class.java) {
      override fun beforeActivityLaunched() {
        super.beforeActivityLaunched()
        beforeActivityAction?.invoke()
      }
    }

  protected fun testComponent(): TestComponent = DaggerTestComponent.builder()
    .context(context)
    .build()

  companion object {
    const val TAG = "ui_test_tag"
    private const val anrText = "isn't responding"
  }
}
