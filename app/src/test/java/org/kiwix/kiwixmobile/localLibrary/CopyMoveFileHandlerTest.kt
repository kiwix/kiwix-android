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

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import eu.mhutti1.utils.storage.StorageDevice
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.EXTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.INTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.nav.destination.library.CopyMoveFileHandler
import org.kiwix.kiwixmobile.nav.destination.library.local.CopyMoveProgressBarController
import org.kiwix.kiwixmobile.nav.destination.library.local.FileOperationHandler
import org.kiwix.kiwixmobile.nav.destination.library.local.MultipleFilesProcessAction
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.Companion.FOUR_GIGABYTES_IN_KILOBYTES
import org.kiwix.sharedFunctions.MainDispatcherRule
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class CopyMoveFileHandlerTest {
  private val context: Context = mockk()
  private val kiwixDataStore: KiwixDataStore = mockk()

  private val storageCalculator: StorageCalculator = mockk()
  private val fat32Checker: Fat32Checker = mockk()
  private val fileOperationHandler = mockk<FileOperationHandler>()
  private val copyMoveProgressBarController = mockk<CopyMoveProgressBarController>(relaxed = true)

  @RegisterExtension
  private val dispatcher = MainDispatcherRule()
  private lateinit var fileHandler: CopyMoveFileHandler

  private val storageFile: File = mockk()
  private val destinationFile: File = mockk()
  private val selectedFile: DocumentFile = mockk()
  private val fileCopyMoveCallback: CopyMoveFileHandler.FileCopyMoveCallback = mockk()

  @BeforeEach
  fun setup() {
    fileHandler = CopyMoveFileHandler(
      context = context,
      kiwixDataStore = kiwixDataStore,
      storageCalculator = storageCalculator,
      fat32Checker = fat32Checker,
      fileOperationHandler = fileOperationHandler,
      copyMoveProgressBarController = copyMoveProgressBarController,
      dispatcher = dispatcher.dispatcher
    )

    fileHandler.setStorageFileForUnitTest(storageFile, destinationFile)
    fileHandler.setSelectedFileAndUri(mockk(), selectedFile)
    fileHandler.setFileCopyMoveCallback(fileCopyMoveCallback)
    every { selectedFile.length() } returns 1000L
    every { selectedFile.name } returns "test.zim"
    every { context.getString(any(), any()) } returns "Test String"
    every { destinationFile.path } returns "/storage/test.zim"
  }

  @AfterEach
  fun dispose() {
    clearAllMocks()
  }

  @Nested
  inner class ShowMoveFileToPublicDirectoryDialog {
    @Test
    fun getStorageDeviceList_whenIsEmpty_showPreparingCopyMoveDialog() = runTest {
      fileHandler = spyk(fileHandler)

      coEvery {
        fileHandler.validateZimFileCanCopyOrMove()
      } returns false

      every {
        kiwixDataStore.shouldShowStorageSelectionDialogOnCopyMove
      } returns MutableStateFlow(false)

      fileHandler.showMoveFileToPublicDirectoryDialog(
        storageDeviceList = emptyList(),
        fragmentManager = mockk(relaxed = true),
        isSingleFileSelected = true
      )

      verify(exactly = 1) {
        copyMoveProgressBarController.showPreparingCopyMoveDialog()
      }
    }

    @Nested
    inner class ShowStorageSelectionDialogEnabled {
      @Test
      fun whenShowStorageAndMultipleStorageDevice_showsDialogAndHidesPreparingDialog() = runTest {
        fileHandler = spyk(fileHandler)

        every {
          context.getString(any())
        } returns "Test String"
        coEvery {
          kiwixDataStore.shouldShowStorageSelectionDialogOnCopyMove
        } returns MutableStateFlow(true)

        every {
          fileHandler.showStorageSelectDialog(any())
        } just Runs

        val devices = listOf(
          StorageDevice(File("/internal"), true),
          StorageDevice(File("/sdcard"), false)
        )

        fileHandler.showMoveFileToPublicDirectoryDialog(
          storageDeviceList = devices,
          fragmentManager = mockk(relaxed = true),
          isSingleFileSelected = true
        )

        verify {
          copyMoveProgressBarController.hidePreparingCopyMoveDialog()
        }
        verify {
          copyMoveProgressBarController.showCopyMoveDialog(any(), any(), any())
        }
      }

      @Nested
      inner class ShowStorageSelectionDialogDisabled {
        @Test
        fun whenOnlyOneStorageDeviceAvailable_enablesStorageSelectionDialog() = runTest {
          coEvery {
            kiwixDataStore.shouldShowStorageSelectionDialogOnCopyMove
          } returns MutableStateFlow(false)

          coEvery {
            kiwixDataStore.setShowStorageSelectionDialogOnCopyMove(true)
          } just Runs

          fileHandler = spyk(fileHandler)

          coEvery {
            fileHandler.validateZimFileCanCopyOrMove()
          } returns false

          fileHandler.showMoveFileToPublicDirectoryDialog(
            storageDeviceList = listOf(
              StorageDevice(File("/internal"), true)
            ),
            fragmentManager = mockk(relaxed = true),
            isSingleFileSelected = true
          )

          coVerify {
            kiwixDataStore.setShowStorageSelectionDialogOnCopyMove(true)
          }
          verify {
            copyMoveProgressBarController.hidePreparingCopyMoveDialog()
          }
        }

        @Nested
        inner class ValidateZimFileCanCopyOrMoveTrue {
          @Test
          fun whenMultipleFilesProcessActionCopy_performsCopyOperation() = runTest {
            fileHandler = spyk(fileHandler)

            val storageDevice = StorageDevice(
              File("/storage/internal"),
              true
            )

            coEvery {
              kiwixDataStore.shouldShowStorageSelectionDialogOnCopyMove
            } returns MutableStateFlow(false)

            coEvery {
              fileHandler.validateZimFileCanCopyOrMove()
            } returns true

            coEvery {
              fileHandler.performCopyOperation()
            } just Runs

            fileHandler.showMoveFileToPublicDirectoryDialog(
              storageDeviceList = listOf(storageDevice),
              fragmentManager = mockk(relaxed = true),
              multipleFilesProcessAction = MultipleFilesProcessAction.Copy,
              isSingleFileSelected = true
            )

            coVerify(exactly = 1) {
              fileHandler.performCopyOperation()
            }
          }

          @Test
          fun whenMultipleFilesProcessActionMove_performsMoveOperation() = runTest {
            fileHandler = spyk(fileHandler)

            val storageDevice = StorageDevice(
              File("/storage/internal"),
              true
            )

            coEvery {
              kiwixDataStore.shouldShowStorageSelectionDialogOnCopyMove
            } returns MutableStateFlow(false)

            coEvery {
              fileHandler.validateZimFileCanCopyOrMove()
            } returns true

            coEvery {
              fileHandler.performMoveOperation()
            } just Runs

            fileHandler.showMoveFileToPublicDirectoryDialog(
              storageDeviceList = listOf(storageDevice),
              fragmentManager = mockk(relaxed = true),
              multipleFilesProcessAction = MultipleFilesProcessAction.Move,
              isSingleFileSelected = true
            )

            coVerify(exactly = 1) {
              fileHandler.performMoveOperation()
            }
          }

          @Test
          fun whenMultipleFilesProcessActionIsNull_showsCopyMoveDialog() = runTest {
            coEvery {
              kiwixDataStore.shouldShowStorageSelectionDialogOnCopyMove
            } returns MutableStateFlow(false)

            fileHandler = spyk(fileHandler)

            coEvery {
              fileHandler.validateZimFileCanCopyOrMove()
            } returns true

            every {
              context.getString(any())
            } returns "Copy Move Dialog"

            every {
              copyMoveProgressBarController.showCopyMoveDialog(
                any(),
                any(),
                any()
              )
            } just Runs

            fileHandler.showMoveFileToPublicDirectoryDialog(
              storageDeviceList = listOf(
                StorageDevice(File("/storage/internal"), true)
              ),
              fragmentManager = mockk(relaxed = true),
              multipleFilesProcessAction = null,
              isSingleFileSelected = true
            )

            verify(exactly = 1) {
              copyMoveProgressBarController.showCopyMoveDialog(
                any(),
                any(),
                any()
              )
            }
          }
        }
      }
    }
  }

  @Nested
  inner class CopyMoveZIMFileInSelectedStorage {
    @BeforeEach
    fun setupCopyMoveStorage() {
      coEvery {
        kiwixDataStore.setShowStorageSelectionDialogOnCopyMove(any())
      } just Runs

      coEvery {
        kiwixDataStore.setSelectedStorage(any())
      } just Runs

      coEvery {
        kiwixDataStore.setSelectedStoragePosition(any())
      } just Runs

      coEvery {
        kiwixDataStore.getPublicDirectoryPath(any())
      } returns "/storage/internal"
    }

    @Test
    fun updatesStorageConfiguration() = runTest {
      fileHandler = spyk(fileHandler)

      coEvery {
        fileHandler.validateZimFileCanCopyOrMove()
      } returns false

      val storageDevice = StorageDevice(
        File("/storage/internal"),
        true
      )

      fileHandler.copyMoveZIMFileInSelectedStorage(storageDevice)

      coVerify {
        kiwixDataStore.setShowStorageSelectionDialogOnCopyMove(false)

        kiwixDataStore.setSelectedStorage("/storage/internal")
      }
    }

    @Test
    fun internalStorage_setsInternalStoragePosition() = runTest {
      fileHandler = spyk(fileHandler)

      coEvery {
        fileHandler.validateZimFileCanCopyOrMove()
      } returns false

      val storageDevice = StorageDevice(
        File("/storage/internal"),
        true
      )

      fileHandler.copyMoveZIMFileInSelectedStorage(storageDevice)

      coVerify {
        kiwixDataStore.setSelectedStoragePosition(
          INTERNAL_SELECT_POSITION
        )
      }
    }

    @Test
    fun externalStorage_setsExternalStoragePosition() = runTest {
      fileHandler = spyk(fileHandler)

      coEvery {
        fileHandler.validateZimFileCanCopyOrMove()
      } returns false

      val storageDevice = StorageDevice(
        File("/storage/sdcard"),
        false
      )

      fileHandler.copyMoveZIMFileInSelectedStorage(storageDevice)

      coVerify {
        kiwixDataStore.setSelectedStoragePosition(
          EXTERNAL_SELECT_POSITION
        )
      }
    }

    @Test
    fun validationSuccess_performsCopyOperation() = runTest {
      val storageDevice = StorageDevice(
        File("/storage/internal"),
        true
      )

      coEvery {
        storageCalculator.availableBytes(storageFile)
      } returns 5000L

      every {
        fat32Checker.fileSystemStates.value
      } returns Fat32Checker.FileSystemState.CanWrite4GbFile

      coEvery {
        fileOperationHandler.copy(any(), any(), any())
      } just Runs

      fileHandler.copyMoveZIMFileInSelectedStorage(storageDevice)

      coVerify {
        fileOperationHandler.copy(any(), any(), any())
      }
    }
  }

  @Nested
  inner class IsBookLessThan4GB {
    @Test
    fun fileSmallerThan4Gb_returnsTrue() {
      every {
        selectedFile.length()
      } returns 1000L

      assertTrue(fileHandler.isBookLessThan4GB())
    }

    @Test
    fun fileGreaterThan4Gb_returnsFalse() {
      every {
        selectedFile.length()
      } returns FOUR_GIGABYTES_IN_KILOBYTES + 1

      assertFalse(fileHandler.isBookLessThan4GB())
    }
  }

  @Nested
  inner class ValidateZimFileCanCopyOrMove {
    @Test
    fun insufficientStorage_returnsFalseAndNotifiesCallback() = runTest {
      every { selectedFile.length() } returns 1000L
      coEvery { storageCalculator.availableBytes(storageFile) } returns 100L

      val result = fileHandler.validateZimFileCanCopyOrMove()

      assertFalse(result)

      verify {
        copyMoveProgressBarController.hidePreparingCopyMoveDialog()
        fileCopyMoveCallback.insufficientSpaceInStorage(100L)
      }
    }

    @Test
    fun detectingFileSystem_returnsFalse() = runTest {
      every { selectedFile.length() } returns 1000L

      coEvery {
        storageCalculator.availableBytes(storageFile)
      } returns 5000L

      every {
        fat32Checker.fileSystemStates.value
      } returns Fat32Checker.FileSystemState.DetectingFileSystem

      val result = fileHandler.validateZimFileCanCopyOrMove()

      assertFalse(result)

      verify {
        copyMoveProgressBarController.hidePreparingCopyMoveDialog()
      }
    }

    @Test
    fun cannotWrite4GbFile_returnsFalse() = runTest {
      every { selectedFile.length() } returns 1000L

      coEvery {
        storageCalculator.availableBytes(storageFile)
      } returns 5000L

      every {
        fat32Checker.fileSystemStates.value
      } returns Fat32Checker.FileSystemState.CannotWrite4GbFile

      val result = fileHandler.validateZimFileCanCopyOrMove()

      assertFalse(result)

      verify {
        copyMoveProgressBarController.hidePreparingCopyMoveDialog()
      }
    }

    @Test
    fun supportedFileSystem_returnsTrue() = runTest {
      every { selectedFile.length() } returns 1000L

      coEvery {
        storageCalculator.availableBytes(storageFile)
      } returns 5000L

      every {
        fat32Checker.fileSystemStates.value
      } returns Fat32Checker.FileSystemState.CanWrite4GbFile

      val result = fileHandler.validateZimFileCanCopyOrMove()

      assertTrue(result)

      verify {
        copyMoveProgressBarController.hidePreparingCopyMoveDialog()
      }

      verify(exactly = 0) {
        fileCopyMoveCallback.insufficientSpaceInStorage(any())
      }
    }
  }

  @Nested
  inner class HandleDetectingFileSystemState {
    @Test
    fun bookLessThan4Gb_doesNotShowPreparingDialog() = runTest {
      fileHandler = spyk(fileHandler)
      every {
        selectedFile.length()
      } returns 1000L

      coEvery {
        fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable(storageFile)
      } just Runs

      fileHandler.handleDetectingFileSystemState(storageFile)

      coVerify {
        fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable(storageFile)
      }

      verify(exactly = 0) {
        copyMoveProgressBarController.showPreparingCopyMoveDialog()
      }
      verify(exactly = 0) {
        fileHandler.observeFileSystemState()
      }
    }

    @Test
    fun bookGreaterThan4Gb_showsPreparingDialogAndObservesFileSystemState() = runTest {
      fileHandler = spyk(fileHandler)

      every {
        selectedFile.length()
      } returns FOUR_GIGABYTES_IN_KILOBYTES + 1

      every {
        fileHandler.observeFileSystemState()
      } just Runs

      fileHandler.handleDetectingFileSystemState(storageFile)

      verify {
        copyMoveProgressBarController.showPreparingCopyMoveDialog()
      }

      verify {
        fileHandler.observeFileSystemState()
      }
    }
  }

  @Nested
  inner class HandleCannotWrite4GbFileState {
    @Test
    fun bookLessThan4Gb_performsCopyMoveOperationIfSufficientSpaceAvailable() = runTest {
      fileHandler = spyk(fileHandler)

      every {
        selectedFile.length()
      } returns 1000L

      coEvery {
        fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable(storageFile)
      } just Runs

      fileHandler.handleCannotWrite4GbFileState(storageFile)

      coVerify {
        fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable(storageFile)
      }

      verify(exactly = 0) {
        fileCopyMoveCallback.filesystemDoesNotSupportedCopyMoveFilesOver4GB()
      }
    }

    @Test
    fun bookGreaterThan4Gb_notifiesFilesystemLimitation() = runTest {
      every {
        selectedFile.length()
      } returns FOUR_GIGABYTES_IN_KILOBYTES + 1

      fileHandler.handleCannotWrite4GbFileState(storageFile)

      verify {
        fileCopyMoveCallback.filesystemDoesNotSupportedCopyMoveFilesOver4GB()
      }
    }
  }

  @Nested
  inner class ObserveFileSystemState {
    @Test
    fun observingJobAlreadyRunning_doesNothing() {
      val activeJob = mockk<Job>()

      every { activeJob.isActive } returns true

      fileHandler.setStorageObservingJob(activeJob)

      fileHandler.observeFileSystemState()

      verify(exactly = 0) {
        fat32Checker.fileSystemStates
      }
    }

    @Test
    fun fileSystemDetectionCompletes_hidesPreparingDialog() = runTest {
      fileHandler = spyk(fileHandler)

      fileHandler.setLifeCycleScope(this)

      every {
        fat32Checker.fileSystemStates
      } returns MutableStateFlow(
        Fat32Checker.FileSystemState.CanWrite4GbFile
      )

      coEvery {
        fileHandler.validateZimFileCanCopyOrMove()
      } returns false

      fileHandler.observeFileSystemState()

      advanceUntilIdle()

      verify {
        copyMoveProgressBarController.hidePreparingCopyMoveDialog()
      }
    }

    @Test
    fun fileSystemDetectionCompletes_checksValidation() = runTest {
      fileHandler = spyk(fileHandler)

      fileHandler.setLifeCycleScope(this)

      every {
        fat32Checker.fileSystemStates
      } returns MutableStateFlow(
        Fat32Checker.FileSystemState.CanWrite4GbFile
      )

      coEvery {
        fileHandler.validateZimFileCanCopyOrMove()
      } returns true

      fileHandler.observeFileSystemState()

      advanceUntilIdle()

      coVerify(exactly = 1) {
        fileHandler.validateZimFileCanCopyOrMove()
      }
    }
  }

  @Nested
  inner class PerformCopyMoveOperationIfSufficientSpaceAvailable {
    @Test
    fun insufficientStorage_notifiesCallback() = runTest {
      every { selectedFile.length() } returns 1000L

      coEvery {
        storageCalculator.availableBytes(storageFile)
      } returns 100L

      fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable(storageFile)

      verify {
        fileCopyMoveCallback.insufficientSpaceInStorage(100L)
      }
    }

    @Test
    fun sufficientStorage_doesNotNotifyInsufficientStorage() = runTest {
      coEvery {
        storageCalculator.availableBytes(storageFile)
      } returns 5000L

      fileHandler.performCopyMoveOperationIfSufficientSpaceAvailable(storageFile)

      verify(exactly = 0) {
        fileCopyMoveCallback.insufficientSpaceInStorage(any())
      }
    }
  }

  @Nested
  inner class PerformCopyOperation {
    @Test
    fun performCopyOperation_setsCopyStateAndNotifiesSelection() = runTest {
      fileHandler = spyk(fileHandler)

      every {
        fileHandler.showStorageSelectDialog(any())
      } returns Unit

      fileHandler.isMoveOperation = true

      fileHandler.performCopyOperation(true)

      assertFalse(fileHandler.isMoveOperation)

      verify {
        fileCopyMoveCallback.onMultipleFilesProcessSelection(
          MultipleFilesProcessAction.Copy
        )
      }
    }

    @Test
    fun showStorageSelectionDialogTrue_showsStorageDialog() = runTest {
      fileHandler = spyk(fileHandler)

      every {
        fileHandler.showStorageSelectDialog(any())
      } returns Unit

      fileHandler.performCopyOperation(true)

      verify {
        fileHandler.showStorageSelectDialog(any())
      }
    }

    @Test
    fun showStorageSelectionDialogFalse_copiesFile() = runTest {
      coEvery {
        fileOperationHandler.copy(any(), any(), any())
      } just Runs

      fileHandler.performCopyOperation(false)

      coVerify {
        fileOperationHandler.copy(any(), any(), any())
      }
    }
  }

  @Nested
  inner class PerformMoveOperation {
    @Test
    fun whenInvoked_setsMoveStateAndNotifiesSelection() = runTest {
      fileHandler = spyk(fileHandler)

      every {
        fileHandler.showStorageSelectDialog(any())
      } returns Unit

      fileHandler.isMoveOperation = false

      fileHandler.performMoveOperation(true)

      assertTrue(fileHandler.isMoveOperation)

      verify {
        fileCopyMoveCallback.onMultipleFilesProcessSelection(
          MultipleFilesProcessAction.Move
        )
      }
    }

    @Test
    fun showStorageSelectionDialogTrue_showsStorageDialog() = runTest {
      fileHandler = spyk(fileHandler)

      every {
        fileHandler.showStorageSelectDialog(any())
      } returns Unit

      fileHandler.performMoveOperation(true)

      verify {
        fileHandler.showStorageSelectDialog(any())
      }
    }
  }

  @Nested
  inner class HandleFileOperationError {
    @Test
    fun handlesFileOperationError() = runTest {
      every {
        destinationFile.delete()
      } returns true

      fileHandler.handleFileOperationError(
        errorMessage = "File copy failed",
        destinationFile = destinationFile
      )

      verify {
        copyMoveProgressBarController.dismissCopyMoveProgressDialog()

        fileCopyMoveCallback.onError("File copy failed")
      }

      verify {
        destinationFile.delete()
      }
    }
  }

  @Nested
  inner class NotifyFileOperationSuccess {
    @Test
    fun validationEnabledAndInvalidFile_handlesInvalidZimFile() = runTest {
      fileHandler = spyk(fileHandler)

      fileHandler.shouldValidateZimFile = true

      val sourceUri = mockk<Uri>()

      coEvery {
        fileHandler.isValidZimFile(destinationFile)
      } returns false

      coEvery {
        fileHandler.handleInvalidZimFile(destinationFile, sourceUri)
      } just Runs

      fileHandler.notifyFileOperationSuccess(
        destinationFile,
        sourceUri
      )

      coVerify {
        fileHandler.handleInvalidZimFile(
          destinationFile,
          sourceUri
        )
      }

      verify(exactly = 0) {
        copyMoveProgressBarController.dismissCopyMoveProgressDialog()
      }
    }

    @Test
    fun copyOperation_notifiesCopiedAndDismissesDialog() = runTest {
      val sourceUri = mockk<Uri>()

      fileHandler.shouldValidateZimFile = false
      fileHandler.isMoveOperation = false

      fileHandler.notifyFileOperationSuccess(
        destinationFile,
        sourceUri
      )

      verify {
        copyMoveProgressBarController.dismissCopyMoveProgressDialog()

        fileCopyMoveCallback.onFileCopied(destinationFile)
      }

      verify(exactly = 0) {
        fileCopyMoveCallback.onFileMoved(any())
      }
    }

    @Test
    fun moveOperation_deletesSourceFileAndNotifiesMovedAndDismissesDialog() = runTest {
      val sourceUri = mockk<Uri>()

      fileHandler.isMoveOperation = true

      coEvery {
        fileOperationHandler.delete(any(), any())
      } returns true

      fileHandler.notifyFileOperationSuccess(
        destinationFile,
        sourceUri
      )

      verify {
        copyMoveProgressBarController.dismissCopyMoveProgressDialog()

        fileCopyMoveCallback.onFileMoved(destinationFile)
      }

      coVerify {
        fileOperationHandler.delete(
          sourceUri,
          selectedFile
        )
      }

      verify(exactly = 0) {
        fileCopyMoveCallback.onFileCopied(any())
      }
    }
  }

  @Nested
  inner class HandleInvalidZimFile {
    @Test
    fun copyOperation_handlesFileOperationError() = runTest {
      fileHandler = spyk(fileHandler)

      val sourceUri = mockk<Uri>()

      fileHandler.isMoveOperation = false

      coEvery {
        fileHandler.handleFileOperationError(any(), destinationFile)
      } just Runs

      fileHandler.handleInvalidZimFile(
        destinationFile,
        sourceUri
      )

      coVerify {
        fileHandler.handleFileOperationError(any(), destinationFile)
      }
    }

    @Test
    fun moveOperationRollbackSuccess_notifiesErrorAndDismissesProgressDialog() = runTest {
      val sourceUri = mockk<Uri>()

      fileHandler.isMoveOperation = true

      every {
        context.getString(any(), any())
      } returns "Invalid Zim"

      coEvery {
        fileOperationHandler.rollbackMove(
          destinationFile,
          sourceUri
        )
      } returns true

      fileHandler.handleInvalidZimFile(
        destinationFile,
        sourceUri
      )

      verify {
        copyMoveProgressBarController.dismissCopyMoveProgressDialog()

        fileCopyMoveCallback.onError("Invalid Zim")
      }
    }

    @Test
    fun moveOperationRollbackFailure_handlesFileOperationError() = runTest {
      fileHandler = spyk(fileHandler)

      val sourceUri = mockk<Uri>()

      fileHandler.isMoveOperation = true

      coEvery {
        fileOperationHandler.rollbackMove(
          destinationFile,
          sourceUri
        )
      } returns false

      coEvery {
        fileHandler.handleFileOperationError(any(), destinationFile)
      } just Runs

      fileHandler.handleInvalidZimFile(
        destinationFile,
        sourceUri
      )

      coVerify {
        fileHandler.handleFileOperationError(
          any(),
          destinationFile
        )
      }
    }
  }

  @Nested
  inner class GetDestinationFile {
    @Test
    fun unitTestDestinationFileExists_returnsUnitTestFile() = runTest {
      val result = fileHandler.getDestinationFile()

      assertSame(destinationFile, result)
    }
  }

  @Nested
  inner class Dispose {
    @Test
    fun cancelsStorageObservingJob() {
      val job = mockk<Job>()

      every {
        job.cancel()
      } just Runs

      fileHandler.setStorageObservingJob(job)

      fileHandler.dispose()

      verify(exactly = 1) {
        job.cancel()
      }
    }

    @Test
    fun disposesFat32Checker() {
      every {
        fat32Checker.dispose()
      } just Runs

      fileHandler.dispose()

      verify(exactly = 1) {
        fat32Checker.dispose()
      }
    }
  }
}
