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
import androidx.documentfile.provider.DocumentFile
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
    every { fileHandler.showCopyMoveDialog() } just Runs
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
  fun handleDetectingFileSystemStateShouldShowCopyMoveDialogIfBookLessThan4GB() {
    fileHandler = spyk(fileHandler)
    prepareFileSystemAndFileForMockk()
    every { fileHandler.isBookLessThan4GB() } returns true
    every { fileHandler.showCopyMoveDialog() } just Runs

    fileHandler.handleDetectingFileSystemState()

    verify { fileHandler.showCopyMoveDialog() }
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
  fun handleCannotWrite4GbFileStateShouldShowCopyMoveDialogIfBookLessThan4GB() {
    fileHandler = spyk(fileHandler)
    prepareFileSystemAndFileForMockk()
    every { fileHandler.isBookLessThan4GB() } returns true
    every { fileHandler.showCopyMoveDialog() } just Runs

    fileHandler.handleCannotWrite4GbFileState()

    verify { fileHandler.showCopyMoveDialog() }
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
  fun showMoveToPublicDirectoryPermissionDialogShouldShowPermissionDialogAtFirstLaunch() {
    every { sharedPreferenceUtil.copyMoveZimFilePermissionDialog } returns false
    every { alertDialogShower.show(any(), any(), any()) } just Runs
    fileHandler.showMoveFileToPublicDirectoryDialog()

    verify {
      alertDialogShower.show(
        KiwixDialog.MoveFileToPublicDirectoryPermissionDialog,
        any(),
        any()
      )
    }
  }

  @Test
  fun copyMoveFunctionsShouldCallWhenClickingOnButtonsInPermissionDialog() {
    val positiveButtonClickSlot = slot<() -> Unit>()
    val negativeButtonClickSlot = slot<() -> Unit>()
    fileHandler = spyk(fileHandler)
    every { sharedPreferenceUtil.copyMoveZimFilePermissionDialog } returns false
    every {
      alertDialogShower.show(
        KiwixDialog.MoveFileToPublicDirectoryPermissionDialog,
        capture(positiveButtonClickSlot),
        capture(negativeButtonClickSlot)
      )
    } just Runs

    fileHandler.showMoveFileToPublicDirectoryDialog()
    every { fileHandler.validateZimFileCanCopyOrMove() } returns true
    every { fileHandler.performCopyOperation() } just Runs

    positiveButtonClickSlot.captured.invoke()
    verify { fileHandler.performCopyOperation() }
    every { sharedPreferenceUtil.copyMoveZimFilePermissionDialog } returns false
    every { fileHandler.performMoveOperation() } just Runs
    negativeButtonClickSlot.captured.invoke()

    verify { fileHandler.performMoveOperation() }

    verify { sharedPreferenceUtil.copyMoveZimFilePermissionDialog = true }
  }

  @Test
  fun showCopyMoveDialog() {
    every { sharedPreferenceUtil.copyMoveZimFilePermissionDialog } returns true
    prepareFileSystemAndFileForMockk()
    every { alertDialogShower.show(any(), any(), any()) } just Runs
    fileHandler.showMoveFileToPublicDirectoryDialog()

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
    every { sharedPreferenceUtil.copyMoveZimFilePermissionDialog } returns true
    every {
      alertDialogShower.show(
        KiwixDialog.CopyMoveFileToPublicDirectoryDialog,
        capture(positiveButtonClickSlot),
        capture(negativeButtonClickSlot)
      )
    } just Runs

    every { fileHandler.validateZimFileCanCopyOrMove() } returns true
    fileHandler.showMoveFileToPublicDirectoryDialog()
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

  @AfterEach
  fun dispose() {
    fileHandler.dispose()
  }
}
