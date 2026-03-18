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

import android.app.Activity
import android.net.Uri
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.nav.destination.library.local.ProcessSelectedZimFilesForStandalone
import org.kiwix.kiwixmobile.nav.destination.library.local.SelectedZimFileCallback
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessSelectedZimFilesForStandaloneTest {
  private lateinit var processSelectedZimFiles: ProcessSelectedZimFilesForStandalone
  private val kiwixDataStore: KiwixDataStore = mockk(relaxed = true)
  private val activity: Activity = mockk(relaxed = true)
  private val selectedZimFileCallback: SelectedZimFileCallback = mockk(relaxed = true)

  private lateinit var testDispatcher: TestDispatcher
  private lateinit var testScope: TestScope

  @BeforeEach
  fun setup() {
    clearAllMocks()
    mockkStatic("org.kiwix.kiwixmobile.core.extensions.ContextExtensionsKt")
    mockkStatic(FileUtils::class)
    mockkStatic("org.kiwix.kiwixmobile.core.extensions.FileExtensionsKt")

    testDispatcher = StandardTestDispatcher()
    testScope = TestScope(testDispatcher)

    processSelectedZimFiles = ProcessSelectedZimFilesForStandalone(
      kiwixDataStore,
      activity
    )
    processSelectedZimFiles.setSelectedZimFileCallback(selectedZimFileCallback)
  }

  @Test
  fun `canHandleUris should return true when not play store build with android 11 or above`() =
    testScope.runTest {
      coEvery { kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove() } returns false

      val result = processSelectedZimFiles.canHandleUris()

      assertTrue(result)
    }

  @Test
  fun `canHandleUris should return false when play store build with android 11 or above`() =
    testScope.runTest {
      coEvery { kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove() } returns true

      val result = processSelectedZimFiles.canHandleUris()

      assertFalse(result)
    }

  @Test
  fun `processSelectedFiles should call navigateToReaderFragment for valid single file`() =
    testScope.runTest {
      val uri = createValidUri()

      coEvery { selectedZimFileCallback.navigateToReaderFragment(any()) } just Runs

      processSelectedZimFiles.processSelectedFiles(listOf(uri))

      coVerify { selectedZimFileCallback.navigateToReaderFragment(any()) }
    }

  @Test
  fun `processSelectedFiles should show toast when file is invalid`() = testScope.runTest {
    val uri = mockk<Uri>()
    val filePath = "/storage/emulated/0/test.jpg"

    coEvery { FileUtils.getLocalFilePathByUri(any(), uri) } returns filePath
    coEvery { any<File>().isFileExist() } returns true
    every { FileUtils.isValidZimFile(filePath) } returns false
    every { activity.getString(R.string.error_file_invalid, filePath) } returns "Invalid file"
    every { activity.toast(any<String>(), any()) } just Runs

    processSelectedZimFiles.processSelectedFiles(listOf(uri))

    verify { activity.toast(any<String>(), any()) }
  }

  @Test
  fun `processSelectedFiles should show toast when file not found`() = testScope.runTest {
    val uri = mockk<Uri>()

    coEvery { FileUtils.getLocalFilePathByUri(any(), uri) } returns null
    coEvery { any<File>().isFileExist() } returns false
    every { uri.toString() } returns "content://test"
    every {
      activity.getString(R.string.error_file_not_found, "content://test")
    } returns "File not found"
    every { activity.toast(any<String>(), any()) } just Runs

    processSelectedZimFiles.processSelectedFiles(listOf(uri))

    verify { activity.toast(any<String>(), any()) }
  }

  @Test
  fun `processSelectedFiles should add multiple valid files to library`() = testScope.runTest {
    val uri1 = createValidUri("content://test1", "/storage/emulated/0/test1.zim")
    val uri2 = createValidUri("content://test2", "/storage/emulated/0/test2.zim")

    every {
      activity.getString(R.string.your_selected_files_added_to_library)
    } returns "Files added"
    every { activity.toast(any<String>(), any()) } just Runs

    coEvery { selectedZimFileCallback.addBookToLibkiwixBookOnDisk(any()) } just Runs

    processSelectedZimFiles.processSelectedFiles(listOf(uri1, uri2))

    coVerify(exactly = 2) { selectedZimFileCallback.addBookToLibkiwixBookOnDisk(any()) }
    verify { activity.toast("Files added", any()) }
  }

  @Test
  fun `processSelectedFiles should show error dialog for invalid file in multiple selection`() =
    testScope.runTest {
      val invalidUri = createValidUri("content://invalid", "/storage/emulated/0/test.jpg")
      val validUri = createValidUri("content://valid", "/storage/emulated/0/test.zim")

      every { FileUtils.isValidZimFile("/storage/emulated/0/test.jpg") } returns false
      every {
        activity.getString(R.string.error_file_invalid, "/storage/emulated/0/test.jpg")
      } returns "Invalid file"

      coEvery {
        selectedZimFileCallback.showFileCopyMoveErrorDialog(any(), any())
      } just Runs

      processSelectedZimFiles.processSelectedFiles(listOf(invalidUri, validUri))

      coVerify {
        selectedZimFileCallback.showFileCopyMoveErrorDialog("Invalid file", any())
      }
    }

  @Test
  fun `processMultipleFiles should continue with next file after error dialog callback`() =
    testScope.runTest {
      val invalidUri = createValidUri("content://invalid", "/storage/emulated/0/test.jpg")
      val validUri = createValidUri("content://valid", "/storage/emulated/0/test.zim")
      val callbackSlot = slot<suspend () -> Unit>()

      every { FileUtils.isValidZimFile("/storage/emulated/0/test.jpg") } returns false
      every {
        activity.getString(R.string.error_file_invalid, "/storage/emulated/0/test.jpg")
      } returns "Invalid file"
      every {
        activity.getString(R.string.your_selected_files_added_to_library)
      } returns "Files added"
      every { activity.toast(any<String>(), any()) } just Runs

      every {
        selectedZimFileCallback.showFileCopyMoveErrorDialog(any(), capture(callbackSlot))
      } answers {
        // Simulate callback invocation
        runBlocking {
          callbackSlot.captured.invoke()
        }
      }
      coEvery { selectedZimFileCallback.addBookToLibkiwixBookOnDisk(any()) } just Runs

      processSelectedZimFiles.processSelectedFiles(listOf(invalidUri, validUri))

      coVerify { selectedZimFileCallback.addBookToLibkiwixBookOnDisk(any()) }
    }

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  private fun createValidUri(
    uriString: String = "content://test",
    filePath: String = "/storage/emulated/0/test.zim"
  ): Uri {
    val uri = mockk<Uri>()
    every { uri.toString() } returns uriString
    coEvery { FileUtils.getLocalFilePathByUri(any(), uri) } returns filePath
    coEvery { any<File>().isFileExist() } returns true
    every { FileUtils.isValidZimFile(filePath) } returns true
    return uri
  }
}
