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

package org.kiwix.kiwixmobile.nav.destination.library

import android.os.Build
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
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
import leakcanary.LeakAssertions
import org.hamcrest.Matchers.anyOf
import org.junit.After
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
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.ui.KiwixDestination
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class LocalLibraryTest : BaseActivityTest() {
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
    KiwixDataStore(
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    ).apply {
      lifeCycleScope.launch {
        setWifiOnly(false)
        setIntroShown()
      }
    }
    PreferenceManager.getDefaultSharedPreferences(
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    ).edit {
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      // set PREF_MANAGE_EXTERNAL_FILES false for hiding
      // manage external storage permission dialog on android 11 and above
      putBoolean(SharedPreferenceUtil.PREF_MANAGE_EXTERNAL_FILES, false)
      // Set PREF_SHOW_MANAGE_PERMISSION_DIALOG_ON_REFRESH to false to hide
      // the manage external storage permission dialog on Android 11 and above
      // while refreshing the content in LocalLibraryFragment.
      putBoolean(SharedPreferenceUtil.PREF_SHOW_MANAGE_PERMISSION_DIALOG_ON_REFRESH, false)
      putBoolean(SharedPreferenceUtil.PREF_SCAN_FILE_SYSTEM_DIALOG_SHOWN, true)
      putBoolean(SharedPreferenceUtil.PREF_IS_FIRST_RUN, false)
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
            SharedPreferenceUtil(context)
          )
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
  fun testLocalLibrary() {
    activityScenario.onActivity {
      it.navigate(KiwixDestination.Library.route)
    }
    library {
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
      deleteZimIfExists(composeTestRule)
    }
    // load a zim file to test, After downloading zim file library list is visible or not
    loadZimFileInReader("testzim.zim")
    library {
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
      assertLibraryListDisplayed(composeTestRule)
      validateZIMFiles(composeTestRule)
    }
    LeakAssertions.assertNoLeaks()
  }

  @Test
  fun testScanStorageDialog() {
    // Delete all the files before opening the library screen.
    TestUtils.deleteTemporaryFilesOfTestCases(context)
    activityScenario.onActivity {
      it.navigate(KiwixDestination.Library.route)
    }
    library {
      // Delete any ZIM file if available.
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
      deleteZimIfExists(composeTestRule)
      assertShowSwipeDownToScanFileSystemTextDisplayed(composeTestRule)
      showScanFileSystemDialog(
        scanFileSystemDialogShown = false,
        false,
        showManagePermissionDialog = true,
        isPlayStoreBuild = false
      )
      clickOnReaderFragment(composeTestRule)
      clickOnLocalLibraryFragment(composeTestRule)
      composeTestRule.waitUntilTimeout()
      // Assert scan dialog visible.
      assertScanFileSystemDialogDisplayed(composeTestRule)
      clickOnDialogConfirmButton(composeTestRule)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        assertManageExternalPermissionDialogDisplayed(composeTestRule)
        clickOnDialogDismissButton(composeTestRule)
      }
      // Assert scan dialog does not show again.
      clickOnReaderFragment(composeTestRule)
      clickOnLocalLibraryFragment(composeTestRule)
      assertScanDialogNotDisplayed(composeTestRule)
      // Assert When there are ZIM files in local library screen then this dialog does not display.
      // Set to not show the "All files permission" dialog.
      showScanFileSystemDialog(
        scanFileSystemDialogShown = false,
        false,
        showManagePermissionDialog = false,
        isPlayStoreBuild = false
      )
      loadZimFileInReader("testzim.zim")
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
      clickOnReaderFragment(composeTestRule)
      showScanFileSystemDialog(
        scanFileSystemDialogShown = false,
        false,
        showManagePermissionDialog = true,
        isPlayStoreBuild = false
      )
      clickOnLocalLibraryFragment(composeTestRule)
      assertScanDialogNotDisplayed(composeTestRule)
    }
  }

  private fun loadZimFileInReader(zimFileName: String) {
    val loadFileStream =
      LocalLibraryTest::class.java.classLoader.getResourceAsStream(zimFileName)
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

  private fun showScanFileSystemDialog(
    scanFileSystemDialogShown: Boolean,
    isTest: Boolean,
    showManagePermissionDialog: Boolean,
    isPlayStoreBuild: Boolean
  ) {
    PreferenceManager.getDefaultSharedPreferences(
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    ).edit {
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, isTest)
      putBoolean(SharedPreferenceUtil.PREF_SCAN_FILE_SYSTEM_DIALOG_SHOWN, scanFileSystemDialogShown)
      putBoolean(
        SharedPreferenceUtil.PREF_SHOW_MANAGE_PERMISSION_DIALOG_ON_REFRESH,
        showManagePermissionDialog
      )
      putBoolean(
        SharedPreferenceUtil.PREF_IS_SCAN_FILE_SYSTEM_TEST,
        true
      )
      putBoolean(SharedPreferenceUtil.IS_PLAY_STORE_BUILD, isPlayStoreBuild)
    }
  }

  @After
  fun finish() {
    showScanFileSystemDialog(
      scanFileSystemDialogShown = true,
      true,
      showManagePermissionDialog = false,
      isPlayStoreBuild = false
    )
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }
}
