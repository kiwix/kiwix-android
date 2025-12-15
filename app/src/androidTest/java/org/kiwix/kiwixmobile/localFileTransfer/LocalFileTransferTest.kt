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
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
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
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
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
  private val lifeCycleScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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

  @Before
  fun setup() {
    context = instrumentation.targetContext.applicationContext
    UiDevice.getInstance(instrumentation).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    val accessibilityValidator = AccessibilityValidator().setRunChecksFromRootView(true).apply {
      setSuppressingResultMatcher(
        anyOf(
          matchesCheck(DuplicateClickableBoundsCheck::class.java)
        )
      )
    }
    composeTestRule.enableAccessibilityChecks(accessibilityValidator)
  }

  @Test
  fun localFileTransfer() {
    shouldShowShowCaseFeatureToUser(false)
    activityScenario =
      ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          kiwixMainActivity = it
          runBlocking {
            handleLocaleChange(
              it,
              "en",
              KiwixDataStore(it)
            )
          }
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
          runBlocking {
            handleLocaleChange(
              it,
              "en",
              KiwixDataStore(it)
            )
          }
          it.navigate(KiwixDestination.Library.route)
        }
      }
    StandardActions.closeDrawer(kiwixMainActivity as CoreMainActivity)
    library {
      assertGetZimNearbyDeviceDisplayed(composeTestRule)
      clickFileTransferIcon(composeTestRule) {
        assertClickNearbyDeviceMessageVisible(composeTestRule)
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        clickOnNextButton(composeTestRule)
        assertDeviceNameMessageVisible(composeTestRule)
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        clickOnNextButton(composeTestRule)
        assertNearbyDeviceListMessageVisible(composeTestRule)
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        clickOnNextButton(composeTestRule)
        assertTransferZimFilesListMessageVisible(composeTestRule)
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
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
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
      }
    }
  }

  private fun shouldShowShowCaseFeatureToUser(shouldShowShowCase: Boolean) {
    KiwixDataStore(context).apply {
      lifeCycleScope.launch {
        setWifiOnly(false)
        setIntroShown()
        setShowCaseViewForFileTransferShown(shouldShowShowCase)
        setPrefLanguage("en")
        setIsScanFileSystemDialogShown(true)
        setIsFirstRun(false)
      }
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
    }
  }
}
