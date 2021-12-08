/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.zim_manager.library_view.adapter

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.NotEnoughSpaceFor4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.Unknown
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem.BookItem

internal class LibraryListItemTest {

  val book = mockk<Book>()

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { book.getId() } returns "0"
    every { book.getSize() } returns "0"
  }

  @Test
  internal fun `Unknown file system state files under 4GB can be downloaded`() {
    assertThat(canBeDownloaded(book, Unknown)).isTrue()
  }

  @Test
  internal fun `Unknown file system state greater than 4GB can't be downloaded`() {
    every { book.getSize() } returns (org.kiwix.kiwixmobile.zimManager.Fat32Checker.FOUR_GIGABYTES_IN_KILOBYTES + 1).toString()
    assertThat(canBeDownloaded(book, Unknown)).isFalse()
  }

  @Test
  internal fun `Unknown file system state empty size can be downloaded`() {
    every { book.getSize() } returns ""
    assertThat(canBeDownloaded(book, Unknown)).isTrue()
  }

  @Test
  internal fun `CannotWrite4GB file system state can be downloaded`() {
    assertThat(canBeDownloaded(book, CannotWrite4GbFile)).isTrue()
  }

  @Test
  internal fun `CanWrite4GbFile file system state can be downloaded`() {
    assertThat(canBeDownloaded(book, CanWrite4GbFile)).isTrue()
  }

  @Test
  internal fun `NotEnoughSpaceFor4GbFile file system state can be downloaded`() {
    assertThat(canBeDownloaded(book, NotEnoughSpaceFor4GbFile)).isTrue()
  }

  private fun canBeDownloaded(book: Book, fileSystemState: FileSystemState) =
    BookItem(book, fileSystemState).canBeDownloaded
}
