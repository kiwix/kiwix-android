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
package org.kiwix.kiwixmobile.language

import android.Manifest
import android.app.Instrumentation
import android.os.Build
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import leakcanary.LeakAssertions
import org.hamcrest.Matchers.anyOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.download.downloadRobot
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.utils.StandardActions

@LargeTest
@RunWith(AndroidJUnit4::class)
class LanguageFragmentTest {
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
  private val lifeCycleScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  @Before
  fun setUp() {
    UiDevice.getInstance(instrumentation).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(instrumentation.targetContext.applicationContext, this)
      }
      waitForIdle()
    }
    val kiwixDataStore = KiwixDataStore(
      instrumentation.targetContext.applicationContext
    ).apply {
      lifeCycleScope.launch {
        setWifiOnly(false)
        setIntroShown()
        setPrefLanguage("en")
        setLastDonationPopupShownInMilliSeconds(System.currentTimeMillis())
      }
    }
    PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext.applicationContext)
      .edit {
        putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
        putBoolean(SharedPreferenceUtil.PREF_SCAN_FILE_SYSTEM_DIALOG_SHOWN, true)
        putBoolean(SharedPreferenceUtil.PREF_IS_FIRST_RUN, false)
      }
    ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
      onActivity {
        kiwixMainActivity = it
        runBlocking {
          handleLocaleChange(
            it,
            "en",
            kiwixDataStore
          )
        }
      }
    }
    val accessibilityValidator = AccessibilityValidator().setRunChecksFromRootView(true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
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
  fun testLanguageFragment() {
    StandardActions.closeDrawer(kiwixMainActivity as CoreMainActivity) // close the drawer if open before running the test cases.
    downloadRobot {
      clickDownloadOnBottomNav(composeTestRule)
      waitForDataToLoad(composeTestRule = composeTestRule)
    }
    language {
      // search and de-select if german language already selected
      clickOnLanguageIcon(composeTestRule)
      waitForLanguageToLoad(composeTestRule)
      clickOnLanguageSearchIcon(composeTestRule)
      searchLanguage(
        composeTestRule = composeTestRule,
        searchLanguage = "german"
      )
      selectLanguage(
        composeTestRule = composeTestRule,
        matchLanguage = "German"
      )
      clickOnSaveLanguageIcon(composeTestRule)
      // test if the selected language filter is applied on the online library or not.
      downloadRobot {
        composeTestRule.waitUntilTimeout(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong())
        waitForDataToLoad(composeTestRule = composeTestRule)
        checkLanguageFilterAppliedToOnlineContent(
          composeTestRule,
          context.getString(R.string.your_language, "German")
        )
      }

      clickOnLanguageIcon(composeTestRule)
      waitForLanguageToLoad(composeTestRule)
      clickOnLanguageSearchIcon(composeTestRule)
      searchLanguage(
        composeTestRule = composeTestRule,
        searchLanguage = "italiano"
      )
      selectLanguage(
        composeTestRule = composeTestRule,
        matchLanguage = "Italian"
      )
      clickOnSaveLanguageIcon(composeTestRule)

      // test if the selected language filter is applied on the online library or not.
      downloadRobot {
        composeTestRule.waitUntilTimeout(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong())
        waitForDataToLoad(composeTestRule = composeTestRule)
        checkLanguageFilterAppliedToOnlineContent(
          composeTestRule,
          context.getString(R.string.your_language, "Italian")
        )
      }
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      LeakAssertions.assertNoLeaks()
    }
  }
}
