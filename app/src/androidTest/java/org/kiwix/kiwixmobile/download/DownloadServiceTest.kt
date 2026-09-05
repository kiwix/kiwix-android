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

package org.kiwix.kiwixmobile.download

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadMonitorService
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.HILT_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.utils.KiwixIdlingResource.Companion.getInstance
import java.util.concurrent.TimeUnit

@LargeTest
@HiltAndroidTest
class DownloadServiceTest : BaseActivityTest() {
  @Rule(order = HILT_RULE_ORDER)
  @JvmField
  val hiltRule = HiltAndroidRule(this)

  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @Rule(order = COMPOSE_TEST_RULE_ORDER)
  @JvmField
  val composeTestRule = createComposeRule()

  private lateinit var kiwixMainActivity: KiwixMainActivity

  @Before
  override fun waitForIdle() {
    hiltRule.injectOnce()
    super.waitForIdle()
    updateKiwixDataStore {
      setShowStorageOption(false)
      setSelectedOnlineContentCategory("")
      setSelectedOnlineContentLanguage("")
    }
    launchMainActivity()
    composeTestRule.enableAccessibilityChecks(createAccessibilityValidator())
  }

  @Test
  fun downloadServiceTest() {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
      activityScenario.onActivity {
        kiwixMainActivity = it
        it.navigate(KiwixDestination.Library.route)
      }
      // Delete leftover ZIM files first, or a stale D3.js Docs download hides the
      // online search result and downloadZimFile() below times out - see #5088.
      library {
        refreshList(composeTestRule)
        waitUntilZimFilesRefreshing(composeTestRule)
        deleteZimIfExists(composeTestRule)
      }
      activityScenario.onActivity {
        it.navigate(KiwixDestination.Downloads.route)
      }
      downloadRobot {
        waitForDataToLoad(composeTestRule = composeTestRule)
        stopDownloadIfAlreadyStarted(composeTestRule, kiwixMainActivity)
        searchD3JsDocsFile(composeTestRule)
        downloadZimFile(composeTestRule)
        assertDownloadStart(composeTestRule)
      }
      assetDownloadService(true)
      // relaunch the application.
      val context = ApplicationProvider.getApplicationContext<Context>()
      val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
      intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
      context.startActivity(intent)
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          kiwixMainActivity = it
          it.navigate(KiwixDestination.Downloads.route)
        }
      }
      downloadRobot {
        waitForDataToLoad(composeTestRule = composeTestRule)
        stopDownloadIfAlreadyStarted(composeTestRule, kiwixMainActivity)
      }
      assetDownloadService(false)
    }
  }

  @After
  fun finish() {
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }

  private fun assetDownloadService(isRunning: Boolean) {
    // press the home button so that application goes into background
    InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(
      AccessibilityService.GLOBAL_ACTION_HOME
    )
    // Poll instead of a fixed sleep - a wait that's enough on one API level/device
    // isn't guaranteed enough on a slower one.
    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      DownloadMonitorService.isDownloadMonitorServiceRunning == isRunning
    }
    composeTestRule.waitUntilTimeout(3000)
  }

  companion object {
    @BeforeClass
    fun beforeClass() {
      IdlingPolicies.setMasterPolicyTimeout(180, TimeUnit.SECONDS)
      IdlingPolicies.setIdlingResourceTimeout(180, TimeUnit.SECONDS)
      IdlingRegistry.getInstance().register(getInstance())
    }
  }
}
