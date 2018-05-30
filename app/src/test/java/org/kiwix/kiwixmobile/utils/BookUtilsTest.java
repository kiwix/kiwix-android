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

package org.kiwix.kiwixmobile.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BookUtilsTest {
  @Mock LanguageUtils.LanguageContainer container;

  /*
   * Test that the language returned for the given language code is correct
   */
  @Test
  public void testLanguageFromCode() {
    BookUtils t = new BookUtils(container);

    //testing trivial cases
    assertEquals("null is passed", "", t.getLanguage(null));
    assertEquals("empty string passed", "", t.getLanguage(""));
    assertEquals("code length more than 3", "", t.getLanguage("english"));

    //testing the hashmap created inside the BookUtils class
    assertEquals("code length equals 3 (English)", "English", t.getLanguage("eng"));
    assertEquals("code length equals 3 (Hindi)", "Hindi", t.getLanguage("hin"));
    assertEquals("code length equals 3 (French)", "French", t.getLanguage("fra"));
    assertEquals("code length equals 3 (Akan)", "Akan", t.getLanguage("aka"));
    assertEquals("code length equals 3 (Burmese)", "Burmese", t.getLanguage("mya"));
    assertEquals("code length equals 3 (Catalan)", "Catalan", t.getLanguage("cat"));

    //this case uses the result from the container nested class inside LanguageUtils. It will be tested in LanguageUtilsTest
    when(container.findLanguageName(anyString())).thenReturn(container);
    when(container.getLanguageName()).thenReturn("English");
    assertEquals("code length equals 2 (dummy)", "English", t.getLanguage("en"));
  }
}
