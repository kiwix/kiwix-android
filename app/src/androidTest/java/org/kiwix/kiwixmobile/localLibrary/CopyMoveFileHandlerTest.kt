/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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
import android.os.Build
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.extensions.deleteFile
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.CopyMoveFileHandler
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryFragment
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.FileWritingFileSystemChecker
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class CopyMoveFileHandlerTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createAndroidComposeRule<KiwixMainActivity>()

  private lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  private lateinit var kiwixMainActivity: KiwixMainActivity
  private lateinit var selectedFile: File
  private lateinit var destinationFile: File
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
      putBoolean(SharedPreferenceUtil.IS_PLAY_STORE_BUILD, true)
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
  }

  @Test
  fun testCopyingZimFileIntoPublicStorage() {
    deleteAllFilesInDirectory(parentFile)
    // Test the scenario in playStore build on Android 11 and above.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      selectedFile = getSelectedFile()
      composeTestRule.apply {
        runOnUiThread {
          kiwixMainActivity.navigate(KiwixDestination.Library.route)
        }
        waitForIdle()
        waitUntilTimeout() // to properly load the library screen.
      }
      // test with first launch
      sharedPreferenceUtil.shouldShowStorageSelectionDialog = true
      showMoveFileToPublicDirectoryDialog(listOf(Uri.fromFile(selectedFile)))
      // should show the permission dialog.
      copyMoveFileHandler {
        assertCopyMoveDialogDisplayed(composeTestRule)
        clickOnCopy(composeTestRule)
        assertStorageSelectionDialogDisplayed(composeTestRule)
        clickOnInternalStorage(composeTestRule)
        assertZimFileCopiedAndShowingIntoTheReader(composeTestRule)
      }
      assertZimFileAddedInTheLocalLibrary()

      // Test with second launch, this time permission dialog should not show.
      // delete the parent directory so that all the previous file will be deleted.
      deleteAllFilesInDirectory(parentFile)
      library { refreshList(composeTestRule) }
      showMoveFileToPublicDirectoryDialog(listOf(Uri.fromFile(selectedFile)))
      // should show the copyMove dialog.
      copyMoveFileHandler {
        assertCopyMoveDialogDisplayed(composeTestRule)
        clickOnCopy(composeTestRule)
        assertZimFileCopiedAndShowingIntoTheReader(composeTestRule)
      }
      assertZimFileAddedInTheLocalLibrary()
      deleteAllFilesInDirectory(parentFile)
      TestUtils.deleteTemporaryFilesOfTestCases(context)

      // Test multiple files copying.
      navigateToLocalLibraryFragment()
      deleteZimFilesIfExistInLocalLibrary()
      val invalidZimFile = getInvalidZimFileUri(".mp4")
      selectedFile = getSelectedFile()
      showMoveFileToPublicDirectoryDialog(mutableListOf(invalidZimFile, Uri.fromFile(selectedFile)))
      copyMoveFileHandler {
        // assert first ZIM file is invalid file so it should show the continue
        // with other ZIM files dialog.
        assertFileCopyMoveErrorDialogDisplayed(composeTestRule)
        clickOnYesButton(composeTestRule)
        assertCopyMoveDialogDisplayed(composeTestRule)
        clickOnCopy(composeTestRule)
        assertZimFileAddedInTheLocalLibrary(composeTestRule)
      }
    }
  }

  @Test
  fun testMovingZimFileIntoPublicDirectory() {
    deleteAllFilesInDirectory(parentFile)
    // Test the scenario in playStore build on Android 11 and above.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      selectedFile = getSelectedFile()
      composeTestRule.apply {
        runOnUiThread {
          kiwixMainActivity.navigate(KiwixDestination.Library.route)
        }
        waitForIdle()
        waitUntilTimeout() // to properly load the library screen.
      }
      // test with first launch
      sharedPreferenceUtil.shouldShowStorageSelectionDialog = true
      showMoveFileToPublicDirectoryDialog(listOf(Uri.fromFile(selectedFile)))
      // should show the permission dialog.
      copyMoveFileHandler {
        assertCopyMoveDialogDisplayed(composeTestRule)
        clickOnMove(composeTestRule)
        assertStorageSelectionDialogDisplayed(composeTestRule)
        clickOnInternalStorage(composeTestRule)
        assertZimFileCopiedAndShowingIntoTheReader(composeTestRule)
      }
      assertZimFileAddedInTheLocalLibrary()
      // Test with second launch, this time permission dialog should not show.
      // delete the parent directory so that all the previous file will be deleted.
      deleteAllFilesInDirectory(parentFile)
      selectedFile = getSelectedFile()
      showMoveFileToPublicDirectoryDialog(listOf(Uri.fromFile(selectedFile)))
      // should show the copyMove dialog.
      copyMoveFileHandler {
        assertCopyMoveDialogDisplayed(composeTestRule)
        clickOnMove(composeTestRule)
        assertZimFileCopiedAndShowingIntoTheReader(composeTestRule)
      }
      assertZimFileAddedInTheLocalLibrary()
      deleteAllFilesInDirectory(parentFile)
      TestUtils.deleteTemporaryFilesOfTestCases(context)

      // Test multiple files copying.
      navigateToLocalLibraryFragment()
      deleteZimFilesIfExistInLocalLibrary()
      val invalidZimFile = getInvalidZimFileUri(".mp4")
      selectedFile = getSelectedFile()
      showMoveFileToPublicDirectoryDialog(mutableListOf(invalidZimFile, Uri.fromFile(selectedFile)))
      copyMoveFileHandler {
        // assert first ZIM file is invalid file so it should show the continue
        // with other ZIM files dialog.
        assertFileCopyMoveErrorDialogDisplayed(composeTestRule)
        clickOnYesButton(composeTestRule)
        assertCopyMoveDialogDisplayed(composeTestRule)
        clickOnMove(composeTestRule)
        assertZimFileAddedInTheLocalLibrary(composeTestRule)
      }
    }
  }

  private fun assertZimFileAddedInTheLocalLibrary() {
    navigateToLocalLibraryFragment()
    copyMoveFileHandler { assertZimFileAddedInTheLocalLibrary(composeTestRule) }
  }

  private fun navigateToLocalLibraryFragment() {
    UiThreadStatement.runOnUiThread {
      kiwixMainActivity.navigate(KiwixDestination.Library.route)
    }
  }

  private fun deleteZimFilesIfExistInLocalLibrary() {
    library {
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
      deleteZimIfExists(composeTestRule)
    }
  }

  private fun showMoveFileToPublicDirectoryDialog(urisList: List<Uri>) {
    kiwixMainActivity.lifecycleScope.launch {
      val localLibraryFragment =
        kiwixMainActivity.supportFragmentManager.fragments
          .filterIsInstance<LocalLibraryFragment>()
          .firstOrNull()
      localLibraryFragment?.handleSelectedFileUri(urisList)
    }
  }

  private fun getSelectedFile(): File {
    val loadFileStream =
      CopyMoveFileHandlerTest::class.java.classLoader.getResourceAsStream("testzim.zim")
    val zimFile = File(context.getExternalFilesDirs(null)[0], "testzim.zim")
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

  @Test
  fun testGetDestinationFile() {
    composeTestRule.apply {
      runOnUiThread {
        kiwixMainActivity.navigate(KiwixDestination.Library.route)
      }
      waitForIdle()
      waitUntilTimeout() // to properly load the library screen.
    }
    val selectedFileName = "testCopyMove.zim"
    deleteAllFilesInDirectory(parentFile)
    val copyMoveFileHandler = CopyMoveFileHandler(
      kiwixMainActivity,
      sharedPreferenceUtil,
      StorageCalculator(sharedPreferenceUtil),
      Fat32Checker(sharedPreferenceUtil, listOf(FileWritingFileSystemChecker()))
    ).apply {
      setAlertDialogShower(AlertDialogShower())
    }
    runBlocking {
      // test fileName when there is already a file available with same name.
      // it should return different name
      selectedFile = File(parentFile, selectedFileName).apply {
        if (!isFileExist()) createNewFile()
      }
      copyMoveFileHandler.setSelectedFileAndUri(null, DocumentFile.fromFile(selectedFile))
      destinationFile = copyMoveFileHandler.getDestinationFile()
      Assert.assertNotEquals(
        destinationFile.name,
        selectedFile.name
      )
      Assert.assertEquals(
        destinationFile.name,
        "testCopyMove_1.zim"
      )
      withContext(Dispatchers.IO) {
        deleteBothPreviousFiles()
      }

      // test when there is no zim file available in the storage it should return the same fileName
      selectedFile = File(parentFile, selectedFileName)
      copyMoveFileHandler.setSelectedFileAndUri(null, DocumentFile.fromFile(selectedFile))
      destinationFile = copyMoveFileHandler.getDestinationFile()
      Assert.assertEquals(
        destinationFile.name,
        selectedFile.name
      )
      deleteBothPreviousFiles()
    }
  }

  @Test
  fun testInvalidFileShouldNotOpenInReader() {
    deleteAllFilesInDirectory(parentFile)
    // Test the scenario in playStore build on Android 11 and above.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      selectedFile = getSelectedFile()
      composeTestRule.apply {
        runOnUiThread {
          kiwixMainActivity.navigate(KiwixDestination.Library.route)
        }
        waitForIdle()
        waitUntilTimeout() // to properly load the library screen.
      }
      sharedPreferenceUtil.apply {
        shouldShowStorageSelectionDialog = false
        setIsPlayStoreBuildType(true)
      }
      // test opening images
      showMoveFileToPublicDirectoryDialog(listOf(getInvalidZimFileUri(".jpg")))
      copyMoveFileHandler { assertCopyMoveDialogNotDisplayed(composeTestRule) }
      // test opening videos
      showMoveFileToPublicDirectoryDialog(listOf(getInvalidZimFileUri(".mp4")))
      copyMoveFileHandler { assertCopyMoveDialogNotDisplayed(composeTestRule) }
      // test opening pdf
      showMoveFileToPublicDirectoryDialog(listOf(getInvalidZimFileUri(".pdf")))
      copyMoveFileHandler { assertCopyMoveDialogNotDisplayed(composeTestRule) }
    }
  }

  private suspend fun deleteBothPreviousFiles() {
    selectedFile.deleteFile()
    destinationFile.deleteFile()
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

  @After
  fun finish() {
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }
}
