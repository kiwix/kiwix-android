/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.nav.destination.library.local

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.LibkiwixBookFactory
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.StorageSelectDialogConfig
import org.kiwix.libkiwix.Book
import org.kiwix.libzim.Archive
import org.kiwix.sharedFunctions.MainDispatcherRule
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ExternalZimIntentHandlerTest {
  @RegisterExtension
  private val mainDispatcherRule = MainDispatcherRule()

  private val testDispatcher get() = mainDispatcherRule.dispatcher

  private val repositoryActions: MainRepositoryActions = mockk(relaxed = true)
  private val zimReaderFactory: ZimFileReader.Factory = mockk(relaxed = true)
  private val libkiwixBookFactory: LibkiwixBookFactory = mockk(relaxed = true)
  private val processSelectedZimFilesForStandalone: ProcessSelectedZimFilesForStandalone =
    mockk(relaxed = true)
  private val processSelectedZimFilesForPlayStore: ProcessSelectedZimFilesForPlayStore =
    mockk(relaxed = true)
  private val kiwixPermissionChecker: KiwixPermissionChecker = mockk(relaxed = true)

  private lateinit var handler: ExternalZimIntentHandler
  private val activity: KiwixMainActivity = mockk(relaxed = true)
  private val intent: Intent = mockk(relaxed = true)
  private val uri: Uri = mockk(relaxed = true)

  @BeforeEach
  fun setUp() {
    val coreApp = mockk<CoreApp>(relaxed = true)
    CoreApp.instance = coreApp

    mockkStatic(Toast::class)
    val toastMock = mockk<Toast>(relaxed = true)
    every { Toast.makeText(any(), any<Int>(), any()) } returns toastMock
    every { Toast.makeText(any(), any<CharSequence>(), any()) } returns toastMock

    every { intent.data } returns uri
    every { uri.toString() } returns "content://test.zim"

    coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns true
    coEvery { kiwixPermissionChecker.isManageExternalStoragePermissionGranted() } returns true
    every { kiwixPermissionChecker.isAndroid11OrAbove() } returns true

    coEvery { processSelectedZimFilesForStandalone.canHandleUris() } returns true
    coEvery { processSelectedZimFilesForPlayStore.canHandleUris() } returns false

    handler = ExternalZimIntentHandler(
      repositoryActions,
      zimReaderFactory,
      libkiwixBookFactory,
      processSelectedZimFilesForStandalone,
      processSelectedZimFilesForPlayStore,
      kiwixPermissionChecker,
      testDispatcher,
      Dispatchers.Main
    )
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun `init wires up both process handlers`() = runTest {
    handler.init(activity, this)

    verify { processSelectedZimFilesForStandalone.init(handler) }
    verify {
      processSelectedZimFilesForPlayStore.init(
        this@runTest,
        activity.alertDialogShower,
        activity.snackBarHostState,
        handler
      )
    }
  }

  @Test
  fun `handleIntent with all permissions granted imports via standalone handler`() = runTest {
    handler.init(activity, this)

    handler.handleIntent(intent)
    advanceUntilIdle()

    coVerify { processSelectedZimFilesForStandalone.processSelectedFiles(listOf(uri)) }
    coVerify(exactly = 0) { processSelectedZimFilesForPlayStore.processSelectedFiles(any()) }
    verify { activity.clearIntentDataAndAction() }
  }

  @Test
  fun `handleIntent with all permissions granted imports via playstore handler`() = runTest {
    coEvery { processSelectedZimFilesForStandalone.canHandleUris() } returns false
    coEvery { processSelectedZimFilesForPlayStore.canHandleUris() } returns true
    handler.init(activity, this)

    handler.handleIntent(intent)
    advanceUntilIdle()

    coVerify { processSelectedZimFilesForPlayStore.processSelectedFiles(listOf(uri)) }
    coVerify(exactly = 0) { processSelectedZimFilesForStandalone.processSelectedFiles(any()) }
    verify { activity.clearIntentDataAndAction() }
  }

  @Test
  fun `handleIntent requests write permission before checking manage storage permission`() =
    runTest {
      coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns false
      handler.init(activity, this)
      var emitted = false
      val collectJob = launch { handler.requestReadWritePermission.collect { emitted = true } }

      handler.handleIntent(intent)
      advanceUntilIdle()

      assertThat(emitted).isTrue()
      coVerify(exactly = 0) { kiwixPermissionChecker.isManageExternalStoragePermissionGranted() }
      coVerify(exactly = 0) { processSelectedZimFilesForStandalone.processSelectedFiles(any()) }
      verify { activity.clearIntentDataAndAction() }
      collectJob.cancel()
    }

  @Test
  fun `handleIntent with manage storage permission missing does not proceed`() = runTest {
    coEvery { kiwixPermissionChecker.isManageExternalStoragePermissionGranted() } returns false
    handler.init(activity, this)

    handler.handleIntent(intent)
    advanceUntilIdle()

    coVerify(exactly = 0) { processSelectedZimFilesForStandalone.processSelectedFiles(any()) }
    verify { activity.clearIntentDataAndAction() }
  }

  @Test
  fun `handlePendingUri does nothing when there is no pending uri`() = runTest {
    handler.init(activity, this)

    handler.handlePendingUri()
    advanceUntilIdle()

    coVerify(exactly = 0) { processSelectedZimFilesForStandalone.processSelectedFiles(any()) }
  }

  @Test
  fun `handlePendingUri resumes opening once write permission is granted`() = runTest {
    coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns false
    handler.init(activity, this)

    handler.handleIntent(intent)
    advanceUntilIdle()
    coVerify(exactly = 0) { processSelectedZimFilesForStandalone.processSelectedFiles(any()) }

    coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns true
    handler.handlePendingUri()
    advanceUntilIdle()

    coVerify { processSelectedZimFilesForStandalone.processSelectedFiles(listOf(uri)) }
  }

  @Test
  fun `onReadWriteRationalPermission shows read permission required dialog`() = runTest {
    handler.init(activity, this)

    handler.onReadWriteRationalPermission()

    verify { activity.alertDialogShower.show(KiwixDialog.ReadPermissionRequired, any()) }
  }

  @Test
  fun `handlePendingUri resumes opening once manage storage permission is granted`() = runTest {
    coEvery { kiwixPermissionChecker.isManageExternalStoragePermissionGranted() } returns false
    handler.init(activity, this)

    handler.handleIntent(intent)
    advanceUntilIdle()
    coVerify(exactly = 0) { processSelectedZimFilesForStandalone.processSelectedFiles(any()) }

    coEvery { kiwixPermissionChecker.isManageExternalStoragePermissionGranted() } returns true
    handler.handlePendingUri()
    advanceUntilIdle()

    coVerify { processSelectedZimFilesForStandalone.processSelectedFiles(listOf(uri)) }
  }

  @Test
  fun `navigateToReaderScreen opens the reader and saves the book`() = runTest {
    val file = File("/storage/test.zim")
    val zimFileReader: ZimFileReader = mockk(relaxed = true)
    val archive: Archive = mockk(relaxed = true)
    val book = BookTestWrapper()
    every { zimFileReader.jniKiwixReader } returns archive
    coEvery { zimReaderFactory.create(ZimReaderSource(file), false) } returns zimFileReader
    every { libkiwixBookFactory.create() } returns book
    handler.init(activity, this)

    handler.navigateToReaderScreen(file)
    advanceUntilIdle()

    verify { activity.openZimFromFilePath(file.path) }
    coVerify { zimReaderFactory.create(ZimReaderSource(file), false) }
    coVerify { repositoryActions.saveBook(book) }
    verify { zimFileReader.dispose() }
  }

  @Test
  fun `addBookToLibkiwixBookOnDisk creates a zim reader and saves the book`() = runTest {
    val file = File("/storage/test.zim")
    val zimFileReader: ZimFileReader = mockk(relaxed = true)
    val archive: Archive = mockk(relaxed = true)
    val book = BookTestWrapper()
    every { zimFileReader.jniKiwixReader } returns archive
    coEvery { zimReaderFactory.create(ZimReaderSource(file), false) } returns zimFileReader
    every { libkiwixBookFactory.create() } returns book
    handler.init(activity, this)

    handler.addBookToLibkiwixBookOnDisk(file)
    advanceUntilIdle()

    coVerify { zimReaderFactory.create(ZimReaderSource(file), false) }
    coVerify { repositoryActions.saveBook(book) }
    verify { zimFileReader.dispose() }
  }

  @Test
  fun `showFileCopyMoveErrorDialog shows the file copy move error dialog`() = runTest {
    handler.init(activity, this)

    handler.showFileCopyMoveErrorDialog("some error") {}
    advanceUntilIdle()

    verify { activity.alertDialogShower.show(KiwixDialog.FileCopyMoveError("some error"), any()) }
  }

  @Test
  fun `showStorageSelectionDialog shows the storage selection dialog`() = runTest {
    handler.init(activity, this)
    val dialogConfig: StorageSelectDialogConfig = mockk(relaxed = true)

    handler.showStorageSelectionDialog(dialogConfig)
    advanceUntilIdle()

    verify { activity.alertDialogShower.show(any()) }
  }
}

/**
 * `Book()` allocates a native handle, which crashes in a plain JVM unit test. The `Book(handle)`
 * constructor skips that, and overriding [update] avoids the other native call this handler makes.
 * See `BookTestWrapper` in ZimHostViewModelTest/StorageObserverTest for the same pattern.
 */
private class BookTestWrapper : Book(0L) {
  override fun update(archive: Archive?) {
    // no-op to avoid the native call
  }
}
