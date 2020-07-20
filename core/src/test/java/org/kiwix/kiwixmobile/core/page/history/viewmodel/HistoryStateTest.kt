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
  internal fun `visiblePageItems returns history based on filter`() {
    val matchingItem = historyItem(historyTitle = "Title")
    val nonMatchingItem = historyItem(historyTitle = "noMatch")
    assertThat(
      historyState(listOf(matchingItem, nonMatchingItem), searchTerm = "title")
        .visiblePageItems
    ).isEqualTo(listOf(DateItem(matchingItem.dateString), matchingItem))
  }

  @Test
  internal fun `copyNewItems should set new items to pageItems`() {
    assertThat(historyState(emptyList()).copy(listOf(historyItem())).pageItems).isEqualTo(
      listOf(historyItem())
    )
  }

  @Test
  internal fun `visiblePageItems should merge dates if on same day`() {
    val item1 = historyItem()
    val item2 = historyItem()
    assertThat(historyState(listOf(item1, item2)).visiblePageItems)
      .isEqualTo(listOf(DateItem(item1.dateString), item1, item2))
  }

  @Test
  internal fun `visiblePageItems should not merge dates if on different days`() {
    val item1 = historyItem(dateString = "today")
    val item2 = historyItem(dateString = "tomorrow")
    assertThat(historyState(listOf(item1, item2)).visiblePageItems)
      .isEqualTo(listOf(DateItem(item1.dateString), item1, DateItem(item2.dateString), item2))
  }
}
