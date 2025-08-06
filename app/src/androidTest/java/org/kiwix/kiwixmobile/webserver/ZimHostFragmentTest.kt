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

package org.kiwix.kiwixmobile.webserver

import android.Manifest
import android.content.Context
import android.os.Build
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
import org.junit.After
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
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class ZimHostFragmentTest {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()

  private lateinit var sharedPreferenceUtil: SharedPreferenceUtil

  private lateinit var activityScenario: ActivityScenario<KiwixMainActivity>

  lateinit var kiwixMainActivity: KiwixMainActivity

  private val permissions =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.NEARBY_WIFI_DEVICES,
        Manifest.permission.ACCESS_NETWORK_STATE
      )
    } else {
      arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE
      )
    }

  @Rule
  @JvmField
  var permissionRules: GrantPermissionRule =
    GrantPermissionRule.grant(*permissions)
  private var context: Context? = null

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
  fun waitForIdle() {
    context = InstrumentationRegistry.getInstrumentation().targetContext
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    context?.let {
      sharedPreferenceUtil =
        SharedPreferenceUtil(it).apply {
          setIntroShown()
          putPrefWifiOnly(false)
          setIsPlayStoreBuildType(true)
          prefIsTest = true
          putPrefLanguage("en")
          lastDonationPopupShownInMilliSeconds = System.currentTimeMillis()
        }
    }
    activityScenario =
      ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          handleLocaleChange(
            it,
            "en",
            SharedPreferenceUtil(it)
          )
        }
      }
  }

  @Test
  fun testZimHostFragment() {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1 &&
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    ) {
      activityScenario.onActivity {
        kiwixMainActivity = it
        it.navigate(KiwixDestination.Library.route)
      }
      StandardActions.closeDrawer(kiwixMainActivity as CoreMainActivity) // close the drawer if open before running the test cases.
      // delete all the ZIM files showing in the LocalLibrary
      // screen to properly test the scenario.
      library {
        refreshList(composeTestRule)
        waitUntilZimFilesRefreshing(composeTestRule)
        deleteZimIfExists(composeTestRule)
      }
      loadZimFileInApplication("testzim.zim")
      loadZimFileInApplication("small.zim")
      zimHost {
        refreshLibraryList(composeTestRule)
        assertZimFilesLoaded(composeTestRule)
        openZimHostFragment(kiwixMainActivity as CoreMainActivity, composeTestRule)

        // Check if server is already started
        stopServerIfAlreadyStarted(composeTestRule)

        // Check if both zim file are selected or not to properly run our test case
        selectZimFileIfNotAlreadySelected(composeTestRule)

        clickOnTestZim(composeTestRule)

        // Start the server with one ZIM file
        startServer(composeTestRule)
        assertServerStarted(composeTestRule)

        // Check that only one ZIM file is hosted on the server
        assertItemHostedOnServer(1, composeTestRule)

        // Check QR code shown
        assertQrShown(composeTestRule)

        // Stop the server
        stopServer(composeTestRule)
        assertServerStopped(composeTestRule)

        // Check QR code not shown after stopping the server
        assertQrNotShown(composeTestRule)

        // Select the test ZIM file to host on the server
        clickOnTestZim(composeTestRule)

        // Start the server with two ZIM files
        startServer(composeTestRule)
        assertServerStarted(composeTestRule)

        // Check that both ZIM files are hosted on the server
        assertItemHostedOnServer(2, composeTestRule)

        // Unselect the test ZIM to test restarting server functionality
        clickOnTestZim(composeTestRule)

        // Check if the server is running
        assertServerStarted(composeTestRule)

        // Check that only one ZIM file is hosted on the server after unselecting
        assertItemHostedOnServer(1, composeTestRule)

        // finally close the server at the end of test case
        stopServer(composeTestRule)
      }
    }
    LeakAssertions.assertNoLeaks()
  }

  private fun loadZimFileInApplication(zimFileName: String) {
    val loadFileStream =
      ZimHostFragmentTest::class.java.classLoader.getResourceAsStream(zimFileName)
    val zimFile = File(sharedPreferenceUtil.defaultStorage(), zimFileName)
    if (zimFile.exists()) zimFile.delete()
    zimFile.createNewFile()
    loadFileStream.use { inputStream ->
      val outputStream: OutputStream = FileOutputStream(zimFile)
      outputStream.use { it ->
        val buffer = ByteArray(inputStream.available())
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
          it.write(buffer, 0, length)
        }
      }
    }
  }

  @After
  fun finish() {
    context?.let(TestUtils::deleteTemporaryFilesOfTestCases)
  }
}
