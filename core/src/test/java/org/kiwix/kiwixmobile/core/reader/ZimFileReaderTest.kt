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
  inner class ContentUrlTest {
    @Test
    fun entryPathsSurviveTheRoundTripThroughContentUrls() {
      listOf(
        "A/Earth",
        "Mercury_(planet)",
        "C++",
        "4+4_Photocycloaddition",
        "Is_0_even?",
        "C#",
        "100%",
        "%_(v/v)",
        "50%25",
        "Rock_&_Roll",
        "C=O",
        "Industrial_&_Engineering_Chemistry;_Analytical_Edition",
        "Hello World",
        "Café",
        "Ünïcödé_日本語_😀",
        "AC/DC",
        "it's_[a]_{b}",
        "@!$*,~:\"<>|\\^`",
        "_assets_/1%2C2-Dibromohexane.svg.png",
        "All +?#%&=;/ @é cases"
      ).forEach { path ->
        val url = path.addContentPrefix
        assertEquals(path, url.entryPath, "entry path of $url")
      }
    }

    @Test
    fun addContentPrefixEncodesOnlyCharactersThatAreNotAllowedInUrlPaths() {
      mapOf(
        "A/index.html" to "https://kiwix.app/A/index.html",
        "" to "https://kiwix.app/",
        "Mercury_(planet)" to "https://kiwix.app/Mercury_(planet)",
        "C++" to "https://kiwix.app/C%2B%2B",
        "Is_0_even?" to "https://kiwix.app/Is_0_even%3F",
        "C#" to "https://kiwix.app/C%23",
        "100%" to "https://kiwix.app/100%25",
        "Hello World" to "https://kiwix.app/Hello%20World",
        "Café" to "https://kiwix.app/Caf%C3%A9",
        "https://kiwix.app/A/index.html" to "https://kiwix.app/A/index.html",
        "https://kiwix.app/C%2B%2B" to "https://kiwix.app/C%2B%2B",
        // Only a prefix at the very start counts.
        "https://kiwix.app" to "https://kiwix.app/https://kiwix.app",
        "A/See_also_https://kiwix.app/page" to "https://kiwix.app/A/See_also_https://kiwix.app/page"
      ).forEach { (path, url) -> assertEquals(url, path.addContentPrefix, "content URL of $path") }
    }

    @Test
    fun entryPathIgnoresTheQueryAndTheFragment() {
      mapOf(
        "https://kiwix.app/A/Earth" to "A/Earth",
        "https://kiwix.app/C/d/search.htm?s=eczema" to "C/d/search.htm",
        "https://kiwix.app/Foo#History" to "Foo",
        "https://kiwix.app/C++?a=1+1#Syntax" to "C++",
        "https://kiwix.app/C%2B%2B" to "C++",
        "https://kiwix.app/Is_0_even%3F#top" to "Is_0_even?"
      ).forEach { (url, path) -> assertEquals(path, url.entryPath, "entry path of $url") }
    }

    @Test
    fun decodeUrlDecodesOnlyPercentEscapes() {
      mapOf(
        "C%2B%2B" to "C++",
        "Caf%C3%A9" to "Café",
        "Hello%20World" to "Hello World",
        "%F0%9F%98%80" to "😀",
        "50%2525" to "50%25",
        "A/Test%20Page%3Fquery%3Dvalue" to "A/Test Page?query=value",
        "A/50%25_off" to "A/50%_off",
        "a%2Bb+c" to "a+b+c",
        "" to "",
        // Not percent-encoded, e.g. URLs saved before entry paths were encoded: kept as they are.
        "C++" to "C++",
        "100%" to "100%",
        "A/FT%" to "A/FT%",
        "%_(v/v)" to "%_(v/v)",
        "a%zz" to "a%zz",
        "A/100%_Orange_Juice" to "A/100%_Orange_Juice",
        "a++b" to "a++b",
        "Café" to "Café"
      ).forEach { (url, decoded) -> assertEquals(decoded, url.decodeUrl, "decoded $url") }
    }
  }
}
