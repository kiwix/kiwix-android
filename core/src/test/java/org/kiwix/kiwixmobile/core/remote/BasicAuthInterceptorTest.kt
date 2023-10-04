/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.remote

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.kiwix.kiwixmobile.core.data.remote.isAuthenticationUrl
import org.kiwix.kiwixmobile.core.data.remote.secretKey

class BasicAuthInterceptorTest {

  private val authenticationUrl =
    "https://{{BASIC_AUTH_KEY}}@www.dwds.de/kiwix/f/dwds_de_dictionary_nopic_2023-09-12.zim"

  private val normalUrl = "https://download.kiwix.org/zim/wikipedia_fr_test.zim"

  @Test
  fun checkUrlIsAuthenticationUrl() {
    assertEquals(true, authenticationUrl.isAuthenticationUrl)
  }

  @Test
  fun checkUrlIsNormalUrl() {
    assertEquals(false, normalUrl.isAuthenticationUrl)
  }

  @Test
  fun testGetSecretKeyFromAuthenticationUrl() {
    assertEquals("BASIC_AUTH_KEY", authenticationUrl.secretKey)
  }

  @Test
  fun testGetSecretKeyFromNormalUrl() {
    assertEquals("", normalUrl.secretKey)
  }

  @Test
  fun testEmptyUrl() {
    val emptyUrl = ""
    assertEquals(false, emptyUrl.isAuthenticationUrl)
    assertEquals("", emptyUrl.secretKey)
  }

  @Test
  fun testUrlWithoutAuthentication() {
    val urlWithoutAuth = "https://example.com/kiwix/f/somefile.zim"
    assertEquals(false, urlWithoutAuth.isAuthenticationUrl)
    assertEquals("", urlWithoutAuth.secretKey)
  }

  @Test
  fun testUrlWithDifferentPlaceholder() {
    val urlWithDifferentPlaceholder =
      "https://{{ANOTHER_AUTH_KEY}}@example.com/kiwix/f/somefile.zim"
    assertEquals(true, urlWithDifferentPlaceholder.isAuthenticationUrl)
    assertEquals("ANOTHER_AUTH_KEY", urlWithDifferentPlaceholder.secretKey)
  }

  @Test
  fun testUrlWithFormattingVariations() {
    val formattedUrl1 = "https:// {{ BASIC_AUTH_KEY } } @example.com/kiwix/f/somefile.zim"
    val formattedUrl2 = "https://{{BASIC_AUTH_KEY}}@example.com/kiwix/f/somefile.zim  "
    assertEquals(true, formattedUrl1.isAuthenticationUrl)
    assertEquals(true, formattedUrl2.isAuthenticationUrl)
    assertEquals("BASIC_AUTH_KEY", formattedUrl1.secretKey)
    assertEquals("BASIC_AUTH_KEY", formattedUrl2.secretKey)
  }

  @Test
  fun testUrlWithOtherExtensions() {
    val urlWithTxtExtension = "https://{{BASIC_AUTH_KEY}}@example.com/kiwix/f/somefile.txt"
    val urlWithHtmlExtension = "https://{{BASIC_AUTH_KEY}}@example.com/kiwix/f/somefile.html"
    assertEquals(false, urlWithTxtExtension.isAuthenticationUrl)
    assertEquals(false, urlWithHtmlExtension.isAuthenticationUrl)
    assertEquals("BASIC_AUTH_KEY", urlWithTxtExtension.secretKey)
    assertEquals("BASIC_AUTH_KEY", urlWithHtmlExtension.secretKey)
  }

  @Test
  fun testUrlWithMultiplePlaceholders() {
    val urlWithMultiplePlaceholders =
      "https://{{BASIC_AUTH_KEY}}@example.com/kiwix/f/{{ANOTHER_KEY}}.zim"
    assertEquals(true, urlWithMultiplePlaceholders.isAuthenticationUrl)
    assertEquals("BASIC_AUTH_KEY", urlWithMultiplePlaceholders.secretKey)
  }
}
