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

package org.kiwix.kiwixmobile.core.entity

import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

class LibKiwixBookTest {
  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  @Nested
  inner class PropertyFallbacks {
    @Test
    fun libkiwixBook_whenNativeBookIsNull_returnsEmptyStrings() = runTest {
      val libkiwixBook = LibkiwixBook(nativeBook = null)

      assertThat(libkiwixBook.id).isEmpty()
      assertThat(libkiwixBook.title).isEmpty()
      assertThat(libkiwixBook.path).isNull()
    }
  }

  @Nested
  inner class IdentityAndEquality {
    @Test
    fun libkiwix_whenIDsMatchesRegardlessOfOtherFields_returnsTrue() = runTest {
      val book1 = LibkiwixBook(nativeBook = null).apply {
        id = "kiwix"
        title = "Book 1"
      }
      val book2 = LibkiwixBook(nativeBook = null).apply {
        id = "kiwix"
        title = "Book 2"
      }

      assertThat(book1).isEqualTo(book2)
      assertThat(book1.hashCode()).isEqualTo(book2.hashCode())
    }

    @Test
    fun libkiwix_whenIDsDoesNotMatches_returnsFalse() = runTest {
      val book1 = LibkiwixBook().apply { id = "A" }
      val book2 = LibkiwixBook().apply { id = "B" }
      assertThat(book1).isNotEqualTo(book2)
    }
  }

  @Nested
  inner class SourceCreation {
    @Test
    fun zimReaderSource_whenCreatesSourceWithCorrectFilePath_returnsTrue() = runTest {
      val libkiwixBook = LibkiwixBook().apply { path = "/storage/test.zim" }

      val source = libkiwixBook.zimReaderSource

      assertThat(source.file).isEqualTo(File("/storage/test.zim"))
    }
  }
}
