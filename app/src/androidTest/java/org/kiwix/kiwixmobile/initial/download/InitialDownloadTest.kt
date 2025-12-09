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
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import kotlinx.coroutines.launch
import leakcanary.LeakAssertions
import org.hamcrest.Matchers.anyOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.download.downloadRobot
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.ui.KiwixDestination

@LargeTest
class InitialDownloadTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()
  private lateinit var kiwixMainActivity: KiwixMainActivity

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    KiwixDataStore(context).apply {
      lifeCycleScope.launch {
        setWifiOnly(false)
        setIntroShown()
      }
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_STORAGE_OPTION, true)
      putBoolean(SharedPreferenceUtil.IS_PLAY_STORE_BUILD, true)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.PREF_SCAN_FILE_SYSTEM_DIALOG_SHOWN, true)
      putBoolean(SharedPreferenceUtil.PREF_IS_FIRST_RUN, false)
      putString(SharedPreferenceUtil.PREF_LANG, "en")
      putLong(
        SharedPreferenceUtil.PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS,
        System.currentTimeMillis()
      )
    }
    activityScenario =
      ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          handleLocaleChange(
            it,
            "en",
            SharedPreferenceUtil(context)
          )
        }
      }
    val accessibilityValidator = AccessibilityValidator().setRunChecksFromRootView(true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      accessibilityValidator.setSuppressingResultMatcher(
        anyOf(
          matchesCheck(DuplicateClickableBoundsCheck::class.java),
          matchesCheck(SpeakableTextPresentCheck::class.java)
        )
      )
    } else {
      accessibilityValidator.setSuppressingResultMatcher(
        anyOf(
          matchesCheck(DuplicateClickableBoundsCheck::class.java)
        )
      )
    }
    composeTestRule.enableAccessibilityChecks(accessibilityValidator)
  }

  @Test
  fun initialDownloadTest() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
    activityScenario.onActivity {
      kiwixMainActivity = it
      it.navigate(KiwixDestination.Library.route)
    }
    // delete all the ZIM files showing in the LocalLibrary
    // screen to properly test the scenario.
    library {
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
      deleteZimIfExists(composeTestRule)
    }
    downloadRobot {
      clickDownloadOnBottomNav(composeTestRule)
      waitForDataToLoad(composeTestRule = composeTestRule)
      stopDownloadIfAlreadyStarted(composeTestRule, kiwixMainActivity)
      downloadZimFile(composeTestRule)
    }
    initialDownload {
      assertStorageConfigureDialogDisplayed(composeTestRule)
      clickOnInternalStorage(composeTestRule)
      downloadRobot {
        assertDownloadStart(composeTestRule)
        stopDownload(composeTestRule)
        assertStopDownloadDialogDisplayed(composeTestRule, kiwixMainActivity)
        clickOnYesButton(composeTestRule)
      }
      assertDownloadStop(composeTestRule)
    }
    LeakAssertions.assertNoLeaks()
  }

  @After
  fun finish() {
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }
}
