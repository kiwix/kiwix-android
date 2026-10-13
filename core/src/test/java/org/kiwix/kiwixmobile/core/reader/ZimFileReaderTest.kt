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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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

  @Test
  fun checkDecodeUrlWithEncodedSpaces() {
    val encoded = "A/Hello%20World"
    assertEquals("A/Hello World", encoded.decodeUrl)
  }

  @Test
  fun checkDecodeUrlWithAlreadyDecodedUrl() {
    val plain = "A/Simple"
    assertEquals("A/Simple", plain.decodeUrl)
  }

  @Test
  fun checkDecodeUrlWithPercentSign() {
    val urlWithPercent = "A/FT%"
    assertEquals("A/FT%", urlWithPercent.decodeUrl)
  }

  @Test
  fun checkDecodeUrlWithMultipleEncodings() {
    val encoded = "A/Test%20Page%3Fquery%3Dvalue"
    assertEquals("A/Test Page?query=value", encoded.decodeUrl)
  }

  @Test
  fun checkDecodeUrlKeepsPlusSign() {
    assertEquals("4+4_Photocycloaddition", "4+4_Photocycloaddition".decodeUrl)
    assertEquals("C++", "C++".decodeUrl)
  }

  @Test
  fun checkDecodeUrlWithEncodedPlusSign() {
    assertEquals("C++", "C%2B%2B".decodeUrl)
  }

  @Test
  fun checkDecodeUrlKeepsPlusSignWithOtherEncodings() {
    assertEquals("A/A+B testing?q=1+1", "A/A+B%20testing%3Fq%3D1+1".decodeUrl)
  }

  @Test
  fun checkDecodeUrlKeepsPlusSignWithPercentSign() {
    assertEquals("A/FT%+", "A/FT%+".decodeUrl)
  }

  @Test
  fun checkAddContentPrefixWhenNotPresent() {
    val path = "A/index.html"
    assertEquals("https://kiwix.app/A/index.html", path.addContentPrefix)
  }

  @Test
  fun checkAddContentPrefixWhenAlreadyPresent() {
    val url = "https://kiwix.app/A/index.html"
    assertEquals("https://kiwix.app/A/index.html", url.addContentPrefix)
  }

  @Test
  fun checkAddContentPrefixWithEmptyString() {
    val empty = ""
    assertEquals("https://kiwix.app/", empty.addContentPrefix)
  }
}
