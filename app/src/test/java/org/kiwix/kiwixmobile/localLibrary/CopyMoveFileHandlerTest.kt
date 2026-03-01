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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import eu.mhutti1.utils.storage.StorageDevice
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.extensions.deleteFile
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.language.viewmodel.flakyTest
import org.kiwix.kiwixmobile.nav.destination.library.CopyMoveFileHandler
import org.kiwix.kiwixmobile.nav.destination.library.CopyMoveFileHandler.FileCopyMoveCallback
import org.kiwix.kiwixmobile.nav.destination.library.local.CopyMoveProgressBarController
import org.kiwix.kiwixmobile.nav.destination.library.local.FileOperationHandler
import org.kiwix.kiwixmobile.nav.destination.library.local.MultipleFilesProcessAction
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.Companion.FOUR_GIGABYTES_IN_KILOBYTES
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.DetectingFileSystem
import java.io.File

class CopyMoveFileHandlerTest {
  private lateinit var fileHandler: CopyMoveFileHandler

  private val activity: Activity = mockk(relaxed = true)
  private var kiwixDataStore: KiwixDataStore = mockk(relaxed = true)
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
  private val fileOperationHandler = mockk<FileOperationHandler>()
  private val copyMoveProgressBarController = mockk<CopyMoveProgressBarController>()

