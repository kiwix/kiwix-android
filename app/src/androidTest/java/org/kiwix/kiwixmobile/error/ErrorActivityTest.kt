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

import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.anyOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.ui.KiwixDestination
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

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
    val kiwixDataStore = KiwixDataStore(
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    ).apply {
      lifeCycleScope.launch {
        setWifiOnly(false)
        setIntroShown()
        setPrefLanguage("en")
        setLastDonationPopupShownInMilliSeconds(System.currentTimeMillis())
        setIsScanFileSystemDialogShown(true)
        setIsFirstRun(false)
      }
    }
    PreferenceManager.getDefaultSharedPreferences(context)
      .edit {
        putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      }
    activityScenario =
      ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          runBlocking {
            handleLocaleChange(
              it,
              "en",
              kiwixDataStore
            )
          }
        }
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
  fun verifyErrorActivity() {
    activityScenario.onActivity {
      it.navigate(KiwixDestination.Library.route)
    }
    library {
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
      deleteZimIfExists(composeTestRule)
    }
    loadZimFileInReader("testzim.zim")
    library {
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
      assertLibraryListDisplayed(composeTestRule)
    }
    activityScenario.onActivity {
      it.navigate(KiwixDestination.Help.route)
    }
    errorActivity {
      assertSendDiagnosticReportDisplayed(composeTestRule)
      clickOnSendDiagnosticReport(composeTestRule)
      assertErrorActivityDisplayed(composeTestRule)
      // Click on "No, Thanks" button to see it's functionality working or not.
      clickOnNoThanksButton(composeTestRule)
      // Handle the app restart explicitly. Since test case does not handle the app restart.
      activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).onActivity {
        it.navigate(KiwixDestination.Help.route)
      }
      // Assert HelpFragment is visible or not after clicking on the "No, Thanks" button.
      assertSendDiagnosticReportDisplayed(composeTestRule)
      // Again click on "Send diagnostic report" button to open the ErrorActivity.
      clickOnSendDiagnosticReport(composeTestRule)
      assertErrorActivityDisplayed(composeTestRule)
      // Check diagnostic details are displayed or not.
      assertDetailsIncludedInErrorReportDisplayed(composeTestRule)
      // Click on "Send details" button.
      clickOnSendDetailsButton(composeTestRule)
      // Assert ZIM file validation dialog displayed.
      assertZimFileValidationDialogDisplayed(composeTestRule)
    }
    composeTestRule.onRoot().tryPerformAccessibilityChecks()
  }

  private fun loadZimFileInReader(zimFileName: String) {
    val loadFileStream =
      ErrorActivityTest::class.java.classLoader.getResourceAsStream(zimFileName)
    val zimFile = File(context.getExternalFilesDirs(null)[0], zimFileName)
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
}
