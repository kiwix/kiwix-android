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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import leakcanary.LeakAssertions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.getZimFileFromResourceFolder
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.ui.KiwixDestination

class LocalLibraryTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @Rule(order = COMPOSE_TEST_RULE_ORDER)
  @JvmField
  val composeTestRule = createAndroidComposeRule<KiwixMainActivity>()

  @Before
  override fun waitForIdle() {
    super.waitForIdle()
    updateKiwixDataStore {
      // set `setShowManageExternalFilesPermissionDialog` false for hiding
      // manage external storage permission dialog on android 11 and above
      setShowManageExternalFilesPermissionDialog(false)
      // Set setManageExternalFilesPermissionDialogOnRefresh to false to hide
      // the manage external storage permission dialog on Android 11 and above
      // while refreshing the content in LocalLibraryScreen.
      setManageExternalFilesPermissionDialogOnRefresh(false)
    }
    launchMainActivity()
    composeTestRule.enableAccessibilityChecks(createAccessibilityValidator())
  }

  private fun observeLocalLibraryActions() {
    composeTestRule.runOnIdle {
      val kiwixMainActivity = composeTestRule.activity
      val localLibraryViewModel = ViewModelProvider(
        kiwixMainActivity,
        kiwixMainActivity.viewModelFactory
      )[LocalLibraryViewModel::class.java]

      val validateZimViewModel = ViewModelProvider(
        kiwixMainActivity,
        kiwixMainActivity.viewModelFactory
      )[ValidateZimViewModel::class.java]
      val storageDeviceList = runBlocking { kiwixMainActivity.getStorageDeviceList() }
      localLibraryViewModel.initialize(
        storageDeviceList = storageDeviceList,
        validateZimViewModel = validateZimViewModel,
        kiwixMainActivity.alertDialogShower,
        kiwixMainActivity.snackBarHostState
      )
      kiwixMainActivity.lifecycleScope.launch {
        localLibraryViewModel.sideEffects.collect { effect ->
          effect.invokeWith(kiwixMainActivity)
        }
      }
    }
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
    getZimFileFromResourceFolder(context, "testzim.zim")
    library {
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
      assertLibraryListDisplayed(composeTestRule)
      validateZIMFiles(composeTestRule)
    }
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.TIRAMISU) {
      // Temporary disabling on Android 13
      LeakAssertions.assertNoLeaks()
    }
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
      clickOnLocalLibraryScreen(composeTestRule)
      observeLocalLibraryActions()
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
      clickOnLocalLibraryScreen(composeTestRule)
      observeLocalLibraryActions()
      assertScanDialogNotDisplayed(composeTestRule)
      // Assert When there are ZIM files in local library screen then this dialog does not display.
      // Set to not show the "All files permission" dialog.
      showScanFileSystemDialog(
        scanFileSystemDialogShown = false,
        false,
        showManagePermissionDialog = false,
        isPlayStoreBuild = false
      )
      getZimFileFromResourceFolder(context, "testzim.zim")
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
      clickOnReaderFragment(composeTestRule)
      showScanFileSystemDialog(
        scanFileSystemDialogShown = false,
        false,
        showManagePermissionDialog = true,
        isPlayStoreBuild = false
      )
      clickOnLocalLibraryScreen(composeTestRule)
      observeLocalLibraryActions()
      assertScanDialogNotDisplayed(composeTestRule)
    }
  }

  private fun showScanFileSystemDialog(
    scanFileSystemDialogShown: Boolean,
    isTest: Boolean,
    showManagePermissionDialog: Boolean,
    isPlayStoreBuild: Boolean
  ) {
    updateKiwixDataStore {
      setIsScanFileSystemDialogShown(scanFileSystemDialogShown)
      setIsScanFileSystemTest(true)
      setManageExternalFilesPermissionDialogOnRefresh(showManagePermissionDialog)
      setIsPlayStoreBuild(isPlayStoreBuild)
      setPrefIsTest(isTest)
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
