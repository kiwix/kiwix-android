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
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import kotlinx.coroutines.runBlocking
import leakcanary.LeakAssertions
import org.hamcrest.Matchers.anyOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.intro.intro
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.utils.StandardActions

class KiwixSettingsFragmentTest {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()

  lateinit var kiwixMainActivity: KiwixMainActivity

  private val permissions =
    arrayOf(
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

  @Rule
  @JvmField
  var permissionRules: GrantPermissionRule =
    GrantPermissionRule.grant(*permissions)

  @Before
  fun setup() {
    // Go to IntroFragment
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(
          InstrumentationRegistry.getInstrumentation().targetContext.applicationContext,
          this
        )
      }
      waitForIdle()
    }
    val kiwixDataStore = KiwixDataStore(
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    ).apply {
      runBlocking {
        setLastDonationPopupShownInMilliSeconds(System.currentTimeMillis())
        setIsScanFileSystemDialogShown(true)
        setIsFirstRun(false)
      }
    }
    PreferenceManager.getDefaultSharedPreferences(
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    ).edit {
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.IS_PLAY_STORE_BUILD, true)
    }
    val activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
      onActivity {
        runBlocking {
          handleLocaleChange(
            it,
            "en",
            kiwixDataStore
          )
        }
      }
    }
    val accessibilityValidator = AccessibilityValidator().setRunChecksFromRootView(true).apply {
      setSuppressingResultMatcher(
        anyOf(
          matchesCheck(DuplicateClickableBoundsCheck::class.java)
        )
      )
    }
    composeTestRule.enableAccessibilityChecks(accessibilityValidator)
    activityScenario.onActivity {
      kiwixMainActivity = it
      it.navigate(KiwixDestination.Intro.route)
    }
    composeTestRule.waitForIdle()
    intro {
      swipeLeft(composeTestRule)
      clickGetStarted(composeTestRule) {}
    }
    StandardActions.openDrawer(kiwixMainActivity as CoreMainActivity)
    StandardActions.enterSettings(composeTestRule)
  }

  @Test
  fun testSettingsActivity() {
    settingsRobo {
      assertZoomTextViewPresent(composeTestRule)
      clickNightModePreference(composeTestRule)
      assertNightModeDialogDisplayed(composeTestRule)
      dismissDialog()
      toggleBackToTopPref(composeTestRule)
      toggleOpenNewTabInBackground(composeTestRule)
      toggleExternalLinkWarningPref(composeTestRule)
      toggleWifiDownloadsOnlyPref(composeTestRule)
      clickExternalStoragePreference(composeTestRule)
      clickInternalStoragePreference(composeTestRule)
      clickClearHistoryPreference(composeTestRule)
      assertHistoryDialogDisplayed(composeTestRule)
      dismissDialog()
      clickClearNotesPreference(composeTestRule)
      assertNotesDialogDisplayed(composeTestRule)
      dismissDialog()
      clickExportBookmarkPreference(composeTestRule)
      assertExportBookmarkDialogDisplayed(composeTestRule)
      dismissDialog()
      clickOnImportBookmarkPreference(composeTestRule)
      assertImportBookmarkDialogDisplayed(composeTestRule)
      dismissDialog()
      clickLanguagePreference(composeTestRule, kiwixMainActivity)
      assertLanguagePrefDialogDisplayed(composeTestRule, kiwixMainActivity)
      dismissDialog()
      assertVersionTextViewPresent(composeTestRule)
      clickCredits(composeTestRule)
      assertContributorsDialogDisplayed(composeTestRule)
      dismissDialog()
    }
    LeakAssertions.assertNoLeaks()
  }
}
