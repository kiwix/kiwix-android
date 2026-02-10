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

package org.kiwix.kiwixmobile.core.extensions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class StringExtensionsTest {
  @Test
  internal fun `toSlug replaces spaces with hyphens`() {
    assertThat("hello world".toSlug()).isEqualTo("hello-world")
  }

  @Test
  internal fun `toSlug converts to lowercase`() {
    assertThat("Hello World".toSlug()).isEqualTo("hello-world")
  }

  @Test
  internal fun `toSlug removes forward slashes`() {
    assertThat("path/to/article".toSlug()).isEqualTo("pathtoarticle")
  }

  @Test
  internal fun `toSlug removes colons`() {
    assertThat("title: subtitle".toSlug()).isEqualTo("title-subtitle")
  }

  @Test
  internal fun `toSlug handles multiple special characters`() {
    assertThat("Category: Science/Physics".toSlug()).isEqualTo("category-sciencephysics")
  }

  @Test
  internal fun `toSlug returns empty string for empty input`() {
    assertThat("".toSlug()).isEqualTo("")
  }

  @Test
  internal fun `toSlug handles already slugified string`() {
    assertThat("already-slugified".toSlug()).isEqualTo("already-slugified")
  }

  @Test
  internal fun `toSlug handles multiple consecutive spaces`() {
    assertThat("hello  world".toSlug()).isEqualTo("hello--world")
  }

  @Test
  internal fun `toSlug handles mixed case with special characters`() {
    assertThat("The History of: Art/Music".toSlug()).isEqualTo("the-history-of-artmusic")
  }
}
