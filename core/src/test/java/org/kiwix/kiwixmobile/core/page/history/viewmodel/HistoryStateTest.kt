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

package org.kiwix.kiwixmobile.core.page.history.viewmodel

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.DateItem
import org.kiwix.kiwixmobile.core.page.historyItem
import org.kiwix.kiwixmobile.core.page.historyState

internal class HistoryStateTest {
  @Test
  internal fun `isInSelectionMode is true when item is selected`() {
    assertThat(historyState(listOf(historyItem(isSelected = true))).isInSelectionState)
      .isEqualTo(true)
  }

  @Test
  internal fun `isInSelectionMode is false when no item is selected`() {
    assertThat(historyState(listOf(historyItem(isSelected = false))).isInSelectionState)
      .isEqualTo(false)
  }

  @Test
  internal fun `historyListItems returns history from all books when showAll is true`() {
    val item = historyItem(isSelected = false)
    assertThat(historyState(listOf(item), showAll = true).historyListItems)
      .isEqualTo(listOf(DateItem(item.dateString), item))
  }

  @Test
  internal fun `historyListItems returns history from current book when showAll is false`() {
    val item1 = historyItem(isSelected = false, zimId = "id1")
    val item2 = historyItem(isSelected = false, zimId = "id2")
    assertThat(historyState(listOf(item1, item2), showAll = false, zimId = "id1").historyListItems)
      .isEqualTo(listOf(DateItem(item1.dateString), item1))
  }

  @Test
  internal fun `historyListItems returns history based on filter`() {
    val matchingItem = historyItem(historyTitle = "Title")
    val nonMatchingItem = historyItem(historyTitle = "noMatch")
    assertThat(
      historyState(listOf(matchingItem, nonMatchingItem), searchTerm = "title")
        .historyListItems
    ).isEqualTo(listOf(DateItem(matchingItem.dateString), matchingItem))
  }

  @Test
  internal fun `historyListItems should merge dates if on same day`() {
    val item1 = historyItem()
    val item2 = historyItem()
    assertThat(historyState(listOf(item1, item2)).historyListItems)
      .isEqualTo(listOf(DateItem(item1.dateString), item1, item2))
  }

  @Test
  internal fun `historyListItems should not merge dates if on different days`() {
    val item1 = historyItem(dateString = "today")
    val item2 = historyItem(dateString = "tomorrow")
    assertThat(historyState(listOf(item1, item2)).historyListItems)
      .isEqualTo(listOf(DateItem(item1.dateString), item1, DateItem(item2.dateString), item2))
  }
}
