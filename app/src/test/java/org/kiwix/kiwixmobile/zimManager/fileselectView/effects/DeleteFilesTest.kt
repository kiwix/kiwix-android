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
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.di.components.KiwixActivityComponent
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteFilesTest {
  @BeforeEach
  fun before() {
    mockkObject(FileUtils)
    mockkStatic("org.kiwix.kiwixmobile.core.extensions.FileExtensionsKt")
    mockkStatic("org.kiwix.kiwixmobile.core.extensions.ContextExtensionsKt")
  }

  @AfterEach
  fun tearDown() {
    unmockkAll()
    unmockkObject(FileUtils)
    unmockkStatic("org.kiwix.kiwixmobile.core.extensions.FileExtensionsKt")
    unmockkStatic("org.kiwix.kiwixmobile.core.extensions.ContextExtensionsKt")
  }

  @Test
  fun `invokeWith should show delete confirmation dialog`() {
    val activity = mockActivity()
    val dialogShower = mockk<DialogShower>(relaxed = true)

    val dialogSlot = slot<KiwixDialog>()

    every {
      dialogShower.show(capture(dialogSlot), any())
    } just Runs

    val book1 = mockBook("Wikipedia")
    val book2 = mockBook("Wiktionary")

    val effect = DeleteFiles(
      booksOnDiskListItems = listOf(book1, book2),
      dialogShower = dialogShower
    )

    injectDependencies(effect)

    effect.invokeWith(activity)

    val dialog = dialogSlot.captured as KiwixDialog.DeleteZims

    assertThat(dialog.args)
      .containsExactly("Wikipedia\nWiktionary")
  }

  private fun mockActivity(): KiwixMainActivity {
    val activity = mockk<KiwixMainActivity>(relaxed = true)

    val component = mockk<KiwixActivityComponent>(relaxed = true)

    every { activity.cachedComponent } returns component
    every { component.inject(any<DeleteFiles>()) } just Runs

    return activity
  }

  private fun injectDependencies(effect: DeleteFiles) {
    effect.libkiwixBookOnDisk = mockk(relaxed = true)
    effect.zimReaderContainer = mockk(relaxed = true)
  }

  private fun mockBook(
    title: String,
    zimReaderSource: ZimReaderSource = mockk(relaxed = true),
    fileExists: Boolean = false
  ): BookOnDisk {
    val book = mockk<BookOnDisk>(relaxed = true)

    val innerBook = mockk<LibkiwixBook>(relaxed = true)

    val file = mockk<File>(relaxed = true)

    every { innerBook.title } returns title
    every { innerBook.id } returns "book-id"

    every { book.book } returns innerBook
    every { book.zimReaderSource } returns zimReaderSource

    every { zimReaderSource.file } returns file

    every { file.path } returns "/tmp/test.zim"

    coEvery { file.isFileExist() } returns fileExists

    return book
  }
}
