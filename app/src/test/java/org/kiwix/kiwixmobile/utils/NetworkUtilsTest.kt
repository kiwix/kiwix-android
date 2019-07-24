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

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.R
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.regex.Pattern

class NetworkUtilsTest {

  private val context: Context = mockk()
  private val connectivity: ConnectivityManager = mockk()
  private val networkInfo: NetworkInfo = mockk()
  private val networkInfo1: NetworkInfo = mockk()
  private val networkInfo2: NetworkInfo = mockk()

  @Test
  fun testNetworkAvailability() {
    val networkInfos = arrayOf(networkInfo1, networkInfo2)
    every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivity
    every { connectivity.allNetworkInfo } returns networkInfos

    // one network is connected
    every { (networkInfo1.state) } returns NetworkInfo.State.CONNECTED
    every { (networkInfo2.state) } returns NetworkInfo.State.DISCONNECTING
    assertEquals(true, NetworkUtils.isNetworkAvailable(context))

    every { (networkInfo1.state) } returns NetworkInfo.State.DISCONNECTING
    every { (networkInfo2.state) } returns NetworkInfo.State.CONNECTING
    assertEquals(false, NetworkUtils.isNetworkAvailable(context))

    // no network is available
    every { (networkInfo1.state) } returns NetworkInfo.State.DISCONNECTED
    every { (networkInfo2.state) } returns NetworkInfo.State.DISCONNECTED
    assertEquals(false, NetworkUtils.isNetworkAvailable(context))
  }

  @Test
  fun test_isNetworkConnectionOK() {
    every { (networkInfo2.state) } returns NetworkInfo.State.CONNECTING
    assertFalse(NetworkUtils.isNetworkConnectionOK(networkInfo2))

    every { (networkInfo2.state) } returns NetworkInfo.State.CONNECTED
    assertTrue(NetworkUtils.isNetworkConnectionOK(networkInfo2))
  }

  @Test
  fun testWifiAvailability() {
    every { (context.getSystemService(Context.CONNECTIVITY_SERVICE)) } returns connectivity
    every { (connectivity.activeNetworkInfo) } returns networkInfo

    // SDK >= 23
    try {
      setSDKVersion(Build.VERSION::class.java.getField("SDK_INT"), 23)
    } catch (e: Exception) {
      Log.d("NetworkUtilsTest", "Unable to set Build SDK Version")
    }

    // on Mobile Data
    every { (networkInfo.type) } returns ConnectivityManager.TYPE_MOBILE
    assertEquals(false, NetworkUtils.isWiFi(context))
    // verify that the correct methods are used according to the build SDK version
    verify {
      connectivity.activeNetworkInfo
      networkInfo.type
    }
    verify(exactly = 0) { connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI) }

