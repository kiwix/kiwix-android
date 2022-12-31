/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.initial.download

import android.os.Build
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import leakcanary.LeakAssertions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule

@LargeTest
@RunWith(AndroidJUnit4::class)
class InitialDownloadTest : BaseActivityTest() {
  override var activityRule: ActivityTestRule<KiwixMainActivity> = activityTestRule {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      putBoolean(SharedPreferenceUtil.PREF_SHOW_STORAGE_OPTION, true)
      putBoolean(SharedPreferenceUtil.IS_PLAY_STORE_BUILD, true)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
    }
  }

  @Rule
  @JvmField
  var retryRule = RetryRule()

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle()
  }

  @Test
  fun initialDownloadTest() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      initialDownload {
        clickLibraryOnBottomNav()
        // This is for if download test fails for some reason after downloading the zim file
        deleteZimIfExists()
        clickDownloadOnBottomNav()
        assertLibraryListDisplayed()
        refreshList()
        waitForDataToLoad()
        downloadZimFile()
        assertStorageConfigureDialogDisplayed()
        clickOnYesToConfirm()
        assertDownloadStart()
        stopDownload()
        assertStopDownloadDialogDisplayed()
        clickOnYesToConfirm()
        assertDownloadStop()
      }
      LeakAssertions.assertNoLeaks()
    }
  }

  @After
  fun setPrefStorageOption() {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_STORAGE_OPTION, false)
      putBoolean(SharedPreferenceUtil.IS_PLAY_STORE_BUILD, false)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, false)
    }
  }
}
