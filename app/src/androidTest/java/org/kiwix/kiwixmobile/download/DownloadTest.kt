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
package org.kiwix.kiwixmobile.download

import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import com.adevinta.android.barista.interaction.BaristaSwipeRefreshInteractions.refresh
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.utils.KiwixIdlingResource.Companion.getInstance
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class DownloadTest : BaseActivityTest() {

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle()
    PreferenceManager.getDefaultSharedPreferences(
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    ).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      putBoolean(SharedPreferenceUtil.PREF_SHOW_STORAGE_OPTION, false)
      putBoolean(SharedPreferenceUtil.IS_PLAY_STORE_BUILD, true)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
    }
  }

  @Test
  fun downloadTest() {
    ActivityScenario.launch(KiwixMainActivity::class.java)
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    try {
      downloadRobot {
        clickLibraryOnBottomNav()
        deleteZimIfExists()
        clickDownloadOnBottomNav()
        waitForDataToLoad()
        scrollToAlpineWikiZim()
        downloadZimFile()
        assertDownloadStart()
        waitUntilDownloadComplete()
        clickLibraryOnBottomNav()
        checkIfZimFileDownloaded()
      }
    } catch (e: Exception) {
      Assert.fail(
        """
        Couldn't find downloaded file 'A little question a day'
        Original Exception:
        ${e.localizedMessage}
        """.trimIndent()
      )
    }
    try {
      refresh(R.id.zim_swiperefresh)
    } catch (e: RuntimeException) {
      Log.w(KIWIX_DOWNLOAD_TEST, "Failed to refresh ZIM list: " + e.localizedMessage)
    }
  }

  @After
  fun finish() {
    IdlingRegistry.getInstance().unregister(getInstance())
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.IS_PLAY_STORE_BUILD, false)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, false)
    }
  }

  companion object {
    private const val KIWIX_DOWNLOAD_TEST = "kiwixDownloadTest"

    @BeforeClass
    fun beforeClass() {
      IdlingPolicies.setMasterPolicyTimeout(180, TimeUnit.SECONDS)
      IdlingPolicies.setIdlingResourceTimeout(180, TimeUnit.SECONDS)
      IdlingRegistry.getInstance().register(getInstance())
    }
  }
}
