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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(MockitoJUnitRunner.class)
public class NetworkUtilsTest {

  @Mock private Context context;
  @Mock private ConnectivityManager connectivity;
  @Mock private NetworkInfo networkInfo;
  @Mock private NetworkInfo networkInfo1;
  @Mock private NetworkInfo networkInfo2;

  public void testNetworkAvailability() {
    NetworkInfo[] info = {networkInfo1, networkInfo2};
    when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivity);
    when(connectivity.getAllNetworkInfo()).thenReturn(info);

    //one network is connected
    when(networkInfo1.getState()).thenReturn(NetworkInfo.State.CONNECTED);
    when(networkInfo2.getState()).thenReturn(NetworkInfo.State.DISCONNECTING);
      assertTrue("", NetworkUtils.isNetworkAvailable(context));

    //one network is connecting
    when(networkInfo1.getState()).thenReturn(NetworkInfo.State.DISCONNECTING);
    when(networkInfo2.getState()).thenReturn(NetworkInfo.State.CONNECTING);
      assertTrue("", NetworkUtils.isNetworkAvailable(context));

    //no network is available
    when(networkInfo1.getState()).thenReturn(NetworkInfo.State.DISCONNECTED);
    when(networkInfo2.getState()).thenReturn(NetworkInfo.State.DISCONNECTED);
      assertFalse("", NetworkUtils.isNetworkAvailable(context));
  }

  public void testWifiAvailability(){
    when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivity);
    when(connectivity.getActiveNetworkInfo()).thenReturn(networkInfo);

    //SDK >= 23
    try{
      SetSDKVersion(Build.VERSION.class.getField("SDK_INT"), 23);
    }catch (Exception e){
      Log.d("NetworkUtilsTest", "Unable to set Build SDK Version");
    }

    //on Mobile Data
    when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
      assertFalse(NetworkUtils.isWiFi(context));
    //verify that the correct methods are used according to build SDK version
    verify(connectivity).getActiveNetworkInfo();
    verify(networkInfo).getType();
    verify(connectivity, never()).getNetworkInfo(ConnectivityManager.TYPE_WIFI);

    //on WIFI
    when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_WIFI);
      assertTrue(NetworkUtils.isWiFi(context));
    verify(connectivity).getActiveNetworkInfo();
    verify(connectivity, never()).getNetworkInfo(ConnectivityManager.TYPE_WIFI);

    //SDK < 23
    try{
      SetSDKVersion(Build.VERSION.class.getField("SDK_INT"), 22);
    }catch (Exception e){
      Log.d("NetworkUtilsTest", "Unable to set Build SDK Version");
    }

    //WIFI connected
    when(connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI)).thenReturn(networkInfo);
    when(networkInfo.isConnected()).thenReturn(true);
      assertTrue(NetworkUtils.isWiFi(context));
    verify(connectivity).getNetworkInfo(ConnectivityManager.TYPE_WIFI);

    //WIFI disconnected
    when(connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI)).thenReturn(networkInfo);
    when(networkInfo.isConnected()).thenReturn(false);
      assertFalse(NetworkUtils.isWiFi(context));
    verify(connectivity).getNetworkInfo(ConnectivityManager.TYPE_WIFI);
  }

  @Test
  public void testFilenameFromUrl() {
    // TODO: find a way to assert regex matching via JUnit and rewrite the test

    String defaultUUIDRegex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
    Pattern pattern = Pattern.compile(defaultUUIDRegex);

    // URL is an Empty String
    Matcher matcher = pattern.matcher(NetworkUtils.getFileNameFromUrl(""));
    if(!matcher.matches()) assertEquals("filename doesn't match UUID regex (for empty string URL)", 0, 1);

    // URL contains no '?' character but has '/' characters
    assertEquals("File Name from URL (no '?' character)", "q=kiwix+android", NetworkUtils.getFileNameFromUrl("https://github.com/search/q=kiwix+android"));
    // and ends with a '/' character
    matcher = pattern.matcher(NetworkUtils.getFileNameFromUrl("https://github.com/search/q=kiwix+android/"));
    if(!matcher.matches()) assertEquals("filename doesn't match UUID regex (for no '?' and '/' in end)", 0, 1);

    // Empty string between last '?' and preceding '/'
    matcher = pattern.matcher(NetworkUtils.getFileNameFromUrl("https://github.com/search/?q=kiwix+android"));
    if(!matcher.matches()) assertEquals("filename doesn't match UUID regex (for consecutive '/?')", 0, 1);

    // Standard case
    // Here the Method should return the substring between the first '?' character and the nearest '/' character preceeding it
    assertEquals("File Name from URL standard case", "search", NetworkUtils.getFileNameFromUrl("https://www.google.com/search?source=hp&ei=zs4LW6W1E5n6rQH65Z-wDQ&q=kiwix+android&oq=kiwix+android&gs_l=psy-ab.3...2590.6259.0.6504.14.12.0.0.0.0.263.485.2-2.2.0....0...1c.1.64.psy-ab..12.2.485.0..0j35i39k1.0.WSlGY7hWzTo"));
    assertEquals("File Name from URL standard case", "Special:Search", NetworkUtils.getFileNameFromUrl("https://en.wikipedia.org/wiki/Special:Search?search=kiwix+android&go=Go&searchToken=3zrcxw8fltdcij99zvoh5c6wy"));
    assertEquals("File Name from URL standard case", "search", NetworkUtils.getFileNameFromUrl("https://github.com/search?q=kiwix+android"));
  }

  @Test
  public void testParsedURL() {
    when(context.getString(R.string.zim_nopic)).thenReturn("No Pictures");
    when(context.getString(R.string.zim_novid)).thenReturn("No Videos");
    when(context.getString(R.string.zim_simple)).thenReturn("Simple");

    assertEquals("URL Parsing on empty string", "", NetworkUtils.parseURL(context, ""));

    //Using the standard Kiwix Download URLs
    assertEquals("URL Parsing", "No Pictures", NetworkUtils.parseURL(context, "http://ftpmirror.your.org/pub/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"));
    assertEquals("URL Parsing", "No Videos", NetworkUtils.parseURL(context, "http://www.mirrorservice.org/sites/download.kiwix.org/zim/wikipedia/wikipedia_af_all_novid_2016-05.zim"));
    assertEquals("URL Parsing", "Simple", NetworkUtils.parseURL(context, "http://download.wikimedia.org/kiwix/zim/wikipedia/wikipedia_af_all_simple_2016-05.zim"));
    assertEquals("URL Parsing", "No Pictures", NetworkUtils.parseURL(context, "http://mirror.netcologne.de/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"));
    assertEquals("URL Parsing", "Simple", NetworkUtils.parseURL(context, "http://mirror3.kiwix.org/zim/wikipedia/wikipedia_af_all_simple_2016-05.zim"));
  }

  //Sets the Build SDK version
  private static void SetSDKVersion(Field field, Object newValue) throws Exception {
    field.setAccessible(true);
    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    field.set(null, newValue);
  }
}
