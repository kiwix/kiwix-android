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

import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesViews
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import leakcanary.LeakAssertions
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.main.topLevel
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.utils.KiwixIdlingResource.Companion.getInstance
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class DownloadTest : BaseActivityTest() {

  @Rule
  @JvmField
  var retryRule = RetryRule()

  init {
    AccessibilityChecks.enable().apply {
      setRunChecksFromRootView(true)
      setSuppressingResultMatcher(
        allOf(
          matchesCheck(DuplicateClickableBoundsCheck::class.java),
          matchesViews(ViewMatchers.withId(R.id.get_zim_nearby_device))
        )
      )
    }
  }

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    PreferenceManager.getDefaultSharedPreferences(
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    ).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      putBoolean(SharedPreferenceUtil.PREF_SHOW_STORAGE_OPTION, false)
      putBoolean(SharedPreferenceUtil.IS_PLAY_STORE_BUILD, true)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.PREF_PLAY_STORE_RESTRICTION, false)
      putString(SharedPreferenceUtil.PREF_LANG, "en")
      putLong(
        SharedPreferenceUtil.PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS,
        System.currentTimeMillis()
      )
    }
    activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
      onActivity {
        handleLocaleChange(
          it,
          "en",
          SharedPreferenceUtil(context)
        )
      }
    }
  }

  @Test
  fun downloadTest() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    activityScenario.onActivity {
      it.navigate(R.id.libraryFragment)
    }
    try {
      downloadRobot(DownloadRobot::refreshLocalLibraryData)
      // delete all the ZIM files showing in the LocalLibrary
      // screen to properly test the scenario.
      library {
        waitUntilZimFilesRefreshing()
        deleteZimIfExists()
      }
      downloadRobot {
        clickDownloadOnBottomNav()
        refreshOnlineList()
        waitForDataToLoad()
        stopDownloadIfAlreadyStarted()
        downloadZimFile()
        assertDownloadStart()
        pauseDownload()
        assertDownloadPaused()
        resumeDownload()
        assertDownloadResumed()
        waitUntilDownloadComplete()
        clickLibraryOnBottomNav()
        // refresh the local library list to show the downloaded zim file
        refreshLocalLibraryData()
        checkIfZimFileDownloaded()
      }
    } catch (e: Exception) {
      Assert.fail(
        "Couldn't find downloaded file\n Original Exception: ${e.message}"
      )
    }
    LeakAssertions.assertNoLeaks()
  }

  @Test
  fun testPauseAndResumeInOtherLanguage() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    activityScenario.onActivity {
      it.navigate(R.id.libraryFragment)
    }
    try {
      downloadRobot(DownloadRobot::refreshLocalLibraryData)
      // delete all the ZIM files showing in the LocalLibrary
      // screen to properly test the scenario.
      library {
        waitUntilZimFilesRefreshing()
        deleteZimIfExists()
      }
      downloadRobot {
        // change the application language
        topLevel {
          clickSettingsOnSideNav {
            clickLanguagePreference()
            assertLanguagePrefDialogDisplayed()
            selectDeviceDefaultLanguage()
            clickLanguagePreference()
            assertLanguagePrefDialogDisplayed()
            selectAlbanianLanguage()
          }
        }
        clickDownloadOnBottomNav()
        refreshOnlineList()
        waitForDataToLoad()
        stopDownloadIfAlreadyStarted()
        downloadZimFile()
        assertDownloadStart()
        pauseDownload()
        assertDownloadPaused()
        resumeDownload()
        assertDownloadResumed()
        stopDownloadIfAlreadyStarted()
        // select the default device language to perform other test cases.
        topLevel {
          clickSettingsOnSideNav {
            clickLanguagePreference()
            assertLanguagePrefDialogDisplayed()
            selectDeviceDefaultLanguage()
            // check if the device default language is selected or not.
            clickLanguagePreference()
            // close the language dialog.
            pressBack()
          }
        }
      }
    } catch (e: Exception) {
      Assert.fail(
        "Couldn't find downloaded file ' Off the Grid ' Original Exception: ${e.message}"
      )
    }
    LeakAssertions.assertNoLeaks()
  }

  @After
  fun finish() {
    IdlingRegistry.getInstance().unregister(getInstance())
  }

  companion object {
    const val KIWIX_DOWNLOAD_TEST = "kiwixDownloadTest"

    @BeforeClass
    fun beforeClass() {
      IdlingPolicies.setMasterPolicyTimeout(180, TimeUnit.SECONDS)
      IdlingPolicies.setIdlingResourceTimeout(180, TimeUnit.SECONDS)
      IdlingRegistry.getInstance().register(getInstance())
    }
  }
}
