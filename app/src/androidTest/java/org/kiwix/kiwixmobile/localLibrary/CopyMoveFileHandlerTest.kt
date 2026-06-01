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
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.os.LocaleListCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.extensions.deleteFile
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.CopyMoveFileHandler
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.nav.destination.library.local.CopyMoveProgressBarControllerImpl
import org.kiwix.kiwixmobile.nav.destination.library.local.FileOperationHandlerImpl
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.getZimFileFromResourceFolder
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.FileWritingFileSystemChecker
import org.kiwix.sharedFunctions.MainDispatcherRule
import java.io.File

class CopyMoveFileHandlerTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createAndroidComposeRule<KiwixMainActivity>()

  @get:Rule
  private val dispatcher = MainDispatcherRule()
  private lateinit var kiwixMainActivity: KiwixMainActivity
  private lateinit var selectedFile: File
  private lateinit var destinationFile: File
  private lateinit var parentFile: File

  @Before
  override fun waitForIdle() {
    super.waitForIdle()
    composeTestRule.apply {
      runOnUiThread {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        parentFile = runBlocking { File(kiwixDataStore.selectedStorage.first()) }
      }
      kiwixMainActivity = activity
      waitForIdle()
    }
    composeTestRule.enableAccessibilityChecks(createAccessibilityValidator())
  }

  @Test
  fun testCopyingZimFileIntoPublicStorage() {
    deleteAllFilesInDirectory(parentFile)
    // Test the scenario in playStore build on Android 11 and above.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      selectedFile = getZimFileFromResourceFolder(context, "testzim.zim")
      composeTestRule.apply {
        waitForIdle()
        runOnUiThread {
          kiwixMainActivity.navigate(KiwixDestination.Library.route)
        }
        waitForIdle()
        waitUntilTimeout() // to properly load the library screen.
      }
      // test with first launch
      updateKiwixDataStore { setShowStorageSelectionDialogOnCopyMove(true) }
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
      navigateToLocalLibraryScreen()
      deleteZimFilesIfExistInLocalLibrary()
      val invalidZimFile = getInvalidZimFileUri(".mp4")
      selectedFile = getZimFileFromResourceFolder(context, "testzim.zim")
      val secondValidZimFile = getZimFileFromResourceFolder(context, "small.zim")
      showMoveFileToPublicDirectoryDialog(
        mutableListOf(
          invalidZimFile,
          Uri.fromFile(selectedFile),
          Uri.fromFile(secondValidZimFile)
        )
      )
      copyMoveFileHandler {
        // assert first ZIM file is invalid file so it should show the continue
        // with other ZIM files dialog.
        assertFileCopyMoveErrorDialogDisplayed(composeTestRule)
        clickOnYesButton(composeTestRule)
        assertCopyMoveDialogDisplayed(composeTestRule, true)
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
      selectedFile = getZimFileFromResourceFolder(context, "testzim.zim")
      composeTestRule.apply {
        waitForIdle()
        runOnUiThread {
          kiwixMainActivity.navigate(KiwixDestination.Library.route)
        }
        waitForIdle()
        waitUntilTimeout() // to properly load the library screen.
      }
      // test with first launch
      updateKiwixDataStore { setShowStorageSelectionDialogOnCopyMove(true) }
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
      library { refreshList(composeTestRule) }
      selectedFile = getZimFileFromResourceFolder(context, "testzim.zim")
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
      navigateToLocalLibraryScreen()
      deleteZimFilesIfExistInLocalLibrary()
      val invalidZimFile = getInvalidZimFileUri(".mp4")
      selectedFile = getZimFileFromResourceFolder(context, "testzim.zim")
      val secondValidZimFile = getZimFileFromResourceFolder(context, "small.zim")
      showMoveFileToPublicDirectoryDialog(
        mutableListOf(
          invalidZimFile,
          Uri.fromFile(selectedFile),
          Uri.fromFile(secondValidZimFile)
        )
      )
      copyMoveFileHandler {
        // assert first ZIM file is invalid file so it should show the continue
        // with other ZIM files dialog.
        assertFileCopyMoveErrorDialogDisplayed(composeTestRule)
        clickOnYesButton(composeTestRule)
        assertCopyMoveDialogDisplayed(composeTestRule, true)
        clickOnMove(composeTestRule)
        assertZimFileAddedInTheLocalLibrary(composeTestRule)
      }
    }
  }

  private fun assertZimFileAddedInTheLocalLibrary() {
    navigateToLocalLibraryScreen()
    copyMoveFileHandler { assertZimFileAddedInTheLocalLibrary(composeTestRule) }
  }

  private fun navigateToLocalLibraryScreen() {
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
    composeTestRule.runOnIdle {
      kiwixMainActivity = composeTestRule.activity

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
        kiwixMainActivity.snackBarHostState,
        kiwixMainActivity.supportFragmentManager
      )
      kiwixMainActivity.lifecycleScope.launch {
        localLibraryViewModel.handleSelectedFileUri(urisList)
        localLibraryViewModel.sideEffects.collect { effect ->
          effect.invokeWith(kiwixMainActivity)
        }
      }
    }
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
      kiwixDataStore,
      StorageCalculator(kiwixDataStore),
      Fat32Checker(kiwixDataStore, listOf(FileWritingFileSystemChecker()), dispatcher.dispatcher),
      FileOperationHandlerImpl(kiwixMainActivity, dispatcher.dispatcher),
      CopyMoveProgressBarControllerImpl(kiwixMainActivity),
      dispatcher.dispatcher
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
      deleteBothPreviousFiles()

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
      selectedFile = getZimFileFromResourceFolder(context, "testzim.zim")
      composeTestRule.apply {
        runOnUiThread {
          kiwixMainActivity.navigate(KiwixDestination.Library.route)
        }
        waitForIdle()
        waitUntilTimeout() // to properly load the library screen.
      }
      updateKiwixDataStore {
        setShowStorageSelectionDialogOnCopyMove(false)
        setIsPlayStoreBuild(true)
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
