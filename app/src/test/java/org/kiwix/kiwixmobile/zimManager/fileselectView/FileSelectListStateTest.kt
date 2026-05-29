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

package org.kiwix.kiwixmobile.zimManager.fileselectView

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.NORMAL

class FileSelectListStateTest {
  @Test
  fun `selectedBooks should return only selected BookOnDisk items`() {
    val selectedBook = mockBook(isSelected = true)
    val unselectedBook = mockBook(isSelected = false)

    val state = FileSelectListState(bookOnDiskListItems = listOf(selectedBook, unselectedBook))
    assertThat(state.selectedBooks)
      .containsExactly(selectedBook)
  }

  @Test
  fun `selectedBooks should ignore non BookOnDisk items`() {
    val selectedBook = mockBook(isSelected = true)

    val otherItem = mockk<BooksOnDiskListItem>(relaxed = true)

    val state = FileSelectListState(bookOnDiskListItems = listOf(selectedBook, otherItem))
    assertThat(state.selectedBooks)
      .containsExactly(selectedBook)
  }

  @Test
  fun `selectedBooks should return empty list when no books selected`() {
    val book1 = mockBook(isSelected = false)
    val book2 = mockBook(isSelected = false)

    val state = FileSelectListState(bookOnDiskListItems = listOf(book1, book2))
    assertThat(state.selectedBooks).isEmpty()
  }

  @Test
  fun `selectionMode should default to NORMAL`() {
    val state = FileSelectListState(bookOnDiskListItems = emptyList())
    assertThat(state.selectionMode).isEqualTo(NORMAL)
  }

  private fun mockBook(isSelected: Boolean): BookOnDisk {
    return mockk<BookOnDisk> {
      every { this@mockk.isSelected } returns isSelected
    }
  }
}