    // on WIFI connected
    every { (networkInfo.type) } returns ConnectivityManager.TYPE_WIFI
    every { (networkInfo.isConnected) } returns java.lang.Boolean.TRUE
    assertEquals(true, NetworkUtils.isWiFi(context))
    verify(exactly = 2) { connectivity.activeNetworkInfo }
    verify(exactly = 0) { connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI) }

    // on WIFI disconnected
    every { (networkInfo.type) } returns ConnectivityManager.TYPE_WIFI
    every { (networkInfo.isConnected) } returns java.lang.Boolean.FALSE
    assertEquals(false, NetworkUtils.isWiFi(context))
    verify(exactly = 3) { connectivity.activeNetworkInfo }
    verify(exactly = 0) { connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI) }

    // SDK < 23
    try {
      setSDKVersion(Build.VERSION::class.java.getField("SDK_INT"), 22)
    } catch (e: Exception) {
      Log.d("NetworkUtilsTest", "Unable to set Build SDK Version")
    }

    // WIFI connected
    every { (connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI)) } returns networkInfo
    every { (networkInfo.isConnected) } returns true
    assertEquals(true, NetworkUtils.isWiFi(context))
    verify { connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI) }

    // WIFI disconnected
    every { (connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI)) } returns networkInfo
    every { (networkInfo.isConnected) } returns false
    assertEquals(false, NetworkUtils.isWiFi(context))
    verify(exactly = 2) { connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI) }
  }

  @Test
  fun testFilenameFromUrl() {
    // TODO: find a way to assert regex matching via JUnit and rewrite the test

    val defaultUUIDRegex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
    val pattern = Pattern.compile(defaultUUIDRegex)

    // URL is an Empty String
    var matcher = pattern.matcher(NetworkUtils.getFileNameFromUrl(""))
    if (!matcher.matches()) {
      assertEquals("filename doesn't match UUID regex (for empty string URL)", 0, 1)
    }

    // URL contains no '?' character but has '/' characters
    assertEquals(
      "File Name from URL (no '?' character)", "q=kiwix+android",
      NetworkUtils.getFileNameFromUrl("https://github.com/search/q=kiwix+android")
    )
    // and ends with a '/' character
    matcher = pattern.matcher(
      NetworkUtils.getFileNameFromUrl("https://github.com/search/q=kiwix+android/")
    )
    if (!matcher.matches()) {
      assertEquals("filename doesn't match UUID regex (for no '?' and '/' in end)", 0, 1)
    }

    // Empty string between last '?' and preceding '/'
    matcher = pattern.matcher(
      NetworkUtils.getFileNameFromUrl("https://github.com/search/?q=kiwix+android")
    )
    if (!matcher.matches()) {
      assertEquals("filename doesn't match UUID regex (for consecutive '/?')", 0, 1)
    }

    // Standard case
    // Here the Method should return the substring between the first '?' character and the nearest '/' character preceeding it
    assertEquals(
      "File Name from URL standard case", "search", NetworkUtils.getFileNameFromUrl(
        "https://www.google.com/search?source=hp&ei=zs4LW6W1E5n6rQH65Z-wDQ&q=" +
          "kiwix+android&oq=kiwix+android&gs_l=psy-ab.3..." +
          "2590.6259.0.6504.14.12.0.0.0.0.263.485.2-2.2.0...." +
          "0...1c.1.64.psy-ab..12.2.485.0..0j35i39k1.0.WSlGY7hWzTo"
      )
    )
    assertEquals(
      "File Name from URL standard case", "Special:Search",
      NetworkUtils.getFileNameFromUrl(
        "https://en.wikipedia.org/wiki/Special:Search?search=" +
          "kiwix+android&go=Go&searchToken=3zrcxw8fltdcij99zvoh5c6wy"
      )
    )
    assertEquals(
      "File Name from URL standard case", "search",
      NetworkUtils.getFileNameFromUrl("https://github.com/search?q=kiwix+android")
    )
  }

  @Test
  fun testParsedURL() {
    every { (context.getString(R.string.zim_nopic)) } returns "No Pictures"
    every { (context.getString(R.string.zim_novid)) } returns "No Videos"
    every { (context.getString(R.string.zim_simple)) } returns "Simple"

    assertEquals("URL Parsing on empty string", "", NetworkUtils.parseURL(context, ""))

    // Using the standard Kiwix Download URLs
    assertEquals(
      "URL Parsing", "No Pictures", NetworkUtils.parseURL(
        context,
        "http://ftpmirror.your.org/pub/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"
      )
    )
    assertEquals(
      "URL Parsing", "No Videos", NetworkUtils.parseURL(
        context,
        "http://www.mirrorservice.org/sites/download.kiwix.org/zim/wikipedia/" +
          "wikipedia_af_all_novid_2016-05.zim"
      )
    )
    assertEquals(
      "URL Parsing", "Simple", NetworkUtils.parseURL(
        context,
        "http://download.wikimedia.org/kiwix/zim/wikipedia/wikipedia_af_all_simple_2016-05.zim"
      )
    )
    assertEquals(
      "URL Parsing", "No Pictures", NetworkUtils.parseURL(
        context,
        "http://mirror.netcologne.de/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"
      )
    )
    assertEquals(
      "URL Parsing", "Simple", NetworkUtils.parseURL(
        context,
        "http://mirror3.kiwix.org/zim/wikipedia/wikipedia_af_all_simple_2016-05.zim"
      )
    )
  }

  // Sets the Build SDK version
  @Throws(Exception::class)
  private fun setSDKVersion(
    field: Field,
    newValue: Any
  ) {
    field.isAccessible = true
    val modifiersField = Field::class.java.getDeclaredField("modifiers")
    modifiersField.isAccessible = true
    modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
    field.set(null, newValue)
  }
}
