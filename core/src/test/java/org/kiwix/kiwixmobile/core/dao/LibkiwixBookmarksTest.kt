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

package org.kiwix.kiwixmobile.core.dao

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LibkiwixBookmarksTest {
  @Test
  fun `bookmark url of the article matches the article path`() {
    assertTrue(isBookmarkOfPage("https://kiwix.app/Food_vs._fuel", "Food_vs._fuel"))
  }

  @Test
  fun `bookmark url of a redirect does not match the article path`() {
    assertFalse(isBookmarkOfPage("https://kiwix.app/Food_vs_fuel", "Food_vs._fuel"))
  }

  @Test
  fun `encoded bookmark url matches the decoded article path`() {
    assertTrue(isBookmarkOfPage("https://kiwix.app/A/C%C3%B4te_d%27Ivoire", "A/Côte_d'Ivoire"))
  }
}
