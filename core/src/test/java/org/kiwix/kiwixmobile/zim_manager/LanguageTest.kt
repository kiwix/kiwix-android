package org.kiwix.kiwixmobile.zim_manager

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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.language

class LanguageTest {

  @Nested
  inner class Equals {
    @Test
    @Suppress("UnusedEquals", "ReplaceCallWithBinaryOperator") // cannot == Unit
    fun `throws exception when object is not language item`() {
      assertThrows(ClassCastException::class.java) { language().equals(Unit) }
    }

    @Test
    fun `not equals with mismatched active states`() {
      assertThat(language() == language(isActive = true)).isFalse()
    }

    @Test
    fun `not equals with mismatched language`() {
      assertThat(language() == language(language = "mismatch")).isFalse()
    }

    @Test
    fun `is equal when language and active are equal`() {
      assertThat(
        language(language = "lang", isActive = true) == language(
          language = "lang", isActive = true
        )
      ).isTrue()
    }
  }

  @Nested
  inner class Matches {
    @Test
    fun `does not match if language and localised do not contain filter`() {
      assertThat(language().matches("filter")).isFalse()
    }

    @Test
    fun `matches if language contains filter`() {
      assertThat(language(language = "Filtermatcher").matches("filter")).isTrue()
    }

    @Test
    fun `matches if languageLocalized contains filter`() {
      assertThat(language(languageLocalized = "Filtermatcher").matches("filter")).isTrue()
    }
  }
}
