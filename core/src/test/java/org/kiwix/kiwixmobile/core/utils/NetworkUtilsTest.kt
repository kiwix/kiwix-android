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

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.compat.CompatV25
import java.util.regex.Pattern

class NetworkUtilsTest {

  private val context: Context = mockk()
  private val connectivity: ConnectivityManager = mockk()
  private val networkCapabilities: NetworkCapabilities = mockk()

  @Test
  fun testNetworkAvailability_CompatV25() {
    every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivity
    val compatV25 = CompatV25()
    val network: Network = mockk()

    every { connectivity.activeNetwork } returns network
    every { connectivity.getNetworkCapabilities(network) } returns networkCapabilities
    every { networkCapabilities.hasCapability(NET_CAPABILITY_INTERNET) } returns true

    assertTrue(compatV25.isNetworkAvailable(connectivity))
    every { networkCapabilities.hasCapability(NET_CAPABILITY_INTERNET) } returns false
    assertFalse(compatV25.isNetworkAvailable(connectivity))
  }

  @Test
  fun test_isWifi_CompatV25() {
    val compatV25 = CompatV25()
    val network: Network = mockk()

    every { connectivity.activeNetwork } returns network
    every { connectivity.getNetworkCapabilities(network) } returns networkCapabilities
    every { networkCapabilities.hasTransport(TRANSPORT_WIFI) } returns true

    assertTrue(compatV25.isWifi(connectivity))
    every { networkCapabilities.hasTransport(TRANSPORT_WIFI) } returns false
    assertFalse(compatV25.isWifi(connectivity))
  }

  @Test
  fun testNetworkAvailability_NoNetwork_CompatV25() {
    every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivity
    val compatV25 = CompatV25()
    val network: Network = mockk()

    every { connectivity.activeNetwork } returns network
    every { connectivity.getNetworkCapabilities(network) } returns networkCapabilities
    every { networkCapabilities.hasCapability(NET_CAPABILITY_INTERNET) } returns false

    assertFalse(compatV25.isNetworkAvailable(connectivity))
  }

  @Test
  fun testFilenameFromUrl() {
    // TODO find a way to assert regex matching via JUnit and rewrite the test

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
      "File Name from URL standard case", "search",
      NetworkUtils.getFileNameFromUrl(
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
    every { context.getString(R.string.zim_no_pic) } returns "No Pictures"
    every { context.getString(R.string.zim_no_vid) } returns "No Videos"
    every { context.getString(R.string.zim_simple) } returns "Simple"

    assertEquals(
      "URL Parsing on empty string", "",
      NetworkUtils.parseURL(context, "")
    )

    // Using the standard Kiwix Download URLs
    assertEquals(
      "URL Parsing", "No Pictures",
      NetworkUtils.parseURL(
        context,
        "http://ftpmirror.your.org/pub/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"
      )
    )
    assertEquals(
      "URL Parsing", "No Videos",
      NetworkUtils.parseURL(
        context,
        "http://www.mirrorservice.org/sites/download.kiwix.org/zim/wikipedia/" +
          "wikipedia_af_all_novid_2016-05.zim"
      )
    )
    assertEquals(
      "URL Parsing", "Simple",
      NetworkUtils.parseURL(
        context,
        "http://download.wikimedia.org/kiwix/zim/wikipedia/wikipedia_af_all_simple_2016-05.zim"
      )
    )
    assertEquals(
      "URL Parsing", "No Pictures",
      NetworkUtils.parseURL(
        context,
        "http://mirror.netcologne.de/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"
      )
    )
    assertEquals(
      "URL Parsing", "Simple",
      NetworkUtils.parseURL(
        context,
        "http://mirror3.kiwix.org/zim/wikipedia/wikipedia_af_all_simple_2016-05.zim"
      )
    )
  }
}
