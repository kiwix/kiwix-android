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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.page.PageImpl
import org.kiwix.kiwixmobile.core.page.adapter.Page

internal class PageStateTest {
  @Test
  internal fun `isInSelectionMode is true when item is selected`() {
    assertThat(TestablePageState(listOf(PageImpl(isSelected = true))).isInSelectionState)
      .isEqualTo(true)
  }

  @Test
  internal fun `isInSelectionMode is false when no item is selected`() {
    assertThat(TestablePageState(listOf(PageImpl())).isInSelectionState)
      .isEqualTo(false)
  }

  @Test
  internal fun `filteredPageItems should show all if show all is true`() {
    val item = PageImpl(zimId = "notSame")
    assertThat(
      TestablePageState(
        listOf(item),
        showAll = true
      ).publicFilteredPageItems
    ).isEqualTo(listOf(item))
  }

  @Test
  internal fun `filteredPageItems should not show all if showAll is false with different zimId`() {
    val item = PageImpl(zimId = "notSame")
    assertThat(
      TestablePageState(listOf(item), showAll = false).publicFilteredPageItems
    ).isEqualTo(emptyList<Page>())
  }

  @Test
  internal fun `filteredPageItems should show item if zimId is same`() {
    val item = PageImpl(zimId = "sameId")
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
    val item = PageImpl(title = "title")
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
    val item = PageImpl(title = "title")
    assertThat(
      TestablePageState(
        listOf(item),
        searchTerm = "Title",
        showAll = false
      ).publicFilteredPageItems
    ).isEqualTo(emptyList<Page>())
  }
}
