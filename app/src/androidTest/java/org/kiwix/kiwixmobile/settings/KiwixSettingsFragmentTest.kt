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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesViews
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import leakcanary.LeakAssertions
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.intro.intro
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.utils.StandardActions

class KiwixSettingsFragmentTest {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()

  private val permissions =
    arrayOf(
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

  @Rule
  @JvmField
  var permissionRules: GrantPermissionRule =
    GrantPermissionRule.grant(*permissions)

  init {
    AccessibilityChecks.enable().apply {
      setRunChecksFromRootView(true)
      setSuppressingResultMatcher(
        anyOf(
          allOf(
            matchesCheck(TouchTargetSizeCheck::class.java),
            matchesViews(withContentDescription("More options"))
          ),
          matchesCheck(SpeakableTextPresentCheck::class.java)
        )
      )
    }
  }

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
    val activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
      onActivity {
        handleLocaleChange(
          it,
          "en",
          SharedPreferenceUtil(it).apply {
            setIsPlayStoreBuildType(true)
            lastDonationPopupShownInMilliSeconds = System.currentTimeMillis()
          }
        )
      }
    }
    activityScenario.onActivity {
      it.navigate(R.id.introFragment)
    }
    intro {
      swipeLeft(composeTestRule)
      clickGetStarted(composeTestRule) {}
    }
    StandardActions.openDrawer()
    StandardActions.enterSettings()
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
      clickLanguagePreference(composeTestRule)
      assertLanguagePrefDialogDisplayed(composeTestRule)
      dismissDialog()
      assertVersionTextViewPresent(composeTestRule)
      clickCredits(composeTestRule)
      assertContributorsDialogDisplayed(composeTestRule)
      dismissDialog()
    }
    LeakAssertions.assertNoLeaks()
  }
}
