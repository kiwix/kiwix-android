/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryListItem.CategoryItem
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryListItem.HeaderItem
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State.Content
import org.kiwix.sharedFunctions.category

class StateTest {
  @Nested
  inner class ContentTests {
    @Test
    fun `creates category list items with headers for active and inactive categories`() {
      val content = Content(listOf(category(), category(isActive = true)))
      assertThat(content.viewItems).isEqualTo(
        listOf(
          HeaderItem(HeaderItem.SELECTED),
          CategoryItem(category(isActive = true)),
          HeaderItem(HeaderItem.OTHER),
          CategoryItem(category())
        )
      )
    }

    @Test
    fun `filters out based on filter`() {
      val content =
        Content(
          listOf(category(category = "matchesFilter"), category(isActive = true))
        ).updateFilter("matches")
      assertThat(content.viewItems).isEqualTo(
        listOf(
          HeaderItem(HeaderItem.OTHER),
          CategoryItem(category(category = "matchesFilter"))
        )
      )
    }

    @Test
    fun `select updates category items in-place without resorting`() {
      val cat1 = category(id = 1L, category = "wikipedia", isActive = false)
      val cat2 = category(id = 2L, category = "gutenberg", isActive = false)
      val content = Content(listOf(cat1, cat2))

      val initialOrder = content.viewItems
      assertThat(initialOrder).hasSize(3) // 1 header (OTHER) + 2 categories

      val selectedWikiItem = CategoryItem(cat1)
      val updatedContent = content.select(selectedWikiItem)

      assertThat(updatedContent.viewItems).hasSize(3)
      assertThat(updatedContent.viewItems[0]).isEqualTo(HeaderItem(HeaderItem.OTHER))
      assertThat((updatedContent.viewItems[1] as CategoryItem).category.active).isTrue()
      assertThat((updatedContent.viewItems[2] as CategoryItem).category.active).isFalse()
    }
  }
}
