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
package org.kiwix.kiwixmobile.download

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
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
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.main.topLevel
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.utils.KiwixIdlingResource.Companion.getInstance
import java.util.concurrent.TimeUnit

const val TWO_MINUTES_IN_MILLISECONDS = 2 * 60 * 1000

@LargeTest
class DownloadTest : BaseActivityTest() {
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
    KiwixDataStore(
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    ).apply {
      lifeCycleScope.launch {
        setWifiOnly(false)
        setIntroShown()
        setPrefLanguage("en")
        setLastDonationPopupShownInMilliSeconds(System.currentTimeMillis())
        setIsScanFileSystemDialogShown(true)
        setShowStorageOption(false)
        setIsFirstRun(false)
        setIsPlayStoreBuild(true)
        setPrefIsTest(true)
        setSelectedOnlineContentCategory("")
      }
    }
    activityScenario =
      ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
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
  fun downloadTest() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    activityScenario.onActivity {
      kiwixMainActivity = it
      it.navigate(KiwixDestination.Library.route)
    }
    try {
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
        try {
          assertDownloadStart(composeTestRule)
          pauseDownload(composeTestRule)
          assertDownloadPaused(composeTestRule, activityScenario)
          resumeDownload(composeTestRule)
          assertDownloadResumed(composeTestRule, kiwixMainActivity)
          waitUntilDownloadComplete(
            composeTestRule = composeTestRule,
            kiwixMainActivity = kiwixMainActivity
          )
        } catch (ignore: Exception) {
          // do nothing as ZIM file already downloaded, since we are downloading the smallest file
          // so it can be downloaded immediately after starting.
          Log.e(
            KIWIX_DOWNLOAD_TEST,
            "Could not pause download. Original exception = $ignore"
          )
        }
        UiThreadStatement.runOnUiThread {
          kiwixMainActivity.navigate(KiwixDestination.Library.route)
        }
        // refresh the local library list to show the downloaded zim file
        library { refreshList(composeTestRule) }
        checkIfZimFileDownloaded(composeTestRule)
      }
    } catch (e: Exception) {
      Assert.fail(
        "Couldn't find downloaded file\n Original Exception: ${e.message}"
      )
    }
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
      LeakAssertions.assertNoLeaks()
    }
  }

  @Test
  fun testPauseAndResumeInOtherLanguage() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
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
      // change the application language
      topLevel {
        clickSettingsOnSideNav(kiwixMainActivity as CoreMainActivity, composeTestRule, true) {
          clickLanguagePreference(composeTestRule, activityScenario)
          assertLanguagePrefDialogDisplayed(composeTestRule, activityScenario)
          selectDeviceDefaultLanguage(composeTestRule)
          // Advance the main clock to settle the frame of compose.
          composeTestRule.mainClock.advanceTimeByFrame()
          composeTestRule.waitUntil(timeoutMillis = 5_000) {
            activityScenario.state.isAtLeast(Lifecycle.State.RESUMED)
          }
          composeTestRule.waitForIdle()
          activityScenario.onActivity {
            kiwixMainActivity = it
          }
          clickLanguagePreference(composeTestRule, activityScenario)
          assertLanguagePrefDialogDisplayed(composeTestRule, activityScenario)
          selectAlbanianLanguage(composeTestRule)
          // Advance the main clock to settle the frame of compose.
          composeTestRule.mainClock.advanceTimeByFrame()
          composeTestRule.waitUntil(timeoutMillis = 5_000) {
            activityScenario.state.isAtLeast(Lifecycle.State.RESUMED)
          }
          composeTestRule.waitForIdle()
          activityScenario.onActivity {
            kiwixMainActivity = it
            it.onBackPressedDispatcher.onBackPressed()
          }
        }
      }
      clickDownloadOnBottomNav(composeTestRule)
      waitForDataToLoad(composeTestRule = composeTestRule)
      stopDownloadIfAlreadyStarted(composeTestRule, kiwixMainActivity)
      downloadZimFile(composeTestRule)
      assertDownloadStart(composeTestRule)
      pauseDownload(composeTestRule)
      assertDownloadPaused(composeTestRule, activityScenario)
      resumeDownload(composeTestRule)
      assertDownloadResumed(composeTestRule, kiwixMainActivity)
      stopDownloadIfAlreadyStarted(composeTestRule, kiwixMainActivity)
      // select the default device language to perform other test cases.
      topLevel {
        clickSettingsOnSideNav(kiwixMainActivity as CoreMainActivity, composeTestRule, true) {
          clickLanguagePreference(composeTestRule, activityScenario)
          assertLanguagePrefDialogDisplayed(composeTestRule, activityScenario)
          selectDeviceDefaultLanguage(composeTestRule)
          // Advance the main clock to settle the frame of compose.
          composeTestRule.mainClock.advanceTimeByFrame()
          composeTestRule.waitUntil(timeoutMillis = 5_000) {
            activityScenario.state.isAtLeast(Lifecycle.State.RESUMED)
          }
          composeTestRule.waitForIdle()
          activityScenario.onActivity {
            kiwixMainActivity = it
          }
          // close the language dialog.
          composeTestRule.runOnUiThread {
            kiwixMainActivity.onBackPressedDispatcher.onBackPressed()
          }
        }
      }
    }
  }

  @Test
  fun downloadZIMFileInBackground() {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
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
        searchZappingSauvageFile(composeTestRule)
        downloadZimFile(composeTestRule)
        assertDownloadStart(composeTestRule)
      }
      // press the home button so that application goes into background
      InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(
        AccessibilityService.GLOBAL_ACTION_HOME
      )
      // wait for 2 minutes to download the ZIM file in background.
      composeTestRule.waitUntilTimeout(TWO_MINUTES_IN_MILLISECONDS.toLong())
      // relaunch the application.
      val context = ApplicationProvider.getApplicationContext<Context>()
      val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
      intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
      context.startActivity(intent)
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      activityScenario.onActivity {
        kiwixMainActivity = it
        it.navigate(KiwixDestination.Library.route)
      }
      library {
        refreshList(composeTestRule)
        waitUntilZimFilesRefreshing(composeTestRule)
        downloadRobot {
          runCatching {
            checkIfZimFileDownloaded(composeTestRule)
          }.onFailure {
            // if currently downloading check.
            clickDownloadOnBottomNav(composeTestRule)
            waitForDataToLoad(composeTestRule = composeTestRule)
            stopDownloadIfAlreadyStarted(composeTestRule, kiwixMainActivity)
          }
        }
      }
    }
  }

  @After
  fun finish() {
    IdlingRegistry.getInstance().unregister(getInstance())
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }

  companion object {
    const val KIWIX_DOWNLOAD_TEST = "kiwixDownloadTest"

    @BeforeClass
    fun beforeClass() {
      IdlingPolicies.setMasterPolicyTimeout(180, TimeUnit.SECONDS)
      IdlingPolicies.setIdlingResourceTimeout(180, TimeUnit.SECONDS)
      IdlingRegistry.getInstance().register(getInstance())
    }
  }
}
