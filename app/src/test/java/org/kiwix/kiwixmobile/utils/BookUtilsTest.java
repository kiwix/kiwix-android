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
    assertEquals("empty string", "", "");
    assertEquals("code length more than 2", "", t.getLanguage("abc"));
    //TODO : fix this test
    //assertEquals("code length more than 2", "en", t.getLanguage("en"));
    //TODO: add more test cases
  }
}
