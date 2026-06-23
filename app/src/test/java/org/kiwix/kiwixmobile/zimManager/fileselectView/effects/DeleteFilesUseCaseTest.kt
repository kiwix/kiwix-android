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
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import java.io.File

class DeleteFilesUseCaseTest {
  private lateinit var deleteFilesUseCase: DeleteFilesUseCase

  private val libkiwixBookOnDisk = mockk<LibkiwixBookOnDisk>(relaxed = true)
  private val zimReaderContainer = mockk<ZimReaderContainer>(relaxed = true)

  private var file1 = File("/storage/kiwix.zim")
  private val file2 = File("/storage/test.zim")
  private lateinit var book: BookOnDisk

  @BeforeEach
  fun setup() {
    clearAllMocks()
    mockkStatic(FileUtils::class)
    mockkObject(FileUtils)
    mockkStatic("org.kiwix.kiwixmobile.core.extensions.FileExtensionsKt")

    coEvery { any<File>().isFileExist() } returns false

    coEvery {
      FileUtils.deleteZimFile(file1.path)
    } just Runs

    val libkiwixBook =
      LibkiwixBook(_id = "book-id-1", file = file1)

    book = BookOnDisk(book = libkiwixBook, zimReaderSource = ZimReaderSource(file1))

    deleteFilesUseCase =
      DeleteFilesUseCase(libkiwixBookOnDisk, zimReaderContainer)
  }

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun invoke_whenFileDeleted_deletesBookAndReturnsTrue() = runTest {
    val result = deleteFilesUseCase(listOf(book))

    assertTrue(result)

    coVerify(exactly = 1) {
      libkiwixBookOnDisk.delete(book.book.id)
    }
  }

  @Test
  fun invoke_whenFileStillExists_returnsFalseAndDoesNotDeleteBook() = runTest {
    coEvery {
      file1.isFileExist()
    } returns true

    val result = deleteFilesUseCase(listOf(book))

    assertFalse(result)

    coVerify(exactly = 1) {
      FileUtils.deleteZimFile(file1.path)
    }
    coVerify(exactly = 0) {
      libkiwixBookOnDisk.delete(book.book.id)
    }
  }

  @Test
  fun invoke_whenFileIsNull_returnsFalseAndDoesNotDeleteBook() = runTest {
    val libkiwixBook = LibkiwixBook(_id = "")
    book = BookOnDisk(book = libkiwixBook, zimReaderSource = ZimReaderSource())

    val result = deleteFilesUseCase(listOf(book))

    assertFalse(result)

    coVerify(exactly = 0) {
      FileUtils.deleteZimFile(any())
    }
    coVerify(exactly = 0) {
      libkiwixBookOnDisk.delete(book.book.id)
    }
  }

  @Test
  fun invoke_whenCurrentBookIsOpenAndDeletesBook_clearsReaderSource() = runTest {
    val currentSource = book.zimReaderSource

    every {
      zimReaderContainer.zimReaderSource
    } returns currentSource

    deleteFilesUseCase(listOf(book))

    coVerify {
      zimReaderContainer.setZimReaderSource(null)
    }
  }

  @Test
  fun invoke_whenDifferentBookIsOpenAndDeletesBook_doesNotClearReaderSource() = runTest {
    val file2Source = ZimReaderSource(file2)

    every {
      zimReaderContainer.zimReaderSource
    } returns file2Source

    deleteFilesUseCase(listOf(book))

    coVerify(exactly = 0) {
      zimReaderContainer.setZimReaderSource(null)
    }
  }

  @Test
  fun invoke_whenAllBooksDeleted_returnsTrue() = runTest {
    val secondBook =
      BookOnDisk(
        book = LibkiwixBook(_id = "book-id-2"),
        zimReaderSource = ZimReaderSource(file2)
      )

    val result =
      deleteFilesUseCase(
        listOf(book, secondBook)
      )

    assertTrue(result)

    coVerify(exactly = 1) {
      libkiwixBookOnDisk.delete("book-id-1")
    }

    coVerify(exactly = 1) {
      libkiwixBookOnDisk.delete("book-id-2")
    }
  }

  @Test
  fun invoke_whenOneBookFails_returnsFalse() = runTest {
    coEvery {
      file2.isFileExist()
    } returns true

    val failingBook =
      BookOnDisk(
        book = LibkiwixBook(_id = "book-id-2"),
        zimReaderSource = ZimReaderSource(file2)
      )

    val result =
      deleteFilesUseCase(
        listOf(book, failingBook)
      )

    assertFalse(result)
  }
}
