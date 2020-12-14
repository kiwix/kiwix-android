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

package org.kiwix.kiwixmobile.core.search.viewmodel

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.reader.ZimReader
import org.kiwix.kiwixmobile.core.search.SearchSuggestion
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.ZimSearchResultListItem

internal class ZimSearchResultGeneratorTest {

  private val zimReader: ZimReader = mockk()

  private val zimSearchResultGenerator: ZimSearchResultGenerator =
    ZimSearchResultGenerator()

  @Test
  internal fun `empty search term returns empty list`() {
    runBlocking {
      assertThat(zimSearchResultGenerator.generateSearchResults("", zimReader))
        .isEqualTo(emptyList<ZimSearchResultListItem>())
    }
  }

  @Test
  internal fun `suggestion results are distinct`() {
    val validTitle = "title"
    val searchTerm = " "
    val item = mockk<SearchSuggestion>()
    every { zimReader.searchSuggestions(" ", 200) } returns true
    every { zimReader.getNextSuggestion() } returnsMany listOf(item, item, null)
    every { item.title } returns validTitle
    runBlocking {
      assertThat(zimSearchResultGenerator.generateSearchResults(searchTerm, zimReader))
        .isEqualTo(listOf(ZimSearchResultListItem(validTitle)))
      verify {
        zimReader.searchSuggestions(searchTerm, 200)
      }
    }
  }
}
