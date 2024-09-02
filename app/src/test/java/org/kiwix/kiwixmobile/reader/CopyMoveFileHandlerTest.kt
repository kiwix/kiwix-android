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

package org.kiwix.kiwixmobile.reader

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.nav.destination.reader.CopyMoveFileHandler
import org.kiwix.kiwixmobile.nav.destination.reader.CopyMoveFileHandler.FileCopyMoveCallback
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.DetectingFileSystem
import java.io.File
import java.io.FileDescriptor
import java.io.FileNotFoundException

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
  private val progressBarDialog: AlertDialog = mockk(relaxed = true)
  private val destinationFile: File = mockk(relaxed = true)
  private val parcelFileDescriptor: ParcelFileDescriptor = mockk(relaxed = true)
  private val storageFile: File = mockk(relaxed = true)
  private val selectedFile: File = mockk(relaxed = true)
  private val storagePath = "storage/0/emulated/Android/media/org.kiwix.kiwixmobile"

  @BeforeEach
  fun setup() {
    fileHandler = CopyMoveFileHandler(
      activity,
      sharedPreferenceUtil,
      alertDialogShower,
      storageCalculator,
      fat32Checker
    ).apply {
      setSelectedFileAndUri(null, selectedFile)
      lifecycleScope = testScope
      this.fileCopyMoveCallback = this@CopyMoveFileHandlerTest.fileCopyMoveCallback
    }
  }

  @Test
  fun validateZimFileCanCopyOrMoveShouldReturnTrueWhenSufficientSpaceAndValidFileSystem() {
    every { storageFile.exists() } returns true
    every { storageFile.freeSpace } returns 1000L
    every { storageFile.path } returns storagePath
    every { selectedFile.length() } returns 100L
    every { storageCalculator.availableBytes(storageFile) } returns 1000L
    every { fat32Checker.fileSystemStates.value } returns CanWrite4GbFile

    val result = fileHandler.validateZimFileCanCopyOrMove(storageFile)

    assertTrue(result)
    // check insufficientSpaceInStorage callback should not call.
    verify(exactly = 0) { fileCopyMoveCallback.insufficientSpaceInStorage(any()) }
  }

  @Test
  fun validateZimFileCanCopyOrMoveShouldReturnFalseAndCallCallbackWhenInsufficientSpace() {
    every { selectedFile.length() } returns 2000L
    every { storageFile.exists() } returns true
    every { storageFile.freeSpace } returns 1000L
    every { storageFile.path } returns storagePath
    every { storageCalculator.availableBytes(storageFile) } returns 1000L
    every { fat32Checker.fileSystemStates.value } returns CanWrite4GbFile

    val result = fileHandler.validateZimFileCanCopyOrMove()

    assertFalse(result)
    verify { fileCopyMoveCallback.insufficientSpaceInStorage(any()) }
  }

  @Test
  fun validateZimFileCanCopyOrMoveShouldReturnFalseWhenDetectingAndCanNotWrite4GBFiles() {
    every { selectedFile.length() } returns 1000L
    every { storageFile.exists() } returns true
    every { storageFile.freeSpace } returns 2000L
    every { storageFile.path } returns storagePath
    every { storageCalculator.availableBytes(storageFile) } returns 2000L
    every { fat32Checker.fileSystemStates.value } returns DetectingFileSystem

    // check when detecting the fileSystem
    assertFalse(fileHandler.validateZimFileCanCopyOrMove())

    every { fat32Checker.fileSystemStates.value } returns CannotWrite4GbFile

    // check when Can not write 4GB files on the fileSystem
    assertFalse(fileHandler.validateZimFileCanCopyOrMove())
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
  fun showProgressDialogShouldDisplayProgressDialog() {
    val progressBar: ProgressBar = mockk(relaxed = true)
    val progressTextView: TextView = mockk(relaxed = true)
    val inflatedView: View = mockk()
    val alertDialogBuilder: AlertDialog.Builder = mockk(relaxed = true)

    every {
      activity.layoutInflater.inflate(
        R.layout.copy_move_progress_bar,
        null
      )
    } returns inflatedView
    every { inflatedView.findViewById<ProgressBar>(R.id.progressBar) } returns progressBar
    every { inflatedView.findViewById<TextView>(R.id.progressTextView) } returns progressTextView

    every { AlertDialog.Builder(activity) } returns alertDialogBuilder
    every { alertDialogBuilder.setTitle(any<String>()) } returns alertDialogBuilder
    every { alertDialogBuilder.setView(inflatedView) } returns alertDialogBuilder
    every { alertDialogBuilder.setCancelable(any()) } returns alertDialogBuilder
    every { alertDialogBuilder.create() } returns progressBarDialog
    every { progressBarDialog.show() } just Runs

    fileHandler.showProgressDialog()

    assertTrue(fileHandler.progressBarDialog?.isShowing == true)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun copyZimFileToPublicAppDirectory() = testScope.runTest {
    val sourceUri: Uri = mockk()
    val mockFileDescriptor = mockk<FileDescriptor>(relaxed = true)
    val contentResolver: ContentResolver = mockk()
    every { activity.contentResolver } returns contentResolver
    every {
      contentResolver.openFileDescriptor(
        sourceUri,
        "r"
      )
    } returns parcelFileDescriptor
    every { parcelFileDescriptor.fileDescriptor } returns mockFileDescriptor
    every { destinationFile.createNewFile() } returns true
    every { destinationFile.name } returns "demo.zim"
    every { sharedPreferenceUtil.prefStorage } returns storagePath
    fileHandler = spyk(fileHandler)
    every { fileHandler.getDestinationFile() } returns destinationFile

    // test when selected file is not found
    fileHandler.setSelectedFileAndUri(null, null)
    fileHandler.copyZimFileToPublicAppDirectory()
    verify { fileCopyMoveCallback.onError(any()) }
    verify { destinationFile.delete() }

    // test when selected file found
    fileHandler.setSelectedFileAndUri(sourceUri, selectedFile)
    fileHandler.copyZimFileToPublicAppDirectory()
    verify { fileCopyMoveCallback.onFileCopied(destinationFile) }

    // test when there is an error in copying file
    coEvery {
      fileHandler.copyFile(
        sourceUri,
        destinationFile
      )
    } throws FileNotFoundException("Test Exception")

    fileHandler.copyZimFileToPublicAppDirectory()

    advanceUntilIdle()

    verify(exactly = 0) { fileCopyMoveCallback.onFileCopied(destinationFile) }
    verify { fileCopyMoveCallback.onError(any()) }
  }

  @Test
  fun getDestinationFile() {
    val fileName = "test.txt"
    val rootFile: File = mockk(relaxed = true)
    val newFile: File = mockk(relaxed = true)
    every { newFile.name } returns fileName
    every { rootFile.path } returns storagePath

    every { selectedFile.name } returns fileName
    every { File(rootFile, fileName).exists() } returns false
    every { File(rootFile, fileName).createNewFile() } returns true
    fileHandler = spyk(fileHandler)
    every { fileHandler.getDestinationFile(rootFile) } returns newFile

    // Run the test
    val resultFile = fileHandler.getDestinationFile(rootFile)

    assertEquals(newFile, resultFile)
    verify { File(rootFile, fileName).createNewFile() }
  }
}
