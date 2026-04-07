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

package org.kiwix.kiwixmobile.utils

import android.app.Activity
import android.net.Uri
import androidx.core.content.FileProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.UnsupportedMimeTypeHandler
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.utils.files.SaveResult
import org.kiwix.sharedFunctions.MainDispatcherRule
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class UnsupportedMimeTypeHandlerTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val activity: Activity = mockk(relaxed = true)
  private val zimReaderContainer: ZimReaderContainer = mockk(relaxed = true)
  private val alertDialogShower: AlertDialogShower = mockk(relaxed = true)

  private lateinit var handler: UnsupportedMimeTypeHandler

  private val testUrl = "content://kiwix.pdf"
  private val testMime = "application/pdf"

  private val openAction = slot<() -> Unit>()
  private val saveAction = slot<() -> Unit>()

  @Before
  fun setup() {
    mockkObject(FileUtils)

    mockkStatic("org.kiwix.kiwixmobile.core.extensions.ContextExtensionsKt")
    mockkStatic("org.kiwix.kiwixmobile.core.extensions.FileExtensionsKt")
    mockkStatic(FileProvider::class)
    every { activity.toast(any<Int>()) } returns Unit
    every { activity.toast(any<String>()) } returns Unit
    every { FileProvider.getUriForFile(any(), any(), any()) } returns mockk()
    coEvery { any<File>().isFileExist() } returns true

    handler = UnsupportedMimeTypeHandler(activity, zimReaderContainer)
    handler.setAlertDialogShower(alertDialogShower)
    handler.intent = mockk(relaxed = true)
  }

  @After
  fun tearDown() = unmockkAll()

  private fun TestScope.showDialogWith(result: SaveResult) {
    coEvery {
      FileUtils.downloadFileFromUrl(any(), any(), any(), any())
    } returns result

    handler.showSaveOrOpenUnsupportedFilesDialog(testUrl, testMime, this)

    verify {
      alertDialogShower.show(any(), capture(openAction), capture(saveAction), any())
    }
  }

  @Test
  fun openFile_whenFileSaved_andReaderAvailable_opensExternalApp() = runTest {
    val savedFile = mockk<File>()

    showDialogWith(SaveResult.FileSaved(savedFile))
    every { handler.intent.resolveActivity(any()) } returns mockk()

    openAction.captured.invoke()
    advanceUntilIdle()

    verify { activity.startActivity(handler.intent) }
  }

  @Test
  fun openFile_whenFileSaved_andNoReaderInstalled_showsToast() = runTest {
    val savedFile = mockk<File>()

    showDialogWith(SaveResult.FileSaved(savedFile))
    every { handler.intent.resolveActivity(any()) } returns null

    openAction.captured.invoke()
    advanceUntilIdle()

    verify { activity.toast(any<Int>()) }
  }

  @Test
  fun openFile_whenMediaSaved_andReaderAvailable_opensExternalApp() = runTest {
    val mediaUri = mockk<Uri>()

    showDialogWith(SaveResult.MediaSaved(mediaUri, "demo.pdf"))
    every { handler.intent.resolveActivity(any()) } returns mockk()

    openAction.captured.invoke()
    advanceUntilIdle()

    verify { activity.startActivity(handler.intent) }
  }

  @Test
  fun openFile_whenMediaSaved_andNoReaderInstalled_showsToast() = runTest {
    val mediaUri = mockk<Uri>()

    showDialogWith(SaveResult.MediaSaved(mediaUri, "demo.pdf"))
    every { handler.intent.resolveActivity(any()) } returns null

    openAction.captured.invoke()
    advanceUntilIdle()

    verify { activity.toast(any<Int>()) }
  }

  @Test
  fun openFile_whenInvalidSource_showsErrorToast() = runTest {
    showDialogWith(SaveResult.InvalidSource)

    openAction.captured.invoke()
    advanceUntilIdle()

    verify { activity.toast(any<Int>()) }
  }

  @Test
  fun openFile_whenDownloadFails_showsErrorToast() = runTest {
    showDialogWith(SaveResult.Error("error", Throwable()))

    openAction.captured.invoke()
    advanceUntilIdle()

    verify { activity.toast(any<Int>()) }
  }

  @Test
  fun saveFile_whenFileSaved_showsSuccessMessageWithPath() = runTest {
    val savedFile = mockk<File> {
      every { absolutePath } returns "Kiwix.jpg"
    }

    every {
      activity.getString(R.string.save_media_saved, "Kiwix.jpg")
    } returns "Kiwix.jpg"

    showDialogWith(SaveResult.FileSaved(savedFile))

    saveAction.captured.invoke()
    advanceUntilIdle()

    verify { activity.toast("Kiwix.jpg") }
  }

  @Test
  fun saveFile_whenMediaSaved_showsSuccessMessageWithName() = runTest {
    val displayName = "demo.pdf"

    every {
      activity.getString(R.string.save_media_saved, displayName)
    } returns "Saved demo.pdf"

    showDialogWith(SaveResult.MediaSaved(mockk(), displayName))

    saveAction.captured.invoke()
    advanceUntilIdle()

    verify { activity.toast("Saved demo.pdf") }
  }

  @Test
  fun saveFile_whenInvalidSource_showsErrorToast() = runTest {
    showDialogWith(SaveResult.InvalidSource)

    saveAction.captured.invoke()
    advanceUntilIdle()

    verify { activity.toast(any<Int>()) }
  }

  @Test
  fun saveFile_whenDownloadFails_showsErrorToast() = runTest {
    showDialogWith(SaveResult.Error("error", Throwable()))

    saveAction.captured.invoke()
    advanceUntilIdle()

    verify { activity.toast(any<Int>()) }
  }
}
