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

package org.kiwix.kiwixmobile.zimManager.fileselectView.effects

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.sharedFunctions.MainDispatcherRule
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteFilesTest {
  private var file1 = File("/storage/kiwix.zim")
  private val libkiwixBook1 = LibkiwixBook(_id = "book-id-1", _title = "Book 1", file = file1)
  private val book1 = BookOnDisk(book = libkiwixBook1, zimReaderSource = ZimReaderSource(file1))

  private var file2 = File("/storage/test.zim")
  private val libkiwixBook2 = LibkiwixBook(_id = "book-id-2", _title = "Book 2", file = file2)
  private val book2 = BookOnDisk(book = libkiwixBook2, zimReaderSource = ZimReaderSource(file2))
  private val booksOnDiskListItems: List<BookOnDisk> = listOf(book1, book2)
  private val dialogShower: DialogShower = mockk()
  private val deleteFilesUseCase: DeleteFilesUseCase = mockk()

  private val activity: CoreMainActivity = mockk()

  private lateinit var deleteFiles: DeleteFiles

  @RegisterExtension
  @JvmField
  val mainDispatcherRule = MainDispatcherRule()

  private val viewModelScope = CoroutineScope(mainDispatcherRule.dispatcher)

  @BeforeEach
  fun setup() {
    clearAllMocks()
    mockkStatic("org.kiwix.kiwixmobile.core.extensions.ContextExtensionsKt")

    deleteFiles =
      DeleteFiles(
        booksOnDiskListItems,
        dialogShower,
        deleteFilesUseCase,
        viewModelScope,
        mainDispatcherRule.dispatcher
      )
    every { activity.toast(any<Int>()) } just Runs
  }

  @AfterEach
  fun cleanup() {
    unmockkAll()
  }

  @Test
  fun invokeWith_showsDeleteDialogWithBookTitles() {
    deleteFiles.invokeWith(activity)

    verify {
      dialogShower.show(
        KiwixDialog.DeleteZims("Book 1\nBook 2"),
        any()
      )
    }
  }

  @Test
  fun invokeWith_whenDeleteClicked_callsDeleteFilesUseCase() = runTest {
    val clickSlot = slot<() -> Unit>()

    every {
      dialogShower.show(any(), capture(clickSlot))
    } just Runs

    coEvery { deleteFilesUseCase(any()) } returns true

    deleteFiles.invokeWith(activity)

    clickSlot.captured.invoke()
    advanceUntilIdle()
    coVerify(exactly = 1) {
      deleteFilesUseCase(listOf(book1, book2))
    }
  }

  @Test
  fun invokeWith_whenDeleteSucceeds_showsSuccessToast() = runTest {
    val clickSlot = slot<() -> Unit>()

    every {
      dialogShower.show(any(), capture(clickSlot))
    } just Runs

    coEvery {
      deleteFilesUseCase(any())
    } returns true

    deleteFiles.invokeWith(activity)

    clickSlot.captured.invoke()
    advanceUntilIdle()
    verify {
      activity.toast(R.string.delete_zims_toast)
    }
  }

  @Test
  fun invokeWith_whenDeleteFails_showsFailureToast() = runTest {
    val clickSlot = slot<() -> Unit>()

    every {
      dialogShower.show(any(), capture(clickSlot))
    } just Runs

    coEvery {
      deleteFilesUseCase(any())
    } returns false

    deleteFiles.invokeWith(activity)

    clickSlot.captured.invoke()

    advanceUntilIdle()

    verify {
      activity.toast(R.string.delete_zim_failed)
    }
  }
}
