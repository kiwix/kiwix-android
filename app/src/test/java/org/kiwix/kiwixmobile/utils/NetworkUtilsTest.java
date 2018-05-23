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
import android.content.Context;
import org.junit.Test;
import org.mockito.Mock;

public class NetworkUtilsTest {

  @Mock Context context;


  @Test
  public void testingParsedUrl(){
    assertEquals("empty string", "", NetworkUtils.parseURL(context, ""));
    //TODO: add more test cases
  }

  @Test
  public void testingFilenameFromUrl(){
    //test that the uuid matches the default UUID Regex.
    //assertTrue(NetworkUtils.getFileNameFromUrl("").matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
    //TODO: find a way to assert regex matching via JUnit
    //TODO: add more test cases
  }
}
