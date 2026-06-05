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

import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import leakcanary.LeakAssertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.splash.splash
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.utils.StandardActions

class KiwixSettingsFragmentTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()

  lateinit var kiwixMainActivity: KiwixMainActivity

  @Before
  override fun waitForIdle() {
    super.waitForIdle()
    launchMainActivity()
    composeTestRule.enableAccessibilityChecks(createAccessibilityValidator())
    // Go to IntroScreen
    activityScenario.onActivity {
      kiwixMainActivity = it
      it.navigate(KiwixDestination.Intro.route)
    }
    composeTestRule.waitForIdle()
    splash {
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
      assertExternalStorageSelected(composeTestRule)
      clickInternalStoragePreference(composeTestRule)
      assertInternalStorageSelected(composeTestRule)
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
      clickLanguagePreference(composeTestRule, activityScenario)
      assertLanguagePrefDialogDisplayed(composeTestRule, activityScenario)
      dismissDialog()
      assertVersionTextViewPresent(composeTestRule)
      clickCredits(composeTestRule)
      assertContributorsDialogDisplayed(composeTestRule)
      dismissDialog()
    }
    LeakAssertions.assertNoLeaks()
  }
}
