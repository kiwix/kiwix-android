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

import android.app.Activity
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentManager
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.nav.destination.library.CopyMoveFileHandler
import org.kiwix.kiwixmobile.nav.destination.library.CopyMoveFileHandler.FileCopyMoveCallback
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.DetectingFileSystem
import java.io.File

class CopyMoveFileHandlerTest {
  private lateinit var fileHandler: CopyMoveFileHandler

  private val activity: Activity = mockk(relaxed = true)
  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk(relaxed = true)
  private val alertDialogShower: AlertDialogShower = mockk(relaxed = true)
  private val storageCalculator: StorageCalculator = mockk(relaxed = true)
  private val fat32Checker: Fat32Checker = mockk(relaxed = true)
  private val fileCopyMoveCallback: FileCopyMoveCallback = mockk(relaxed = true)
  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)
  private val storageFile: File = mockk(relaxed = true)
  private val selectedFile: DocumentFile = mockk(relaxed = true)
  private val storagePath = "storage/0/emulated/Android/media/org.kiwix.kiwixmobile"
  private val destinationFile = mockk<File>()
  private val sourceUri = mockk<Uri>()
  private val fragmentManager = mockk<FragmentManager>()

  @BeforeEach
  fun setup() {
    clearAllMocks()
    fileHandler = CopyMoveFileHandler(
      activity,
      sharedPreferenceUtil,
      alertDialogShower,
      storageCalculator,
      fat32Checker
    ).apply {
      setSelectedFileAndUri(null, selectedFile)
      setLifeCycleScope(testScope)
      setFileCopyMoveCallback(this@CopyMoveFileHandlerTest.fileCopyMoveCallback)
    }
    every { destinationFile.canRead() } returns true
  }

  @Test
  fun validateZimFileCanCopyOrMoveShouldReturnTrueWhenSufficientSpaceAndValidFileSystem() {
    prepareFileSystemAndFileForMockk()

    val result = fileHandler.validateZimFileCanCopyOrMove(storageFile)

    assertTrue(result)
    // check insufficientSpaceInStorage callback should not call.
    verify(exactly = 0) { fileCopyMoveCallback.insufficientSpaceInStorage(any()) }
  }

  @Test
  fun validateZimFileCanCopyOrMoveShouldReturnFalseAndCallCallbackWhenInsufficientSpace() {
    prepareFileSystemAndFileForMockk(
      selectedFileLength = 2000L,
      fileSystemState = CanWrite4GbFile
    )
    val result = fileHandler.validateZimFileCanCopyOrMove(storageFile)

    assertFalse(result)
    verify { fileCopyMoveCallback.insufficientSpaceInStorage(any()) }
  }

  @Test
  fun validateZimFileCanCopyOrMoveShouldReturnFalseWhenDetectingAndCanNotWrite4GBFiles() {
    prepareFileSystemAndFileForMockk(fileSystemState = DetectingFileSystem)
    // check when detecting the fileSystem
    assertFalse(fileHandler.validateZimFileCanCopyOrMove(storageFile))

    prepareFileSystemAndFileForMockk(fileSystemState = CannotWrite4GbFile)

    // check when Can not write 4GB files on the fileSystem
    assertFalse(fileHandler.validateZimFileCanCopyOrMove())
  }

  @Test
  fun validateZimFileCanCopyOrMoveShouldReturnFalseWhenDetectingFileSystem() {
    every { fileHandler.isBookLessThan4GB() } returns true
    every { fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable() } just Runs
    prepareFileSystemAndFileForMockk(fileSystemState = DetectingFileSystem)

    val result = fileHandler.validateZimFileCanCopyOrMove(storageFile)

    assertFalse(result)
    verify { fileHandler.handleDetectingFileSystemState() }
  }

  @Test
  fun validateZimFileCanCopyOrMoveShouldReturnFalseWhenCannotWrite4GbFile() {
    every { fileHandler.isBookLessThan4GB() } returns true
    every { fileHandler.showCopyMoveDialog() } just Runs
    every {
      fileCopyMoveCallback.filesystemDoesNotSupportedCopyMoveFilesOver4GB()
    } just Runs
    prepareFileSystemAndFileForMockk(fileSystemState = CannotWrite4GbFile)

    val result = fileHandler.validateZimFileCanCopyOrMove(storageFile)

    assertFalse(result)
    verify { fileHandler.handleCannotWrite4GbFileState() }
  }

  @Test
  fun handleDetectingFileSystemStateShouldPerformCopyMoveOperationIfBookLessThan4GB() {
    fileHandler = spyk(fileHandler)
    prepareFileSystemAndFileForMockk()
    every { fileHandler.isBookLessThan4GB() } returns true
    every { fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable() } just Runs

    fileHandler.handleDetectingFileSystemState()

    verify { fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable() }
  }

  @Test
  fun handleDetectingFileSystemStateShouldObserveFileSystemStateIfBookGreaterThan4GB() {
    fileHandler = spyk(fileHandler)
    prepareFileSystemAndFileForMockk(fileSystemState = DetectingFileSystem)
    every { fileHandler.isBookLessThan4GB() } returns false
    every { fileHandler.observeFileSystemState() } just Runs

    fileHandler.handleDetectingFileSystemState()
    verify { fileHandler.observeFileSystemState() }
  }

  @Test
  fun handleCannotWrite4GbFileStateShouldPerformCopyMoveOperationIfBookLessThan4GB() {
    fileHandler = spyk(fileHandler)
    prepareFileSystemAndFileForMockk()
    every { fileHandler.isBookLessThan4GB() } returns true
    every { fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable() } just Runs

    fileHandler.handleCannotWrite4GbFileState()

    verify { fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable() }
  }

  @Test
  fun handleCannotWrite4GbFileStateShouldCallCallbackIfBookGreaterThan4GB() {
    fileHandler = spyk(fileHandler)
    prepareFileSystemAndFileForMockk()
    every { fileHandler.isBookLessThan4GB() } returns false
    every {
      fileCopyMoveCallback.filesystemDoesNotSupportedCopyMoveFilesOver4GB()
    } just Runs

    fileHandler.handleCannotWrite4GbFileState()

    verify {
      fileCopyMoveCallback.filesystemDoesNotSupportedCopyMoveFilesOver4GB()
    }
  }

  @Test
  fun showStorageConfigureDialogAtFirstLaunch() {
    fileHandler = spyk(fileHandler)
    every { fileHandler.showStorageSelectDialog() } just Runs
    every { sharedPreferenceUtil.shouldShowStorageSelectionDialog } returns true
    every { fileHandler.storageDeviceList } returns listOf(mockk(), mockk())
    val positiveButtonClickSlot = slot<() -> Unit>()
    every {
      alertDialogShower.show(
        KiwixDialog.CopyMoveFileToPublicDirectoryDialog,
        capture(positiveButtonClickSlot),
        any()
      )
    } just Runs
    fileHandler.showMoveFileToPublicDirectoryDialog(fragmentManager = fragmentManager)
    every { fileHandler.validateZimFileCanCopyOrMove() } returns true
    positiveButtonClickSlot.captured.invoke()
    verify { fileHandler.showStorageSelectDialog() }
  }

  @Test
  fun shouldNotShowStorageConfigureDialogWhenThereIsOnlyInternalAvailable() {
    fileHandler = spyk(fileHandler)
    every { sharedPreferenceUtil.shouldShowStorageSelectionDialog } returns true
    every { fileHandler.storageDeviceList } returns listOf(mockk())
    val positiveButtonClickSlot = slot<() -> Unit>()
    every {
      alertDialogShower.show(
        KiwixDialog.CopyMoveFileToPublicDirectoryDialog,
        capture(positiveButtonClickSlot),
        any()
      )
    } just Runs
    every { fileHandler.validateZimFileCanCopyOrMove() } returns true
    fileHandler.showMoveFileToPublicDirectoryDialog(fragmentManager = fragmentManager)
    positiveButtonClickSlot.captured.invoke()
    verify(exactly = 0) { fileHandler.showStorageSelectDialog() }
  }

  @Test
  fun showDirectlyCopyMoveDialogAfterFirstLaunch() {
    fileHandler = spyk(fileHandler)
    every { sharedPreferenceUtil.shouldShowStorageSelectionDialog } returns false
    every { fileHandler.storageDeviceList } returns listOf(mockk(), mockk())
    every { fileHandler.validateZimFileCanCopyOrMove() } returns true
    prepareFileSystemAndFileForMockk()
    every { alertDialogShower.show(any(), any(), any()) } just Runs
    fileHandler.showMoveFileToPublicDirectoryDialog(fragmentManager = fragmentManager)

    verify {
      alertDialogShower.show(
        KiwixDialog.CopyMoveFileToPublicDirectoryDialog,
        any(),
        any()
      )
    }
  }

  @Test
  fun copyMoveFunctionsShouldCallWhenClickingOnButtonsInCopyMoveDialog() {
    val positiveButtonClickSlot = slot<() -> Unit>()
    val negativeButtonClickSlot = slot<() -> Unit>()
    fileHandler = spyk(fileHandler)
    every { fileHandler.storageDeviceList } returns listOf(mockk(), mockk())
    every { sharedPreferenceUtil.shouldShowStorageSelectionDialog } returns false
    every {
      alertDialogShower.show(
        KiwixDialog.CopyMoveFileToPublicDirectoryDialog,
        capture(positiveButtonClickSlot),
        capture(negativeButtonClickSlot)
      )
    } just Runs

    every { fileHandler.validateZimFileCanCopyOrMove() } returns true
    fileHandler.showMoveFileToPublicDirectoryDialog(fragmentManager = fragmentManager)
    every { fileHandler.performCopyOperation() } just Runs

    positiveButtonClickSlot.captured.invoke()
    verify { fileHandler.performCopyOperation() }
    every { fileHandler.performMoveOperation() } just Runs
    negativeButtonClickSlot.captured.invoke()

    verify { fileHandler.performMoveOperation() }
  }

  private fun prepareFileSystemAndFileForMockk(
    storageFileExist: Boolean = true,
    freeSpaceInStorage: Long = 1000L,
    selectedFileLength: Long = 100L,
    availableStorageSize: Long = 1000L,
    fileSystemState: Fat32Checker.FileSystemState = CanWrite4GbFile
  ) {
    every { storageFile.exists() } returns storageFileExist
    every { storageFile.freeSpace } returns freeSpaceInStorage
    every { storageFile.path } returns storagePath
    every { selectedFile.length() } returns selectedFileLength
    every { storageCalculator.availableBytes(storageFile) } returns availableStorageSize
    every { fat32Checker.fileSystemStates.value } returns fileSystemState
  }

  @Test
  fun notifyFileOperationSuccessShouldCallOnFileMovedIfValidZIMFileAndIsMoveOperationIsTrue() =
    runTest {
      fileHandler = spyk(fileHandler)
      coEvery { fileHandler.isValidZimFile(destinationFile) } returns true
      fileHandler.isMoveOperation = true

      fileHandler.notifyFileOperationSuccess(destinationFile, sourceUri)

      verify { fileCopyMoveCallback.onFileMoved(destinationFile) }
      verify { fileHandler.dismissProgressDialog() }
      coVerify { fileHandler.deleteSourceFile(sourceUri) }
    }

  @Test
  fun notifyFileOperationSuccessShouldCallOnFileCopiedIfValidZIMFileAndIsMoveOperationIsFalse() =
    runTest {
      fileHandler = spyk(fileHandler)
      coEvery { fileHandler.isValidZimFile(destinationFile) } returns true
      fileHandler.isMoveOperation = false

      fileHandler.notifyFileOperationSuccess(destinationFile, sourceUri)

      verify { fileCopyMoveCallback.onFileCopied(destinationFile) }
      verify { fileHandler.dismissProgressDialog() }
    }

  @Test
  fun `notifyFileOperationSuccess should handle invalid ZIM file`() = runTest {
    fileHandler = spyk(fileHandler)
    fileHandler.shouldValidateZimFile = true
    coEvery { fileHandler.isValidZimFile(destinationFile) } returns false
    fileHandler.notifyFileOperationSuccess(destinationFile, sourceUri)

    verify { fileHandler.handleInvalidZimFile(destinationFile, sourceUri) }
  }

  @Test
  fun `handleInvalidZimFile should call onError if move is successful`() {
    fileHandler = spyk(fileHandler)
    every { fileHandler.tryMoveWithDocumentContract(any(), any(), any()) } returns true
    every { destinationFile.parentFile } returns mockk()
    fileHandler.isMoveOperation = true

    fileHandler.handleInvalidZimFile(destinationFile, sourceUri)

    verify { fileHandler.dismissProgressDialog() }
    verify { fileCopyMoveCallback.onError(activity.getString(R.string.error_file_invalid)) }
  }

  @Test
  fun `handleInvalidZimFile should delete file and show error if move fails`() {
    fileHandler = spyk(fileHandler)
    every { fileHandler.tryMoveWithDocumentContract(any(), any(), any()) } returns false
    every { destinationFile.parentFile } returns mockk()
    fileHandler.isMoveOperation = true

    fileHandler.handleInvalidZimFile(destinationFile, sourceUri)

    verify {
      fileHandler.handleFileOperationError(
        activity.getString(R.string.error_file_invalid),
        destinationFile
      )
    }
  }

  @AfterEach
  fun dispose() {
    fileHandler.dispose()
  }
}
