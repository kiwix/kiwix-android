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

package org.kiwix.kiwixmobile.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.extensions.getFavicon

@RunWith(AndroidJUnit4::class)
@SmallTest
class BookExtensionsTest {

  @Test
  fun getFaviconShouldReturnNullForNullBook() {
    val result = null.getFavicon()
    assertNull(
      "getFavicon on a null Book should return null",
      result
    )
  }

  @Test
  fun getFaviconShouldReturnEmptyStringWhenIllustrationThrowsException() {
    // Book() with no backing archive will throw when getIllustration is called,
    // exercising the runCatching error-handling path.
    val book = org.kiwix.libkiwix.Book()
    val result = book.getFavicon()
    assertEquals(
      "getFavicon should return empty string when illustration extraction fails",
      "",
      result
    )
  }
}
