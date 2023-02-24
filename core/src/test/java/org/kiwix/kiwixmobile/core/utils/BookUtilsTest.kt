/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.utils

import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test

class BookUtilsTest {

  // Test that the language returned for the given language code is correct
  @Test
  fun testLanguageFromCode() {
    val t = BookUtils()

    // testing trivial cases
    assertEquals("null is passed", "", t.getLanguage(null))
    assertEquals("empty string passed", "", t.getLanguage(""))
    assertEquals("code length more than 3", "", t.getLanguage("english"))

    // testing the hashmap created inside the BookUtils class
    assertEquals("code length equals 3 (English)", "English (India)", t.getLanguage("eng"))
    assertEquals("code length equals 3 (Hindi)", "Hindi (India)", t.getLanguage("hin"))
    assertEquals("code length equals 3 (French)", "French (Rwanda)", t.getLanguage("fra"))
    assertEquals("code length equals 3 (Akan)", "Akan (Ghana)", t.getLanguage("aka"))
    assertEquals(
      "code length equals 3 (Burmese)",
      "Burmese (Myanmar (Burma))",
      t.getLanguage("mya")
    )
    assertEquals("code length equals 3 (Catalan)", "Catalan (Spain)", t.getLanguage("cat"))

    // this case uses the result from the container nested class inside LanguageUtils. It will be tested in LanguageUtilsTest
    assertEquals("code length equals 2 (dummy)", "English", t.getLanguage("en"))
  }
}
