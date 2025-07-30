/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.localFileTransfer

import android.Manifest
import android.app.Instrumentation
import android.content.Context
import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
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
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.utils.StandardActions

class LocalFileTransferTest {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var activityScenario: ActivityScenario<KiwixMainActivity>

  private val permissions =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      arrayOf(
        Manifest.permission.NEARBY_WIFI_DEVICES
      )
    } else {
      arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION
      )
    }

  @Rule
  @JvmField
  var permissionRules: GrantPermissionRule =
    GrantPermissionRule.grant(*permissions)

  lateinit var kiwixMainActivity: KiwixMainActivity

  private val instrumentation: Instrumentation by lazy {
    InstrumentationRegistry.getInstrumentation()
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

  @Before
  fun setup() {
    context = instrumentation.targetContext.applicationContext
    UiDevice.getInstance(instrumentation).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
  }

  @Test
  fun localFileTransfer() {
    shouldShowShowCaseFeatureToUser(false)
    activityScenario =
      ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          kiwixMainActivity = it
          handleLocaleChange(
            it,
            "en",
            SharedPreferenceUtil(context)
          )
        }
      }
    StandardActions.closeDrawer(kiwixMainActivity as CoreMainActivity)
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
      activityScenario.onActivity {
        it.navigate(KiwixDestination.Library.route)
      }
      library {
        assertGetZimNearbyDeviceDisplayed(composeTestRule)
        clickFileTransferIcon(composeTestRule) {
          assertReceiveFileTitleVisible(composeTestRule)
          assertSearchDeviceMenuItemVisible(composeTestRule)
          clickOnSearchDeviceMenuItem(composeTestRule)
          assertLocalFileTransferScreenVisible(composeTestRule)
          pressBack()
          assertLocalLibraryVisible(composeTestRule)
        }
      }
      LeakAssertions.assertNoLeaks()
    }
  }

  @Test
  fun showCaseFeature() {
    shouldShowShowCaseFeatureToUser(true)
    activityScenario =
      ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          kiwixMainActivity = it
          handleLocaleChange(
            it,
            "en",
            SharedPreferenceUtil(context)
          )
          it.navigate(KiwixDestination.Library.route)
        }
      }
    StandardActions.closeDrawer(kiwixMainActivity as CoreMainActivity)
    library {
      assertGetZimNearbyDeviceDisplayed(composeTestRule)
      clickFileTransferIcon(composeTestRule) {
        assertClickNearbyDeviceMessageVisible(composeTestRule)
        clickOnNextButton(composeTestRule)
        assertDeviceNameMessageVisible(composeTestRule)
        clickOnNextButton(composeTestRule)
        assertNearbyDeviceListMessageVisible(composeTestRule)
        clickOnNextButton(composeTestRule)
        assertTransferZimFilesListMessageVisible(composeTestRule)
        clickOnNextButton(composeTestRule)
        pressBack()
        assertGetZimNearbyDeviceDisplayed(composeTestRule)
      }
    }
    LeakAssertions.assertNoLeaks()
  }

  @Test
  fun testShowCaseFeatureShowOnce() {
    shouldShowShowCaseFeatureToUser(false)
    activityScenario =
      ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          kiwixMainActivity = it
          it.navigate(KiwixDestination.Library.route)
        }
      }
    StandardActions.closeDrawer(kiwixMainActivity as CoreMainActivity)
    library {
      // test show case view show once.
      clickFileTransferIcon(composeTestRule) {
        assertClickNearbyDeviceMessageNotVisible(composeTestRule)
      }
    }
  }

  private fun shouldShowShowCaseFeatureToUser(shouldShowShowCase: Boolean) {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.PREF_SHOW_SHOWCASE, shouldShowShowCase)
      putString(SharedPreferenceUtil.PREF_LANG, "en")
    }
  }
}
