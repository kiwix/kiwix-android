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
    // This test previously pinned a bug: truncateMimeType used String.replace() with a
    // regex-pattern string, which does a literal (non-regex) substring replacement - it
    // happened to match this exact input and silently mangled it into "text/css$1". The
    // input contains a literal space (inside "[^ ]"), so truncation now correctly stops
    // there, same as it would for any real "type/subtype extra-junk" mimetype. See #5070.
    val mimeTypeCss = "text/css^([^ ]+).*\$"
    assertEquals("text/css^([^", mimeTypeCss.truncateMimeType)
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
  fun checkReplaceWithEncodedStringReplacesQuestionMark() {
    val url = "A/page?query=value"
    assertEquals("A/page%3Fquery=value", url.replaceWithEncodedString)
  }

  @Test
  fun checkReplaceWithEncodedStringWithoutQuestionMark() {
    val url = "A/simple_page"
    assertEquals("A/simple_page", url.replaceWithEncodedString)
  }

  @Test
  fun checkReplaceWithEncodedStringWithMultipleQuestionMarks() {
    val url = "A/page?a=1?b=2"
    assertEquals("A/page%3Fa=1%3Fb=2", url.replaceWithEncodedString)
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
