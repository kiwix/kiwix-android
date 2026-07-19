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

import kotlinx.coroutines.ExperimentalCoroutinesApi

import android.app.Activity
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.documentfile.provider.DocumentFile

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.R.string
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.StorageDeviceProvider
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.nav.destination.library.CopyMoveFileHandler
import org.kiwix.kiwixmobile.nav.destination.library.StorageSelectDialogConfig
import org.kiwix.kiwixmobile.nav.destination.library.local.ProcessSelectedZimFilesForPlayStore
import org.kiwix.kiwixmobile.nav.destination.library.local.SelectedZimFileCallback
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessSelectedZimFilesForPlayStoreTest {
  private lateinit var processSelectedZimFiles: ProcessSelectedZimFilesForPlayStore
  private val kiwixDataStore: KiwixDataStore = mockk(relaxed = true)
  private val activity: Activity = mockk(relaxed = true)
  private val copyMoveFileHandler: CopyMoveFileHandler = mockk(relaxed = true)
  private val storageCalculator: StorageCalculator = mockk(relaxed = true)
  private val storageDeviceProvider: StorageDeviceProvider = mockk(relaxed = true)
  private val alertDialogShower: AlertDialogShower = mockk(relaxed = true)
  private val snackBarHostState: SnackbarHostState = mockk(relaxed = true)

  private val selectedZimFileCallback: SelectedZimFileCallback = mockk(relaxed = true)

  private lateinit var testScope: TestScope
  private val storagePath = "/storage/emulated/0/Android/media/org.kiwix.kiwixmobile"

  @BeforeEach
  fun setup() {
    testScope = TestScope()
    clearAllMocks()
    mockkStatic("org.kiwix.kiwixmobile.core.extensions.ContextExtensionsKt")
    mockkStatic(DocumentFile::class)
    mockkStatic(FileUtils::class)
    mockkObject(FileUtils)

    processSelectedZimFiles = ProcessSelectedZimFilesForPlayStore(
      kiwixDataStore,
      activity,
      copyMoveFileHandler,
      storageCalculator,
      storageDeviceProvider
    )

    processSelectedZimFiles.init(
      testScope,
      alertDialogShower,
      snackBarHostState,
      selectedZimFileCallback
    )

    every { kiwixDataStore.selectedStorage } returns flowOf(storagePath)
    every { kiwixDataStore.context } returns activity
  }

  @Test
  fun `canHandleUris should return true when play store build with android 11 or above`() =
    testScope.runTest {
      coEvery { kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove() } returns true

      val result = processSelectedZimFiles.canHandleUris()

      assertTrue(result)
    }

  @Test
  fun `canHandleUris should return false when not play store build with android 11 or above`() =
    testScope.runTest {
      coEvery { kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove() } returns false

      val result = processSelectedZimFiles.canHandleUris()

      assertFalse(result)
    }

  @Test
  fun `processSelectedFiles should show copy move dialog even if space is insufficient`() =
    testScope.runTest {
      val uri = mockk<Uri>()
      val documentFile = mockk<DocumentFile>()

      every { uri.scheme } returns "content"
      every { DocumentFile.fromSingleUri(any(), uri) } returns documentFile
      every { documentFile.length() } returns 1000L
      every { documentFile.name } returns "test.zim"
      every { FileUtils.isValidZimFile("test.zim") } returns true
      every { FileUtils.isSplittedZimFile("test.zim") } returns false

      coEvery { storageCalculator.availableBytes(any()) } returns 500L

      processSelectedZimFiles.processSelectedFiles(listOf(uri))
      advanceUntilIdle()

      coVerify {
        copyMoveFileHandler.showMoveFileToPublicDirectoryDialog(
          any(),
          uri,
          documentFile,
          false,
          null,
          true
        )
      }
    }

  @Test
  fun `insufficientSpaceInStorage and clicking change storage shows storage selection dialog`() =
    testScope.runTest {
      mockkStatic("org.kiwix.kiwixmobile.core.extensions.SnackbarHostStateExtensionKt")

      every { activity.getString(R.string.move_no_space) } returns "Not enough space"
      every { activity.getString(R.string.space_available) } returns "Available"
      every { activity.getString(R.string.change_storage) } returns "Change Storage"
      val actionClickSlot = slot<(() -> Unit)?>()
      every {
        snackBarHostState.snack(
          any(),
          any(),
          captureNullable(actionClickSlot),
          any(),
          any(),
          any()
        )
      } just Runs
      processSelectedZimFiles.insufficientSpaceInStorage(500L)
      advanceUntilIdle()
      verify(exactly = 0) {
        selectedZimFileCallback.showStorageSelectionDialog(any())
      }

      actionClickSlot.captured?.invoke()
      advanceUntilIdle()
      verify {
        selectedZimFileCallback.showStorageSelectionDialog(any())
      }
    }

  @Test
  fun `filesystemDoesNotSupportedCopyMoveFilesOver4GB shows snackbar`() {
    mockkStatic("org.kiwix.kiwixmobile.core.extensions.SnackbarHostStateExtensionKt")

    every {
      activity.getString(string.file_system_does_not_support_4gb)
    } returns "File system does not support files over 4GB"

    every {
      snackBarHostState.snack(
        message = "File system does not support files over 4GB",
        actionLabel = any(),
        actionClick = any(),
        snackbarDuration = any(),
        lifecycleScope = any(),
        snackBarResult = any()
      )
    } just Runs

    processSelectedZimFiles.filesystemDoesNotSupportedCopyMoveFilesOver4GB()

    verify {
      snackBarHostState.snack(
        message = "File system does not support files over 4GB",
        actionLabel = any(),
        actionClick = any(),
        snackbarDuration = any(),
        lifecycleScope = any(),
        snackBarResult = any()
      )
    }
  }

  @Test
  fun `processSelectedFiles should process single file when there is sufficient storage`() =
    testScope.runTest {
      val uri = createValidUri()
      val documentFile = DocumentFile.fromSingleUri(activity, uri)!!

      processSelectedZimFiles.processSelectedFiles(listOf(uri))
      advanceUntilIdle()
      coVerify {
        copyMoveFileHandler.showMoveFileToPublicDirectoryDialog(
          any(),
          uri,
          documentFile,
          false,
          null,
          true
        )
      }
    }

  @Test
  fun `processSingleFile should show toast for invalid file when single selection`() =
    testScope.runTest {
      val uri = mockk<Uri>()
      val documentFile = mockk<DocumentFile>()

      every { uri.scheme } returns "content"
      every { DocumentFile.fromSingleUri(any(), uri) } returns documentFile
      every { documentFile.length() } returns 100L
      every { documentFile.name } returns "test.jpg"

      coEvery { storageCalculator.availableBytes(any()) } returns 1000L
      every { activity.getString(R.string.error_file_invalid, "test.jpg") } returns "Invalid file"
      every { activity.toast(any<String>(), any()) } just Runs

      processSelectedZimFiles.processSelectedFiles(listOf(uri))
      advanceUntilIdle()
      verify { activity.toast("Invalid file", any()) }
    }

  @Test
  fun `processMultipleFiles should show success toast when all files processed`() =
    testScope.runTest {
      val uri1 = createValidUri("test1.zim", availableSpace = 10000L)
      val uri2 = createValidUri("test2.zim", availableSpace = 10000L)

      coEvery {
        copyMoveFileHandler.showMoveFileToPublicDirectoryDialog(
          any(),
          any(),
          any(),
          any(),
          any(),
          any()
        )
      } coAnswers {
        processSelectedZimFiles.onFileCopied(mockk<File>(relaxed = true))
      }

      every {
        activity.getString(R.string.your_selected_files_added_to_library)
      } returns "Files added to library"
      every { activity.toast(any<String>(), any()) } just Runs

      processSelectedZimFiles.processSelectedFiles(listOf(uri1, uri2))
      advanceUntilIdle()

      verify { activity.toast("Files added to library", any()) }
    }

  @Test
  fun `isValidZimFile should return true for valid zim extension`() = testScope.runTest {
    val uri = createValidUri()
    val documentFile = DocumentFile.fromSingleUri(activity, uri)!!

    processSelectedZimFiles.processSelectedFiles(listOf(uri))
    advanceUntilIdle()
    coVerify {
      copyMoveFileHandler.showMoveFileToPublicDirectoryDialog(
        any(),
        uri,
        documentFile,
        false,
        null,
        true
      )
    }
  }

  @Test
  fun `isValidZimFile should return true for split zim extension`() = testScope.runTest {
    val uri = createValidUri(fileName = "test.zimaa")
    val documentFile = DocumentFile.fromSingleUri(activity, uri)!!

    processSelectedZimFiles.processSelectedFiles(listOf(uri))
    advanceUntilIdle()
    coVerify {
      copyMoveFileHandler.showMoveFileToPublicDirectoryDialog(
        any(),
        uri,
        documentFile,
        false,
        null,
        true
      )
    }
  }

  @Test
  fun `onFileCopied should navigate to reader for single file`() = testScope.runTest {
    val uri = createValidUri()
    val file = mockk<File>()
    every { file.path } returns "/storage/test.zim"

    processSelectedZimFiles.processSelectedFiles(listOf(uri))

    processSelectedZimFiles.onFileCopied(file)

    verify { selectedZimFileCallback.navigateToReaderScreen(file) }
  }

  @Test
  fun `onFileMoved should navigate to reader for single file`() = testScope.runTest {
    val uri = createValidUri()
    val file = mockk<File>()
    every { file.path } returns "/storage/test.zim"

    processSelectedZimFiles.processSelectedFiles(listOf(uri))
    processSelectedZimFiles.onFileMoved(file)

    verify { selectedZimFileCallback.navigateToReaderScreen(file) }
  }

  @Test
  fun `onError should show toast for single file selection`() = testScope.runTest {
    val uri = createValidUri()
    val errorMessage = "Error copying file"
    every { activity.toast(any<String>(), any()) } just Runs

    processSelectedZimFiles.processSelectedFiles(listOf(uri))

    processSelectedZimFiles.onError(errorMessage)

    verify { activity.toast(errorMessage, any()) }
  }

  @Test
  fun `onError should show error dialog for multiple file selection`() = testScope.runTest {
    val uri1 = createValidUri("test1.zim")
    val uri2 = createValidUri("test2.zim")
    val errorMessage = "Error copying file"

    coEvery {
      selectedZimFileCallback.showFileCopyMoveErrorDialog(any(), any())
    } just Runs

    processSelectedZimFiles.processSelectedFiles(listOf(uri1, uri2), isAfterRetry = true)

    processSelectedZimFiles.onError(errorMessage)

    coVerify {
      selectedZimFileCallback.showFileCopyMoveErrorDialog(errorMessage, any())
    }
  }

  @Test
  fun `showStorageSelectionDialog delegates to selectedZimFileCallback`() {
    val dialogConfig = mockk<StorageSelectDialogConfig>()

    processSelectedZimFiles.showStorageSelectionDialog(dialogConfig)

    verify {
      selectedZimFileCallback.showStorageSelectionDialog(dialogConfig)
    }
  }

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `dispose should clean up resources`() {
    processSelectedZimFiles.dispose()

    verify { copyMoveFileHandler.dispose() }
  }

  @Test
  fun `processSelectedFiles should open file directly when already in app directory`() =
    testScope.runTest {
      // Create a real temp directory simulating the app's public directory
      val appDir = File(System.getProperty("java.io.tmpdir"), "kiwix_test_${System.nanoTime()}")
      appDir.mkdirs()
      val testFile = File(appDir, "test.zim")
      testFile.writeBytes(ByteArray(100))

      try {
        // Point selectedStorage to the temp directory
        every { kiwixDataStore.selectedStorage } returns flowOf(appDir.absolutePath)

        val uri = createValidUri(fileSize = 100L)

        processSelectedZimFiles.processSelectedFiles(listOf(uri))
        advanceUntilIdle()

        // Should navigate directly to reader, NOT show copy/move dialog
        verify { selectedZimFileCallback.navigateToReaderScreen(any()) }
        coVerify(exactly = 0) {
          copyMoveFileHandler.showMoveFileToPublicDirectoryDialog(
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
          )
        }
      } finally {
        testFile.delete()
        appDir.delete()
      }
    }

  @Test
  fun `processSelectedFiles should skip space check when file already in app directory`() =
    testScope.runTest {
      // Create a real temp directory with a file of size 1000 bytes
      val appDir = File(System.getProperty("java.io.tmpdir"), "kiwix_test_space_${System.nanoTime()}")
      appDir.mkdirs()
      val testFile = File(appDir, "test.zim")
      testFile.writeBytes(ByteArray(1000))

      try {
        every { kiwixDataStore.selectedStorage } returns flowOf(appDir.absolutePath)
        // File size = 1000, available space = 500 (insufficient for copy)
        // But since file is already in app dir, space check should be skipped
        val uri = createValidUri(fileSize = 1000L, availableSpace = 500L)

        processSelectedZimFiles.processSelectedFiles(listOf(uri))
        advanceUntilIdle()

        // Should open directly, not show "not enough space" error
        verify { selectedZimFileCallback.navigateToReaderScreen(any()) }
      } finally {
        testFile.delete()
        appDir.delete()
      }
    }

  @Test
  fun `getExistingFileInAppDirectory returns null when file not in app directory`() =
    testScope.runTest {
      val documentFile = mockk<DocumentFile>()
      every { documentFile.name } returns "nonexistent.zim"
      every { documentFile.length() } returns 100L

      val result = processSelectedZimFiles.getExistingFileInAppDirectory(documentFile)

      assertNull(result)
    }

  @Test
  fun `getExistingFileInAppDirectory returns null when sizes differ`() =
    testScope.runTest {
      val appDir = File(System.getProperty("java.io.tmpdir"), "kiwix_test_diff_${System.nanoTime()}")
      appDir.mkdirs()
      val testFile = File(appDir, "test.zim")
      testFile.writeBytes(ByteArray(100))

      try {
        every { kiwixDataStore.selectedStorage } returns flowOf(appDir.absolutePath)

        val documentFile = mockk<DocumentFile>()
        every { documentFile.name } returns "test.zim"
        every { documentFile.length() } returns 200L // Different size than actual file

        val result = processSelectedZimFiles.getExistingFileInAppDirectory(documentFile)

        assertNull(result)
      } finally {
        testFile.delete()
        appDir.delete()
      }
    }

  @Test
  fun `getExistingFileInAppDirectory returns null for null documentFile`() =
    testScope.runTest {
      val result = processSelectedZimFiles.getExistingFileInAppDirectory(null)
      assertNull(result)
    }

  @Test
  fun `getExistingFileInAppDirectory returns file when file exists with matching size`() =
    testScope.runTest {
      val appDir = File(System.getProperty("java.io.tmpdir"), "kiwix_test_match_${System.nanoTime()}")
      appDir.mkdirs()
      val testFile = File(appDir, "test.zim")
      testFile.writeBytes(ByteArray(100))

      try {
        every { kiwixDataStore.selectedStorage } returns flowOf(appDir.absolutePath)

        val documentFile = mockk<DocumentFile>()
        every { documentFile.name } returns "test.zim"
        every { documentFile.length() } returns 100L // Same size as actual file

        val result = processSelectedZimFiles.getExistingFileInAppDirectory(documentFile)

        assertNotNull(result)
        assertTrue(result!!.name == "test.zim")
      } finally {
        testFile.delete()
        appDir.delete()
      }
    }

  private fun createValidUri(
    fileName: String = "test.zim",
    fileSize: Long = 100L,
    availableSpace: Long = 1000L
  ): Uri {
    val uri = mockk<Uri>()
    val documentFile = mockk<DocumentFile>()
    every { uri.scheme } returns "content"
    every { DocumentFile.fromSingleUri(any(), uri) } returns documentFile
    every { documentFile.length() } returns fileSize
    every { documentFile.name } returns fileName
    coEvery { storageCalculator.availableBytes(any()) } returns availableSpace
    return uri
  }
}
