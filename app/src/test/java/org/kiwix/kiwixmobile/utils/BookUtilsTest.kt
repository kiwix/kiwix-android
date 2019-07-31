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

package org.kiwix.kiwixmobile.utils

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test

class BookUtilsTest {
  private val container: LanguageUtils.LanguageContainer = mockk()

  // Test that the language returned for the given language code is correct
  @Test
  fun testLanguageFromCode() {
    val t = BookUtils(container)

    // testing trivial cases
    assertEquals("null is passed", "", t.getLanguage(null))
    assertEquals("empty string passed", "", t.getLanguage(""))
    assertEquals("code length more than 3", "", t.getLanguage("english"))

    // testing the hashmap created inside the BookUtils class
    assertEquals("code length equals 3 (English)", "English", t.getLanguage("eng"))
    assertEquals("code length equals 3 (Hindi)", "Hindi", t.getLanguage("hin"))
    assertEquals("code length equals 3 (French)", "French", t.getLanguage("fra"))
    assertEquals("code length equals 3 (Akan)", "Akan", t.getLanguage("aka"))
    assertEquals("code length equals 3 (Burmese)", "Burmese", t.getLanguage("mya"))
    assertEquals("code length equals 3 (Catalan)", "Catalan", t.getLanguage("cat"))

    // this case uses the result from the container nested class inside LanguageUtils. It will be tested in LanguageUtilsTest
    every { container.findLanguageName(any()) } returns container
    every { container.languageName } returns "English"
    assertEquals("code length equals 2 (dummy)", "English", t.getLanguage("en"))
  }
}
