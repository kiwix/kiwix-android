/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.error

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesViews
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible

class ErrorActivityTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    PreferenceManager.getDefaultSharedPreferences(context)
      .edit {
        putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
        putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
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
            SharedPreferenceUtil(context).apply {
              lastDonationPopupShownInMilliSeconds = System.currentTimeMillis()
            }
          )
        }
      }
  }

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

  @Test
  fun verifyErrorActivity() {
    activityScenario.onActivity {
      it.navigate(R.id.helpFragment)
    }
    errorActivity {
      assertSendDiagnosticReportDisplayed(composeTestRule)
      clickOnSendDiagnosticReport(composeTestRule)
      assertErrorActivityDisplayed(composeTestRule)
      // Click on "No, Thanks" button to see it's functionality working or not.
      clickOnNoThanksButton(composeTestRule)
      // Handle the app restart explicitly. Since test case does not handle the app restart.
      activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).onActivity {
        it.navigate(R.id.helpFragment)
      }
      // Assert HelpFragment is visible or not after clicking on the "No, Thanks" button.
      assertSendDiagnosticReportDisplayed(composeTestRule)
      // Again click on "Send diagnostic report" button to open the ErrorActivity.
      clickOnSendDiagnosticReport(composeTestRule)
      assertErrorActivityDisplayed(composeTestRule)
      // Check check boxes are displayed or not.
      assertCheckBoxesDisplayed(composeTestRule)
      // Click on "Send details" button.
      clickOnSendDetailsButton(composeTestRule)
    }
  }
}
