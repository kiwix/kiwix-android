/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.reader

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class ZimFileReaderTest {
  @Test
  fun checkMimeTypeWithSemicolon() {
    val mimeTypeHtml = "text/html;"
    assertEquals("text/html", mimeTypeHtml.truncateMimeType)
  }

  @Test
  fun checkMimeTypeWithSpecialCharacters() {
    val mimeTypeCss = "text/css^([^ ]+).*\$"
    assertEquals("text/css$1", mimeTypeCss.truncateMimeType)
  }

  @Test
  fun checkMimeTypeWithSemicolonBeforeSpecialCharacters() {
    val mimeTypeCss = "text/application;^([^ ]+).*\$;"
    assertEquals("text/application", mimeTypeCss.truncateMimeType)
  }
}
