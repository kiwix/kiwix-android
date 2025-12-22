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

package org.kiwix.kiwixmobile.onlineCategory

import android.Manifest
import android.app.Instrumentation
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
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
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.download.downloadRobot
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.ui.KiwixDestination

@LargeTest
@RunWith(AndroidJUnit4::class)
class OnlineCategoryTest {
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

  lateinit var kiwixMainActivity: KiwixMainActivity
  private val instrumentation: Instrumentation by lazy(InstrumentationRegistry::getInstrumentation)

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
  fun setUp() {
    UiDevice.getInstance(instrumentation).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(instrumentation.targetContext.applicationContext, this)
      }
      waitForIdle()
    }
    PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext.applicationContext)
      .edit {
        putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
        putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
        putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
        putBoolean(SharedPreferenceUtil.PREF_SCAN_FILE_SYSTEM_DIALOG_SHOWN, true)
        putBoolean(SharedPreferenceUtil.PREF_IS_FIRST_RUN, false)
        putString(SharedPreferenceUtil.PREF_LANG, "en")
        putLong(
          SharedPreferenceUtil.PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS,
          System.currentTimeMillis()
        )
      }
    ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
      onActivity {
        kiwixMainActivity = it
        handleLocaleChange(
          it,
          "en",
          SharedPreferenceUtil(instrumentation.targetContext.applicationContext)
        )
      }
    }
  }

  @Test
  fun testOnlineCategory() {
    composeTestRule.runOnUiThread {
      kiwixMainActivity.navigate(KiwixDestination.Downloads.route)
    }
    downloadRobot {
      waitForDataToLoad(composeTestRule = composeTestRule)
    }
    category {
      clickOnCategoryMenuIcon(composeTestRule)
      assertCategoryDialogDisplayed(composeTestRule)
      waitForCategoryToLoad(composeTestRule)
      // Select the "All language" category to freshly run the test case.
      selectCategory(composeTestRule, "")
      // Select "Gutenberg" category.
      clickOnCategoryMenuIcon(composeTestRule)
      assertCategoryDialogDisplayed(composeTestRule)
      selectCategory(composeTestRule, "gutenberg")
      // assert category is selected.
      clickOnCategoryMenuIcon(composeTestRule)
      assertCategoryDialogDisplayed(composeTestRule)
      assertCategorySelected(composeTestRule, "gutenberg")
    }
  }
}
