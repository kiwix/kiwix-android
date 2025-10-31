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

package org.kiwix.kiwixmobile.localLibrary

import android.net.Uri
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import kotlinx.coroutines.launch
import org.hamcrest.Matchers.anyOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryFragment
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.ui.KiwixDestination
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class ProcessSelectedZimFilesForStandaloneTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createAndroidComposeRule<KiwixMainActivity>()

  private lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  private lateinit var kiwixMainActivity: KiwixMainActivity
  private lateinit var parentFile: File

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.IS_PLAY_STORE_BUILD, false)
      putString(SharedPreferenceUtil.PREF_LANG, "en")
      putLong(
        SharedPreferenceUtil.PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS,
        System.currentTimeMillis()
      )
    }
    composeTestRule.apply {
      kiwixMainActivity = activity
      runOnUiThread {
        sharedPreferenceUtil = SharedPreferenceUtil(kiwixMainActivity)
        LanguageUtils.handleLocaleChange(
          kiwixMainActivity,
          "en",
          sharedPreferenceUtil
        )
        parentFile = File(sharedPreferenceUtil.prefStorage)
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
  fun testZimFileSelectionForStandaloneApp() {
    // Test single ZIM file.
    prepareLocalLibraryForTest()
    var validZimFile1 = getValidZimFile("testzim.zim")
    triggerProcessSelectedZimFiles(listOf(Uri.fromFile(validZimFile1)))
    copyMoveFileHandler { assertZimFileCopiedAndShowingIntoTheReader(composeTestRule) }

    // Test multiple ZIM valid ZIM files.
    prepareLocalLibraryForTest()
    validZimFile1 = getValidZimFile("testzim.zim")
    val validZimFile2 = getValidZimFile("small.zim")
    var listOfZimFilesUri = listOf(Uri.fromFile(validZimFile1), Uri.fromFile(validZimFile2))
    triggerProcessSelectedZimFiles(listOfZimFilesUri)
    copyMoveFileHandler {
      composeTestRule.apply {
        // Wait for some time so that ZIM files can properly loaded in library.
        waitForIdle()
        waitUntilTimeout()
        assertZimFileAddedInTheLocalLibrary(composeTestRule)
      }
    }

    // Test multiple ZIM files with invalid ZIM file.
    prepareLocalLibraryForTest()
    validZimFile1 = getValidZimFile("testzim.zim")
    val invalidZimUri = getInvalidZimFileUri(".jpg")
    listOfZimFilesUri = listOf(invalidZimUri, Uri.fromFile(validZimFile1))
    triggerProcessSelectedZimFiles(listOfZimFilesUri)
    copyMoveFileHandler {
      // Test for invalid ZIM file it shows the error dialog.
      assertFileCopyMoveErrorDialogDisplayed(composeTestRule)
      clickOnYesButton(composeTestRule)
      composeTestRule.waitUntilTimeout()
      assertZimFileAddedInTheLocalLibrary(composeTestRule)
    }
  }

  private fun prepareLocalLibraryForTest() {
    deleteAllFilesInDirectory(parentFile)
    TestUtils.deleteTemporaryFilesOfTestCases(context)
    navigateToLocalLibraryFragment()
    deleteZimFilesIfExistInLocalLibrary()
  }

  private fun triggerProcessSelectedZimFiles(urisList: List<Uri>) {
    kiwixMainActivity.lifecycleScope.launch {
      val localLibraryFragment =
        kiwixMainActivity.supportFragmentManager.fragments
          .filterIsInstance<LocalLibraryFragment>()
          .firstOrNull()
      localLibraryFragment?.handleSelectedFileUri(urisList)
    }
  }

  private fun navigateToLocalLibraryFragment() {
    composeTestRule.apply {
      runOnUiThread {
        kiwixMainActivity.navigate(KiwixDestination.Library.route)
      }
      waitForIdle()
      waitUntilTimeout() // to properly load the library screen.
    }
  }

  private fun deleteZimFilesIfExistInLocalLibrary() {
    library {
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
      deleteZimIfExists(composeTestRule)
    }
  }

  private fun deleteAllFilesInDirectory(directory: File) {
    if (directory.isDirectory) {
      directory.listFiles()?.forEach { file ->
        if (file.isDirectory) {
          // Recursively delete files in subdirectories
          deleteAllFilesInDirectory(file)
        }
        file.delete()
      }
    }
  }

  private fun getValidZimFile(zimFileName: String): File {
    val loadFileStream =
      CopyMoveFileHandlerTest::class.java.classLoader.getResourceAsStream(zimFileName)
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
    return zimFile
  }

  private fun getInvalidZimFileUri(extension: String): Uri {
    val zimFile = File(context.getExternalFilesDirs(null)[0], "testzim$extension")
    if (zimFile.exists()) zimFile.delete()
    zimFile.createNewFile()
    return Uri.fromFile(zimFile)
  }

  @After
  fun finish() {
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }
}
