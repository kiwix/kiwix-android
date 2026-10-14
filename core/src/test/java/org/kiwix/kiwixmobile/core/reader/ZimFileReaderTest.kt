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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ZimFileReaderTest {
  @Nested
  inner class MimeTypeTest {
    @Test
    fun checkTruncateMimeTypeHandlesEdgeCases() {
      assertEquals("text/html", "text/html;".truncateMimeType)
      assertEquals("text/css$1", "text/css^([^ ]+).*\$".truncateMimeType)
      assertEquals("text/application", "text/application;^([^ ]+).*\$;".truncateMimeType)
      assertEquals("", "".truncateMimeType)
      assertEquals("text/html", "text/html; charset=utf-8".truncateMimeType)
      assertEquals("image/png", "image/png".truncateMimeType)
    }
  }

  @Nested
  inner class UrlDecodingTest {
    @Test
    fun checkDecodeUrlPreservesLiteralPlusSigns() {
      assertEquals("4+4_Photocycloaddition", "4+4_Photocycloaddition".decodeUrl)
      assertEquals("C++", "C++".decodeUrl)
      assertEquals("C++", "C%2B%2B".decodeUrl)
      assertEquals("A/A+B testing?q=1+1", "A/A+B%20testing%3Fq%3D1+1".decodeUrl)
      assertEquals("A/FT%+", "A/FT%+".decodeUrl)
      assertEquals("a++b", "a++b".decodeUrl)
      assertEquals("a+b+c", "a%2Bb+c".decodeUrl)
    }

    @Test
    fun checkDecodeUrlHandlesPercentEncodingEdgeCases() {
      assertEquals("A/Hello World", "A/Hello%20World".decodeUrl)
      assertEquals("A/Simple", "A/Simple".decodeUrl)
      assertEquals("A/FT%", "A/FT%".decodeUrl)
      assertEquals("A/Test Page?query=value", "A/Test%20Page%3Fquery%3Dvalue".decodeUrl)
      assertEquals("café", "caf%C3%A9".decodeUrl)
      assertEquals("café", "café".decodeUrl)
      assertEquals("", "".decodeUrl)
      // malformed percent-encoding ("%_") fails to decode, so the original is returned unchanged
      assertEquals("A/100%_Orange_Juice", "A/100%_Orange_Juice".decodeUrl)
      // a correctly-escaped literal percent sign ("%25") still decodes successfully
      assertEquals("A/50%_off", "A/50%25_off".decodeUrl)
    }
  }

  @Nested
  inner class AddContentPrefixTest {
    @Test
    fun checkAddContentPrefixHandlesEdgeCases() {
      assertEquals(
        "${ZimFileReader.CONTENT_PREFIX}A/index.html",
        "A/index.html".addContentPrefix
      )
      assertEquals(
        "${ZimFileReader.CONTENT_PREFIX}A/index.html",
        "${ZimFileReader.CONTENT_PREFIX}A/index.html".addContentPrefix
      )
      assertEquals(ZimFileReader.CONTENT_PREFIX, "".addContentPrefix)
      assertEquals(ZimFileReader.CONTENT_PREFIX, ZimFileReader.CONTENT_PREFIX.addContentPrefix)
      assertEquals(
        "${ZimFileReader.CONTENT_PREFIX}A/4%2B4_Photocycloaddition",
        "A/4+4_Photocycloaddition".addContentPrefix
      )
      assertEquals(
        "${ZimFileReader.CONTENT_PREFIX}A/C%2B%2B",
        "${ZimFileReader.CONTENT_PREFIX}A/C++".addContentPrefix
      )
      assertEquals(
        "${ZimFileReader.CONTENT_PREFIX}%2Bleading_plus",
        "${ZimFileReader.CONTENT_PREFIX}+leading_plus".addContentPrefix
      )
      // Missing the trailing slash from CONTENT_PREFIX, so startsWith() fails
      // and the whole string is treated as a path to be prefixed again.
      val partialPrefix = ZimFileReader.CONTENT_PREFIX.dropLast(1)
      assertEquals(
        "${ZimFileReader.CONTENT_PREFIX}$partialPrefix",
        partialPrefix.addContentPrefix
      )
      // only a prefix at the very start counts; one appearing mid-string is left untouched
      val pathWithEmbeddedPrefix = "A/See_also_${ZimFileReader.CONTENT_PREFIX}page"
      assertEquals(
        "${ZimFileReader.CONTENT_PREFIX}$pathWithEmbeddedPrefix",
        pathWithEmbeddedPrefix.addContentPrefix
      )
    }
  }

  @Nested
  inner class EncodeLiteralPlusTest {
    @Test
    fun checkEncodeLiteralPlusHandlesEdgeCases() {
      assertEquals("a%2Bb", "a+b".encodeLiteralPlus())
      assertEquals("C%2B%2B(Programming Language)", "C++(Programming Language)".encodeLiteralPlus())
      assertEquals("A/Simple", "A/Simple".encodeLiteralPlus())
      assertEquals("", "".encodeLiteralPlus())
      assertEquals("a%2Bb", "a%2Bb".encodeLiteralPlus())
      assertEquals("%2BA%2B", "+A+".encodeLiteralPlus())
    }
  }
}
