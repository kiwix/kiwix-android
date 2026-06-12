/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
 * program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.core.entity

import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.BookTestWrapper
import java.io.File

class LibKiwixBookTest {
  private val bookId = "id"
  private val bookTitle = "title"
  private val bookDescription = "description"
  private val bookLanguage = "eng"
  private val bookCreator = "creator"
  private val bookPublisher = "publisher"
  private val bookDate = "2022-10-30"
  private val bookUrl = "https://kiwix.org/download/alpine-linux.url.meta4"
  private val bookArticleCount = "10"
  private val bookMediaCount = "20"
  private val bookSize = "1024"
  private val name = "Alpine Linux Wiki"
  private val bookPath = "/storage/test.zim"
  private val bookFavIcon = "favIcon"
  private val bookFile = File("")
  private val bookTags = ""
  private val bookSearchMatches = 10

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  @Nested
  inner class PropertyBehavior {
    @Test
    fun libkiwixBookWhenNativeBookIsNullReturnsDefaultValues() = runTest {
      val libkiwixBook = LibkiwixBook()
      assertThat(libkiwixBook.id).isEmpty()
      assertThat(libkiwixBook.title).isEmpty()
      assertThat(libkiwixBook.description).isNull()
      assertThat(libkiwixBook.language).isEmpty()
      assertThat(libkiwixBook.creator).isEmpty()
      assertThat(libkiwixBook.publisher).isEmpty()
      assertThat(libkiwixBook.date).isEmpty()
      assertThat(libkiwixBook.url).isNull()
      assertThat(libkiwixBook.articleCount).isNull()
      assertThat(libkiwixBook.mediaCount).isNull()
      assertThat(libkiwixBook.size).isEmpty()
      assertThat(libkiwixBook.bookName).isNull()
      assertThat(libkiwixBook.favicon).isEmpty()
      assertThat(libkiwixBook.tags).isNull()
      assertThat(libkiwixBook.path).isNull()
      assertThat(libkiwixBook.file).isNull()
    }

    @Test
    fun libkiwixBookWhenNativeBookIsNullAndSetValues() = runTest {
      val libkiwixBook = LibkiwixBook().apply {
        id = bookId
        title = bookTitle
        description = bookDescription
        language = bookLanguage
        creator = bookCreator
        publisher = bookPublisher
        date = bookDate
        url = bookUrl
        articleCount = bookArticleCount
        mediaCount = bookMediaCount
        size = bookSize
        file = bookFile
        path = bookPath
        bookName = name
        favicon = bookFavIcon
        tags = bookTags
        searchMatches = bookSearchMatches
      }
      assertLibkiwixBookValues(libkiwixBook)
    }

    @Test
    fun libkiwixBookWhenNativeBookAvailable() = runTest {
      val nativeBook = BookTestWrapper(
        bookId,
        bookTitle,
        bookDescription,
        bookLanguage,
        bookCreator,
        bookPublisher,
        bookDate,
        bookUrl,
        bookArticleCount,
        bookMediaCount,
        bookSize,
        bookPath,
        name,
        bookTags
      )
      val libkiwixBook = LibkiwixBook(nativeBook).apply {
        searchMatches = bookSearchMatches
        file = bookFile
      }
      assertLibkiwixBookValues(libkiwixBook, true)
    }

    private fun assertLibkiwixBookValues(
      libkiwixBook: LibkiwixBook,
      isNativeBook: Boolean = false
    ) {
      assertThat(libkiwixBook.id).isEqualTo(bookId)
      assertThat(libkiwixBook.title).isEqualTo(bookTitle)
      assertThat(libkiwixBook.description).isEqualTo(bookDescription)
      assertThat(libkiwixBook.language).isEqualTo(bookLanguage)
      assertThat(libkiwixBook.creator).isEqualTo(bookCreator)
      assertThat(libkiwixBook.publisher).isEqualTo(bookPublisher)
      assertThat(libkiwixBook.date).isEqualTo(bookDate)
      assertThat(libkiwixBook.url).isEqualTo(bookUrl)
      assertThat(libkiwixBook.articleCount).isEqualTo(bookArticleCount)
      assertThat(libkiwixBook.mediaCount).isEqualTo(bookMediaCount)
      assertThat(libkiwixBook.size).isEqualTo(bookSize)
      assertThat(libkiwixBook.bookName).isEqualTo(name)
      if (isNativeBook) {
        // Since we are not mocking the native Illustration.
        // Default value is empty string.
        assertThat(libkiwixBook.favicon).isEqualTo("")
      } else {
        assertThat(libkiwixBook.favicon).isEqualTo(bookFavIcon)
      }
      assertThat(libkiwixBook.tags).isEqualTo(bookTags)
      assertThat(libkiwixBook.path).isEqualTo(bookPath)
      assertThat(libkiwixBook.file).isEqualTo(bookFile)
      assertThat(libkiwixBook.searchMatches).isEqualTo(bookSearchMatches)
    }
  }

  @Nested
  inner class IdentityAndEquality {
    @Test
    fun libkiwix_whenIDsMatchesRegardlessOfOtherFields_returnsTrue() = runTest {
      val book1 = LibkiwixBook().apply {
        id = "kiwix"
        title = "Book 1"
      }
      val book2 = LibkiwixBook().apply {
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

    @Test
    fun libkiwix_whenComparedWithDifferentType_returnsFalse() = runTest {
      val book = LibkiwixBook().apply { id = "A" }

      val result = book.equals("not a book")

      assertThat(result).isFalse()
    }

    @Test
    fun libkiwix_whenComparedWithNull_returnsFalse() = runTest {
      val book = LibkiwixBook().apply { id = "A" }

      val result = book == null

      assertThat(result).isFalse()
    }
  }

  @Nested
  inner class SourceCreation {
    @Test
    fun zimReaderSource_whenCreatesSourceWithCorrectFilePath_returnsTrue() = runTest {
      val libkiwixBook = LibkiwixBook().apply { path = bookPath }
      val source = libkiwixBook.zimReaderSource
      assertThat(source.file).isEqualTo(File(bookPath))
    }

    @Test
    fun zimReaderSource_whenPathIsNull_returnsEmptyFile() = runTest {
      val libkiwixBook = LibkiwixBook().apply {
        path = null
      }

      val source = libkiwixBook.zimReaderSource

      assertThat(source.file).isEqualTo(File(""))
    }
  }
}
