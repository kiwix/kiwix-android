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
package org.kiwix.kiwixmobile.settings

import android.Manifest
import android.os.Build
import android.view.Gravity
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.intro.IntroRobot
import org.kiwix.kiwixmobile.intro.intro
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS
import org.kiwix.kiwixmobile.utils.StandardActions

class KiwixSettingsFragmentTest {
  @Rule @JvmField var activityTestRule = ActivityTestRule(
    KiwixMainActivity::class.java
  )

  @Rule @JvmField var readPermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)

  @Rule @JvmField var writePermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

  @Before fun setup() = runBlocking {
    waitForUIDeviceToIdle()
    UiThreadStatement.runOnUiThread {
      activityTestRule.activity.navigate(R.id.introFragment) // Go to IntroFragment
    }
    intro(IntroRobot::swipeLeft) clickGetStarted { }
    waitForUIDeviceToIdle()
    grantPermission()
    clickExternalStorageDialogIfPresent()
    StandardActions.openDrawer(R.id.navigation_container, Gravity.LEFT)
    StandardActions.enterSettings() // Go to SettingFragment
  }

  @Test
  fun testSettingsActivity() {
    settingsRobo {
      assertZoomTextViewPresent()
      assertVersionTextViewPresent()
      clickLanguagePreference()
      assertLanguagePrefDialogDisplayed()
      dismissDialog()
      toggleBackToTopPref()
      toggleOpenNewTabInBackground()
      toggleExternalLinkWarningPref()
      toggleWifiDownloadsOnlyPref()
      clickStoragePreference()
      // Let's pause here for a moment because calculating storage takes some time
      sleepForMoment()
      assertStorageDialogDisplayed()
      dismissDialog()
      clickClearHistoryPreference()
      assertHistoryDialogDisplayed()
      dismissDialog()
      clickNightModePreference()
      assertNightModeDialogDisplayed()
      dismissDialog()
      clickCredits()
      assertContributorsDialogDisplayed()
      dismissDialog()
    }
  }

  /**
   * Waits for the application to idle.
   */
  private fun waitForUIDeviceToIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle()
  }

  /**
   * Sleeps for to finish the job.
   * @param timeToSleep the amount of sleep
   */
  private fun sleepForMoment(timeToSleep: Int = TEST_PAUSE_MS) {
    BaristaSleepInteractions.sleep(timeToSleep.toLong())
  }

  /**
   * Checks if the Build version is greater than [Build.VERSION_CODES.R] and clicks depends on the
   * [positive] parameter.Note that if you pass true to  the [positive] parameter then it goes
   * to system's setting screen. You should handle the app accordingly.
   * @param positive Clicks on the positive or negative button.
   */
  private fun clickExternalStorageDialogIfPresent(positive: Boolean = false) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val text = if (positive) "YES" else "NO"
      val dialogObject: UiObject? =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
          .findObject(UiSelector().text(text))
      dialogObject?.click()
    } else {
      // There was No ManageExternalFilesPermission yet. It was a great time :))
      UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
    }
  }

  private fun grantPermission() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    if (Build.VERSION.SDK_INT >= 23) {
      val allowPermission = UiDevice.getInstance(instrumentation).findObject(
        UiSelector().text(
          when {
            Build.VERSION.SDK_INT == 23 -> "Allow"
            Build.VERSION.SDK_INT <= 28 -> "ALLOW"
            Build.VERSION.SDK_INT == 29 -> "Allow only while using the app"
            else -> "While using the app"
          }
        )
      )
      if (allowPermission.exists()) {
        allowPermission.click()
      }
    }
  }
}
