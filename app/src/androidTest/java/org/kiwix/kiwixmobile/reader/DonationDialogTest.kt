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

package org.kiwix.kiwixmobile.reader

import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.anyOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.THREE_MONTHS_IN_MILLISECONDS
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.ui.KiwixDestination
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class DonationDialogTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()

  private lateinit var kiwixMainActivity: KiwixMainActivity
  private lateinit var sharedPreferenceUtil: SharedPreferenceUtil

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    val kiwixDataStore = KiwixDataStore(context).apply {
      lifeCycleScope.launch {
        setWifiOnly(false)
        setIntroShown()
        setPrefLanguage("en")
      }
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.PREF_SCAN_FILE_SYSTEM_DIALOG_SHOWN, true)
      putBoolean(SharedPreferenceUtil.PREF_IS_FIRST_RUN, false)
    }
    sharedPreferenceUtil = SharedPreferenceUtil(context)
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
  fun showDonationPopupWhenApplicationIsThreeMonthOldAndHaveAtleastOneZIMFile() {
    loadZIMFileInApplication()
    sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds = 0L
    sharedPreferenceUtil.laterClickedMilliSeconds = 0L
    openReaderFragment()
    donation { assertDonationDialogDisplayed(composeTestRule) }
  }

  @Test
  fun shouldNotShowDonationPopupWhenApplicationIsThreeMonthOldAndDoNotHaveAnyZIMFile() {
    sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds = 0L
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(KiwixDestination.Library.route)
    }
    deleteAllZIMFilesFromApplication()
    openReaderFragment()
    donation { assertDonationDialogIsNotDisplayed(composeTestRule) }
  }

  @Test
  fun shouldNotShowPopupIfTimeSinceLastPopupIsLessThanThreeMonth() {
    sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds =
      System.currentTimeMillis() - (THREE_MONTHS_IN_MILLISECONDS / 2)
    loadZIMFileInApplication()
    openReaderFragment()
    donation { assertDonationDialogIsNotDisplayed(composeTestRule) }
  }

  @Test
  fun shouldShowDonationPopupIfTimeSinceLastPopupExceedsThreeMonths() {
    sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds =
      System.currentTimeMillis() - (THREE_MONTHS_IN_MILLISECONDS + 1000)
    loadZIMFileInApplication()
    openReaderFragment()
    donation { assertDonationDialogDisplayed(composeTestRule) }
  }

  @Test
  fun testShouldShowDonationPopupWhenLaterClickedTimeExceedsThreeMonths() {
    sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds = 0L
    sharedPreferenceUtil.laterClickedMilliSeconds =
      System.currentTimeMillis() - (THREE_MONTHS_IN_MILLISECONDS + 1000)
    loadZIMFileInApplication()
    openReaderFragment()
    donation { assertDonationDialogDisplayed(composeTestRule) }
  }

  @Test
  fun testShouldNotShowPopupIfLaterClickedTimeIsLessThanThreeMonths() {
    sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds = 0L
    sharedPreferenceUtil.laterClickedMilliSeconds =
      System.currentTimeMillis() - 10000L
    loadZIMFileInApplication()
    openReaderFragment()
    donation { assertDonationDialogIsNotDisplayed(composeTestRule) }
  }

  private fun openReaderFragment() {
    UiThreadStatement.runOnUiThread {
      kiwixMainActivity.navigate(kiwixMainActivity.readerFragmentRoute)
    }
  }

  private fun loadZIMFileInApplication() {
    openLocalLibraryScreen()
    deleteAllZIMFilesFromApplication()
    val loadFileStream =
      DonationDialogTest::class.java.classLoader.getResourceAsStream("testzim.zim")
    val zimFile =
      File(
        context.getExternalFilesDirs(null)[0],
        "testzim.zim"
      )
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
    refreshZIMFilesList()
  }

  private fun openLocalLibraryScreen() {
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(KiwixDestination.Library.route)
    }
  }

  private fun refreshZIMFilesList() {
    library {
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
    }
  }

  private fun deleteAllZIMFilesFromApplication() {
    refreshZIMFilesList()
    library {
      // delete all the ZIM files showing in the LocalLibrary
      // screen to properly test the scenario.
      deleteZimIfExists(composeTestRule)
    }
  }

  @After
  fun finish() {
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }
}
