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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ValidateZIMFilesTest {
  @RegisterExtension
  @JvmField
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun `invokeWith should show confirmation dialog with joined titles`() {
    val activity = mockk<KiwixMainActivity>(relaxed = true)
    val dialogShower = mockk<DialogShower>(relaxed = true)
    val validateZimViewModel = mockk<ValidateZimViewModel>(relaxed = true)

    val book1 = mockBook("Wikipedia")
    val book2 = mockBook("Wiktionary")

    val dialogSlot = slot<KiwixDialog>()

    every {
      dialogShower.show(capture(dialogSlot), any())
    } just Runs

    val effect = ValidateZIMFiles(
      booksOnDiskListItems = listOf(book1, book2),
      dialogShower = dialogShower,
      validateZimViewModel = validateZimViewModel,
      ioDispatcher = mainDispatcherRule.dispatcher
    )

    effect.invokeWith(activity)

    val dialog = dialogSlot.captured as KiwixDialog.ValidateZimFilesConfirmation

    assertThat(dialog.args).containsExactly("Wikipedia\nWiktionary")
  }

  @Test
  fun `confirm action should start validation and show validating dialog`() = runTest {
    val activity = mockk<KiwixMainActivity>(relaxed = true)
    val dialogShower = mockk<AlertDialogShower>(relaxed = true)
    val validateZimViewModel = mockk<ValidateZimViewModel>(relaxed = true)

    every { validateZimViewModel.items } returns MutableStateFlow(emptyList())
    every { validateZimViewModel.allZIMValidated } returns MutableStateFlow(false)

    val confirmationSlot = slot<KiwixDialog>()
    val confirmCallbackSlot = slot<() -> Unit>()

    every {
      dialogShower.show(
        capture(confirmationSlot),
        capture(confirmCallbackSlot)
      )
    } just Runs

    every {
      dialogShower.show(any<KiwixDialog.ValidatingZimFiles>())
    } just Runs

    val books = listOf(mockBook("Wikipedia"))

    val effect = ValidateZIMFiles(
      booksOnDiskListItems = books,
      dialogShower = dialogShower,
      validateZimViewModel = validateZimViewModel,
      ioDispatcher = mainDispatcherRule.dispatcher
    )

    effect.invokeWith(activity)

    confirmCallbackSlot.captured.invoke()

    advanceUntilIdle()

    coVerify(exactly = 1) {
      validateZimViewModel.startValidation(books, false)
    }

    verify(exactly = 1) {
      dialogShower.show(any<KiwixDialog.ValidatingZimFiles>())
    }
  }

  private fun mockBook(title: String): BookOnDisk {
    val book = mockk<BookOnDisk>()
    val innerBook = mockk<LibkiwixBook>()

    every { innerBook.title } returns title
    every { book.book } returns innerBook

    return book
  }
}