  @OptIn(ExperimentalCoroutinesApi::class)
  @BeforeEach
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    clearAllMocks()
    every { destinationFile.canRead() } returns true
    every { activity.getString(any()) } returns "mocked string"
    every { activity.getString(any(), any()) } returns "mocked string"
    fileHandler = CopyMoveFileHandler(
      activity,
      kiwixDataStore,
      storageCalculator,
      fat32Checker,
      fileOperationHandler,
      copyMoveProgressBarController
    ).apply {
      setAlertDialogShower(alertDialogShower)
      setSelectedFileAndUri(sourceUri, selectedFile)
      setLifeCycleScope(testScope)
      setFileCopyMoveCallback(fileCopyMoveCallback)
      setStorageFileForUnitTest(storageFile, destinationFile)
    }
    every { selectedFile.name } returns "test.zim"
    every { selectedFile.length() } returns 1024L
    every { storageFile.path } returns storagePath
  }

  @Test
  fun `validateZimFileCanCopyOrMove returns false when insufficient storage`() = runTest {
    coEvery { storageCalculator.availableBytes(storageFile) } returns 100L
    every { fat32Checker.fileSystemStates.value } returns CanWrite4GbFile

    val result = fileHandler.validateZimFileCanCopyOrMove()

    assertFalse(result)
    verify {
      fileCopyMoveCallback.insufficientSpaceInStorage(100L)
    }
  }

  @Test
  fun `DetectingFileSystem with file less than 4GB continues operation`() = runTest {
    fileHandler = spyk(fileHandler)
    coEvery { storageCalculator.availableBytes(storageFile) } returns 10_000L
    every { fat32Checker.fileSystemStates.value } returns DetectingFileSystem
    coEvery {
      fileOperationHandler.copy(any(), any(), any())
    } just Runs
    fileHandler.validateZimFileCanCopyOrMove()
    coVerify {
      fileHandler.handleDetectingFileSystemState(storageFile)
      fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable(storageFile)
      fileHandler.performCopyOperation()
      fileCopyMoveCallback.onMultipleFilesProcessSelection(MultipleFilesProcessAction.Copy)
    }
  }

  @Test
  fun `DetectingFileSystem with file larger than 4GB observes filesystem`() = runTest {
    every { selectedFile.length() } returns FOUR_GIGABYTES_IN_KILOBYTES + 1
    coEvery { storageCalculator.availableBytes(storageFile) } returns Long.MAX_VALUE
    every { fat32Checker.fileSystemStates.value } returns DetectingFileSystem

    fileHandler.validateZimFileCanCopyOrMove()

    verify {
      copyMoveProgressBarController.showPreparingCopyMoveDialog()
      fileHandler.observeFileSystemState()
    }
  }

  @Test
  fun `CannotWrite4GbFile shows filesystem limitation error`() = runTest {
    fileHandler = spyk(fileHandler)
    every { selectedFile.length() } returns FOUR_GIGABYTES_IN_KILOBYTES + 1
    coEvery { storageCalculator.availableBytes(storageFile) } returns Long.MAX_VALUE
    every { fat32Checker.fileSystemStates.value } returns CannotWrite4GbFile

    val result = fileHandler.validateZimFileCanCopyOrMove()

    assertFalse(result)
    coVerify {
      fileHandler.handleCannotWrite4GbFileState(storageFile)
      fileCopyMoveCallback.filesystemDoesNotSupportedCopyMoveFilesOver4GB()
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `copy operation success triggers onFileCopied`() = runTest {
    coEvery {
      fileOperationHandler.copy(any(), any(), any())
    } just Runs

    fileHandler.performCopyOperation()

    advanceUntilIdle()

    verify {
      fileCopyMoveCallback.onMultipleFilesProcessSelection(MultipleFilesProcessAction.Copy)
      copyMoveProgressBarController.showProgress(any())
      fileCopyMoveCallback.onFileCopied(any())
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `copy exception deletes destination and shows error and delete the destination file`() =
    runTest {
      coEvery {
        fileOperationHandler.copy(any(), any(), any())
      } throws RuntimeException("file not found")
      every { destinationFile.delete() } returns true

      fileHandler.performCopyOperation()

      advanceUntilIdle()

      verify {
        copyMoveProgressBarController.dismissCopyMoveProgressDialog()
        fileCopyMoveCallback.onError(any())
        destinationFile.delete()
      }
    }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `move operation success deletes source and triggers onFileMoved`() = runTest {
    coEvery { destinationFile.exists() } returns false
    val mockDocFile = mockk<DocumentFile>()
    val mockUri = mockk<Uri>()
    mockkStatic(DocumentFile::class)
    every { DocumentFile.fromFile(any()) } returns mockDocFile
    every { mockDocFile.uri } returns mockUri
    coEvery { fileOperationHandler.delete(any(), any()) } returns true
    coEvery {
      fileOperationHandler.move(any(), any(), any(), any(), any())
    } returns true

    fileHandler.performMoveOperation()

    advanceUntilIdle()

    coVerify {
      copyMoveProgressBarController.showProgress(any())
      fileCopyMoveCallback.onMultipleFilesProcessSelection(MultipleFilesProcessAction.Move)
      fileCopyMoveCallback.onFileMoved(any())
      fileOperationHandler.delete(sourceUri, selectedFile)
    }
  }

  @Test
  fun `CannotWrite4GbFile with file less than 4GB continues operation`() = runTest {
    fileHandler = spyk(fileHandler)
    every { selectedFile.length() } returns 1_000L
    every { fat32Checker.fileSystemStates.value } returns CannotWrite4GbFile
    coEvery { storageCalculator.availableBytes(storageFile) } returns Long.MAX_VALUE

    coEvery {
      fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable(any())
    } just Runs

    val result = fileHandler.validateZimFileCanCopyOrMove()

    assertFalse(result)
    coVerify {
      fileHandler.handleCannotWrite4GbFileState(storageFile)
      fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable(storageFile)
    }
  }

  @Test
  fun `DetectingFileSystem but insufficient storage shows error`() = runTest {
    every { fat32Checker.fileSystemStates.value } returns DetectingFileSystem
    coEvery { storageCalculator.availableBytes(storageFile) } returns 10L
    every { selectedFile.length() } returns 100L

    val result = fileHandler.validateZimFileCanCopyOrMove()

    assertFalse(result)
    verify { fileCopyMoveCallback.insufficientSpaceInStorage(10L) }
  }

  @Test
  fun `validateZimFileCanCopyOrMove returns true when FS and space valid`() = runTest {
    every { fat32Checker.fileSystemStates.value } returns CanWrite4GbFile
    coEvery { storageCalculator.availableBytes(storageFile) } returns Long.MAX_VALUE

    val result = fileHandler.validateZimFileCanCopyOrMove()

    assertTrue(result)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `observeFileSystemState does not start multiple collectors`() = runTest {
    fileHandler.observeFileSystemState()
    fileHandler.observeFileSystemState()
    advanceUntilIdle()
    verify(exactly = 1) {
      fat32Checker.fileSystemStates
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `copy move dialog message uses multiple files text`() = runTest {
    fileHandler = spyk(fileHandler)
    every { fat32Checker.fileSystemStates } returns MutableStateFlow(CanWrite4GbFile)
    coEvery { storageCalculator.availableBytes(any()) } returns Long.MAX_VALUE
    coEvery { fileHandler.getStorageDeviceList() } returns listOf(mockk())
    every {
      kiwixDataStore.shouldShowStorageSelectionDialogOnCopyMove
    } returns MutableStateFlow(true)
    fileHandler.showMoveFileToPublicDirectoryDialog(
      fragmentManager = fragmentManager,
      isSingleFileSelected = false
    )
    advanceUntilIdle()
    verify {
      copyMoveProgressBarController.showCopyMoveDialog(
        any(),
        any(),
        any()
      )
    }
  }

  @Test
  fun validateZimFileCanCopyOrMoveShouldReturnTrueWhenSufficientSpaceAndValidFileSystem() =
    runBlocking {
      prepareFileSystemAndFileForMockk()

      val result = fileHandler.validateZimFileCanCopyOrMove()

      assertTrue(result)
      // check insufficientSpaceInStorage callback should not call.
      verify(exactly = 0) { fileCopyMoveCallback.insufficientSpaceInStorage(any()) }
    }

  @Test
  fun validateZimFileCanCopyOrMoveShouldReturnFalseAndCallCallbackWhenInsufficientSpace() =
    runBlocking {
      prepareFileSystemAndFileForMockk(
        selectedFileLength = 2000L,
        fileSystemState = CanWrite4GbFile
      )
      val result = fileHandler.validateZimFileCanCopyOrMove()

      assertFalse(result)
      verify { fileCopyMoveCallback.insufficientSpaceInStorage(any()) }
    }

  @Test
  fun handleDetectingFileSystemStateShouldPerformCopyMoveOperationIfBookLessThan4GB() = runTest {
    fileHandler = spyk(fileHandler)
    prepareFileSystemAndFileForMockk()
    every { fileHandler.isBookLessThan4GB() } returns true
    coEvery { fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable(storageFile) } just Runs
    fileHandler.handleDetectingFileSystemState(storageFile)

    coVerify { fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable(storageFile) }
  }

  @Test
  fun handleDetectingFileSystemStateShouldObserveFileSystemStateIfBookGreaterThan4GB() = flakyTest {
    runTest {
      fileHandler = spyk(fileHandler)
      prepareFileSystemAndFileForMockk(fileSystemState = DetectingFileSystem)
      every { fileHandler.isBookLessThan4GB() } returns false
      every { fileHandler.observeFileSystemState() } just Runs

      fileHandler.handleDetectingFileSystemState(storageFile)
      verify { fileHandler.observeFileSystemState() }
    }
  }

  @Test
  fun handleCannotWrite4GbFileStateShouldPerformCopyMoveOperationIfBookLessThan4GB() = flakyTest {
    runTest {
      fileHandler = spyk(fileHandler)
      prepareFileSystemAndFileForMockk()
      every { fileHandler.isBookLessThan4GB() } returns true
      coEvery { fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable(storageFile) } just Runs

      fileHandler.handleCannotWrite4GbFileState(storageFile)

      coVerify { fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable(storageFile) }
    }
  }

  @Test
  fun handleCannotWrite4GbFileStateShouldCallCallbackIfBookGreaterThan4GB() = flakyTest {
    runTest {
      prepareFileSystemAndFileForMockk(
        selectedFileLength = FOUR_GIGABYTES_IN_KILOBYTES + 1
      )
      every {
        fileCopyMoveCallback.filesystemDoesNotSupportedCopyMoveFilesOver4GB()
      } just Runs

      fileHandler.handleCannotWrite4GbFileState(storageFile)

      verify {
        fileCopyMoveCallback.filesystemDoesNotSupportedCopyMoveFilesOver4GB()
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun showStorageConfigureDialogAtFirstLaunch() = runTest {
    fileHandler = spyk(fileHandler)
    fileHandler.setLifeCycleScope(this)
    val transaction = mockk<FragmentTransaction>()
    every { transaction.setReorderingAllowed(true) } returns mockk()
    every { transaction.add(any<DialogFragment>(), any()) } returns transaction
    every { fragmentManager.beginTransaction() } returns transaction
    every { transaction.commit() } returns 1
    val storageDeviceList = listOf<StorageDevice>(mockk(), mockk())

    coEvery { kiwixDataStore.shouldShowStorageSelectionDialogOnCopyMove } returns flowOf(true)
    coEvery { fileHandler.getStorageDeviceList() } returns storageDeviceList
    val positiveButtonClickSlot = slot<() -> Unit>()
    every {
      copyMoveProgressBarController.showCopyMoveDialog(
        any(),
        capture(positiveButtonClickSlot),
        any()
      )
    } just Runs
    coEvery { fileHandler.validateZimFileCanCopyOrMove() } returns true
    fileHandler.showMoveFileToPublicDirectoryDialog(
      fragmentManager = fragmentManager,
      isSingleFileSelected = true
    )
    positiveButtonClickSlot.captured.invoke()
    testDispatcher.scheduler.advanceUntilIdle()
    verify {
      fileCopyMoveCallback.onMultipleFilesProcessSelection(MultipleFilesProcessAction.Copy)
      fileHandler.showStorageSelectDialog(storageDeviceList)
    }
  }

  @Test
  fun shouldNotShowStorageConfigureDialogWhenThereIsOnlyInternalAvailable() = runBlocking {
    fileHandler = spyk(fileHandler)
    coEvery { kiwixDataStore.shouldShowStorageSelectionDialogOnCopyMove } returns flowOf(true)
    coEvery { fileHandler.getStorageDeviceList() } returns listOf(mockk())
    val positiveButtonClickSlot = slot<() -> Unit>()
    every {
      copyMoveProgressBarController.showCopyMoveDialog(
        any(),
        capture(positiveButtonClickSlot),
        any()
      )
    } just Runs
    coEvery { fileHandler.validateZimFileCanCopyOrMove() } returns true
    fileHandler.showMoveFileToPublicDirectoryDialog(
      fragmentManager = fragmentManager,
      isSingleFileSelected = true
    )
    positiveButtonClickSlot.captured.invoke()
    verify(exactly = 0) { fileHandler.showStorageSelectDialog(listOf(mockk())) }
  }

  @Test
  fun showDirectlyCopyMoveDialogAfterFirstLaunch() = runBlocking {
    fileHandler = spyk(fileHandler)
    coEvery { kiwixDataStore.shouldShowStorageSelectionDialogOnCopyMove } returns flowOf(false)
    coEvery { fileHandler.getStorageDeviceList() } returns listOf(mockk(), mockk())
    coEvery { fileHandler.validateZimFileCanCopyOrMove() } returns true
    prepareFileSystemAndFileForMockk()
    every { alertDialogShower.show(any(), any(), any()) } just Runs
    fileHandler.showMoveFileToPublicDirectoryDialog(
      fragmentManager = fragmentManager,
      isSingleFileSelected = true
    )

    verify {
      copyMoveProgressBarController.showCopyMoveDialog(
        any(),
        any(),
        any()
      )
    }
  }

  @Test
  fun copyMoveFunctionsShouldCallWhenClickingOnButtonsInCopyMoveDialog() {
    runBlocking {
      val positiveButtonClickSlot = slot<() -> Unit>()
      val negativeButtonClickSlot = slot<() -> Unit>()
      fileHandler = spyk(fileHandler)
      coEvery { fileHandler.getStorageDeviceList() } returns listOf(mockk(), mockk())
      coEvery { storageCalculator.availableBytes(any()) } returns Long.MAX_VALUE
      every { fat32Checker.fileSystemStates } returns MutableStateFlow(CanWrite4GbFile)
      coEvery { kiwixDataStore.shouldShowStorageSelectionDialogOnCopyMove } returns flowOf(false)
      every {
        copyMoveProgressBarController.showCopyMoveDialog(
          any(),
          capture(positiveButtonClickSlot),
          capture(negativeButtonClickSlot)
        )
      } just Runs

      coEvery { fileHandler.validateZimFileCanCopyOrMove() } returns true
      fileHandler.showMoveFileToPublicDirectoryDialog(
        fragmentManager = fragmentManager,
        isSingleFileSelected = true
      )
      coEvery { fileHandler.performCopyOperation() } just Runs

      positiveButtonClickSlot.captured.invoke()
      coEvery { fileHandler.performCopyOperation() }
      coEvery { fileHandler.performMoveOperation() } just Runs
      negativeButtonClickSlot.captured.invoke()

      coEvery { fileHandler.performMoveOperation() }
    }
  }

  private fun prepareFileSystemAndFileForMockk(
    storageFileExist: Boolean = true,
    freeSpaceInStorage: Long = 1000L,
    selectedFileLength: Long = 100L,
    availableStorageSize: Long = 1000L,
    fileSystemState: Fat32Checker.FileSystemState = CanWrite4GbFile
  ) {
    every { kiwixDataStore.selectedStorage } returns flowOf(storagePath)
    every { kiwixDataStore.selectedStorage } answers { flowOf(storagePath) }
    every { storageFile.exists() } returns storageFileExist
    every { storageFile.freeSpace } returns freeSpaceInStorage
    every { storageFile.path } returns storagePath
    every { selectedFile.length() } returns selectedFileLength
    coEvery { storageCalculator.availableBytes(storageFile) } returns availableStorageSize
    every { fat32Checker.fileSystemStates.value } returns fileSystemState
  }

  @Test
  fun notifyFileOperationSuccessShouldCallOnFileMovedIfValidZIMFileAndIsMoveOperationIsTrue() =
    runTest {
      fileHandler = spyk(fileHandler)
      coEvery { fileOperationHandler.delete(any(), any()) } returns true
      coEvery { fileHandler.isValidZimFile(destinationFile) } returns true
      fileHandler.isMoveOperation = true

      fileHandler.notifyFileOperationSuccess(destinationFile, sourceUri)

      verify { fileCopyMoveCallback.onFileMoved(destinationFile) }
      verify { copyMoveProgressBarController.dismissCopyMoveProgressDialog() }
      coVerify { fileOperationHandler.delete(sourceUri, selectedFile) }
    }

  @Test
  fun notifyFileOperationSuccessShouldCallOnFileCopiedIfValidZIMFileAndIsMoveOperationIsFalse() =
    runTest {
      fileHandler = spyk(fileHandler)
      coEvery { fileHandler.isValidZimFile(destinationFile) } returns true
      fileHandler.isMoveOperation = false

      fileHandler.notifyFileOperationSuccess(destinationFile, sourceUri)

      verify { fileCopyMoveCallback.onFileCopied(destinationFile) }
      verify { copyMoveProgressBarController.dismissCopyMoveProgressDialog() }
    }

  @Test
  fun `notifyFileOperationSuccess should handle invalid ZIM file`() = runTest {
    fileHandler = spyk(fileHandler)
    fileHandler.shouldValidateZimFile = true
    every { destinationFile.path } returns ""
    coEvery { destinationFile.delete() } returns true
    coEvery { fileHandler.isValidZimFile(destinationFile) } returns false
    fileHandler.notifyFileOperationSuccess(destinationFile, sourceUri)

    coVerify { fileHandler.handleInvalidZimFile(destinationFile, sourceUri) }
  }

  @Test
  fun `handleInvalidZimFile should call onError if move is successful`() {
    runBlocking {
      fileHandler = spyk(fileHandler)
      coEvery { fileOperationHandler.rollbackMove(any(), any()) } returns true
      coEvery {
        fileOperationHandler.move(any(), any(), any(), any(), any())
      } returns true
      every { destinationFile.parentFile } returns mockk()
      every { destinationFile.path } returns ""
      fileHandler.isMoveOperation = true

      fileHandler.handleInvalidZimFile(destinationFile, sourceUri)

      verify { copyMoveProgressBarController.dismissCopyMoveProgressDialog() }
      verify {
        fileCopyMoveCallback.onError(any())
      }
    }
  }

  @Test
  fun `handleInvalidZimFile should delete file and show error if move fails`() {
    runBlocking {
      fileHandler = spyk(fileHandler)
      every { destinationFile.path } returns ""
      every { destinationFile.delete() } returns true
      coEvery { fileOperationHandler.rollbackMove(any(), any()) } returns false
      coEvery {
        fileOperationHandler.move(any(), any(), any(), any(), any())
      } returns false
      every { destinationFile.parentFile } returns mockk()
      fileHandler.isMoveOperation = true

      fileHandler.handleInvalidZimFile(destinationFile, sourceUri)

      coVerify {
        fileHandler.handleFileOperationError(
          any(),
          destinationFile
        )
        destinationFile.deleteFile()
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @AfterEach
  fun dispose() {
    Dispatchers.resetMain()
    fileHandler.dispose()
  }
}
