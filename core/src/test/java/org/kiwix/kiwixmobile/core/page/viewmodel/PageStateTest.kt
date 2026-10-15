/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.page.viewmodel

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.page.PageImpl
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.bookmark.models.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.page.history.models.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.notes.models.NoteListItem

internal class PageStateTest {
  @Test
  internal fun `isInSelectionMode is true when item is selected`() {
    assertThat(
      TestablePageState(
        listOf(
          PageImpl(
            isSelected = true,
            zimReaderSource = mockk()
          )
        )
      ).isInSelectionState
    )
      .isEqualTo(true)
  }

  @Test
  internal fun `isInSelectionMode is false when no item is selected`() {
    assertThat(TestablePageState(listOf(PageImpl(zimReaderSource = mockk()))).isInSelectionState)
      .isEqualTo(false)
  }

  @Test
  internal fun `filteredPageItems should show all if show all is true`() {
    val item = PageImpl(zimId = "notSame", zimReaderSource = mockk())
    assertThat(
      TestablePageState(
        listOf(item),
        showAll = true
      ).publicFilteredPageItems
    ).isEqualTo(listOf(item))
  }

  @Test
  internal fun `filteredPageItems should not show all if showAll is false with different zimId`() {
    val item = PageImpl(zimId = "notSame", zimReaderSource = mockk())
    assertThat(
      TestablePageState(listOf(item), showAll = false).publicFilteredPageItems
    ).isEqualTo(emptyList<Page>())
  }

  @Test
  internal fun `filteredPageItems should show item if zimId is same`() {
    val item = PageImpl(zimId = "sameId", zimReaderSource = mockk())
    assertThat(
      TestablePageState(
        listOf(item),
        currentZimId = "sameId",
        showAll = false
      ).publicFilteredPageItems
    ).isEqualTo(listOf(item))
  }

  @Test
  internal fun `filteredPageItems should hide items with not equal searchTerm`() {
    val item = PageImpl(title = "title", zimReaderSource = mockk())
    assertThat(
      TestablePageState(
        listOf(item),
        searchTerm = "notEqual",
        showAll = false
      ).publicFilteredPageItems
    ).isEqualTo(emptyList<Page>())
  }

  @Test
  internal fun `filteredPageItems should show items with equal searchTerm (ignore case)`() {
    val item = PageImpl(title = "title", zimReaderSource = mockk())
    assertThat(
      TestablePageState(
        listOf(item),
        searchTerm = "Title",
        showAll = false
      ).publicFilteredPageItems
    ).isEqualTo(emptyList<Page>())
  }

  @Test
  internal fun `getItemsAfterToggleSelectionOfItem toggles isSelected for matching page id`() {
    val item = PageImpl(zimReaderSource = mockk(), id = 1L)
    val result = TestablePageState(listOf(item)).getItemsAfterToggleSelectionOfItem(item)
    assertThat(result.single().isSelected).isTrue()
  }

  @Test
  internal fun `getItemsAfterToggleSelectionOfItem does not toggle isSelected for non matching page id`() {
    val item = PageImpl(zimReaderSource = mockk(), id = 1L)
    val otherPage = PageImpl(zimReaderSource = mockk(), id = 2L)
    val result = TestablePageState(listOf(item)).getItemsAfterToggleSelectionOfItem(otherPage)
    assertThat(result.single().isSelected).isFalse()
  }

  @Test
  internal fun `getItemsAfterToggleSelectionOfItem toggles bookmark when url and zimId both match`() {
    val bookmark = LibkiwixBookmarkItem(zimId = "zim1", bookmarkUrl = "page.html")
    val result = TestablePageState(listOf(bookmark)).getItemsAfterToggleSelectionOfItem(bookmark)
    assertThat(result.single().isSelected).isTrue()
  }

  @Test
  internal fun `getItemsAfterToggleSelectionOfItem does not toggle bookmark with same url but different zimId`() {
    val bookmarkInZim1 = LibkiwixBookmarkItem(zimId = "zim1", bookmarkUrl = "page.html")
    val bookmarkInZim2 = LibkiwixBookmarkItem(zimId = "zim2", bookmarkUrl = "page.html")
    val result =
      TestablePageState(listOf(bookmarkInZim1, bookmarkInZim2))
        .getItemsAfterToggleSelectionOfItem(bookmarkInZim1)
    assertThat((result[0] as LibkiwixBookmarkItem).isSelected).isTrue()
    assertThat((result[1] as LibkiwixBookmarkItem).isSelected).isFalse()
  }

  @Test
  internal fun `getItemsAfterToggleSelectionOfItem toggles only the matching history item by id`() {
    val historyItem1 = HistoryItem(
      zimId = "zim1",
      zimName = "zimName",
      zimReaderSource = null,
      favicon = null,
      historyUrl = "page.html",
      title = "title",
      dateString = "date",
      timeStamp = 0L,
      id = 1L
    )
    val historyItem2 = historyItem1.copy(id = 2L)
    val result =
      TestablePageState(listOf(historyItem1, historyItem2))
        .getItemsAfterToggleSelectionOfItem(historyItem1)
    assertThat((result[0] as HistoryItem).isSelected).isTrue()
    assertThat((result[1] as HistoryItem).isSelected).isFalse()
  }

  @Test
  internal fun `getItemsAfterToggleSelectionOfItem toggles only the matching note item by id`() {
    val note1 = NoteListItem(
      zimId = "zim1",
      title = "title",
      zimReaderSource = null,
      zimUrl = "page.html",
      noteFilePath = "path",
      favicon = null,
      id = 1L
    )
    val note2 = note1.copy(id = 2L)
    val result =
      TestablePageState(listOf(note1, note2)).getItemsAfterToggleSelectionOfItem(note1)
    assertThat((result[0] as NoteListItem).isSelected).isTrue()
    assertThat((result[1] as NoteListItem).isSelected).isFalse()
  }
}
