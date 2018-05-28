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
import static org.mockito.Mockito.when;

import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(MockitoJUnitRunner.class)
public class NetworkUtilsTest {

  @Mock Context context;

  //TODO : Add tests for checking network availability

  //TODO : Add tests for checking wifi connectivity

  //test that the file name returned for a given url is correct
  @Test
  public void testingFilenameFromUrl() {
    //test that the uuid matches the default UUID Regex.
    //TODO: find a way to assert regex matching via JUnit and rewrite the test
    String defaultUUIDRegex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
    Pattern pattern = Pattern.compile(defaultUUIDRegex);
    Matcher matcher = pattern.matcher(NetworkUtils.getFileNameFromUrl(""));

    if(matcher.matches()) {
      assertEquals("default filename doesn't match UUID regex",0,0);
    } else {
      assertEquals("default filename doesn't match UUID regex",0,1);
    }
    //TODO: add more test cases
  }

  //Test the parsed URL
  @Test
  public void testingParsedUrl() {
    when(context.getString(R.string.zim_nopic)).thenReturn(Integer.toString(R.string.zim_nopic));
    when(context.getString(R.string.zim_novid)).thenReturn(Integer.toString(R.string.zim_novid));
    when(context.getString(R.string.zim_simple)).thenReturn(Integer.toString(R.string.zim_simple));

    assertEquals("empty string", "", NetworkUtils.parseURL(context, ""));
    assertEquals("empty string", "2131624179", NetworkUtils.parseURL(context, "http://ftpmirror.your.org/pub/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"));
    assertEquals("empty string", "2131624180", NetworkUtils.parseURL(context, "http://www.mirrorservice.org/sites/download.kiwix.org/zim/wikipedia/wikipedia_af_all_novid_2016-05.zim"));
    assertEquals("empty string", "2131624181", NetworkUtils.parseURL(context, "http://download.wikimedia.org/kiwix/zim/wikipedia/wikipedia_af_all_simple_2016-05.zim"));
    assertEquals("empty string", "2131624179", NetworkUtils.parseURL(context, "http://mirror.netcologne.de/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"));
    assertEquals("empty string", "2131624181", NetworkUtils.parseURL(context, "http://mirror3.kiwix.org/zim/wikipedia/wikipedia_af_all_simple_2016-05.zim"));
  }
}
