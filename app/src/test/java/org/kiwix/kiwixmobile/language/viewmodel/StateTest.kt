/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */

package org.kiwix.kiwixmobile.language.viewmodel

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.language
import org.kiwix.kiwixmobile.language.adapter.LanguageListItem.HeaderItem
import org.kiwix.kiwixmobile.language.adapter.LanguageListItem.LanguageItem
import org.kiwix.kiwixmobile.language.viewmodel.State.Content

class StateTest {
  @Nested
  inner class ContentTests {
    @Test
    fun `creates language list items with headers for active and inactive languages`() {
      val content = Content(listOf(language(), language(isActive = true)))
      assertThat(content.viewItems).isEqualTo(
        listOf(
          HeaderItem(Long.MAX_VALUE),
          LanguageItem(language(isActive = true)),
          HeaderItem(Long.MIN_VALUE),
          LanguageItem(language())
        )
      )
    }

    @Test
    fun `filters out based on filter`() {
      val content = Content(
        listOf(language(language = "matchesFilter"), language(isActive = true))
      ).updateFilter("matches")
      assertThat(content.viewItems).isEqualTo(
        listOf(
          HeaderItem(Long.MIN_VALUE),
          LanguageItem(language(language = "matchesFilter"))
        )
      )
    }
  }
}
