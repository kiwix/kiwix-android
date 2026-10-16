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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.data.remote.isAuthenticationUrl
import org.kiwix.kiwixmobile.core.data.remote.removeAuthenticationFromUrl
import org.kiwix.kiwixmobile.core.data.remote.secretKey

class BasicAuthInterceptorTest {
  private data class Scenario(
    val description: String,
    val url: String,
    val isAuthenticationUrl: Boolean,
    val secretKey: String,
    val removeAuthenticationFromUrl: String
  )

  @Test
  fun basicAuthUrlScenarios() {
    val scenarios = listOf(
      Scenario(
        description = "a normal authentication url",
        url = "https://{{BASIC_AUTH_KEY}}@www.dwds.de/kiwix/f/dwds_de_dictionary_nopic_2023-09-12.zim",
        isAuthenticationUrl = true,
        secretKey = "BASIC_AUTH_KEY",
        removeAuthenticationFromUrl =
          "https://www.dwds.de/kiwix/f/dwds_de_dictionary_nopic_2023-09-12.zim"
      ),
      Scenario(
        description = "a normal url without a placeholder",
        url = "https://download.kiwix.org/zim/wikipedia_fr_test.zim",
        isAuthenticationUrl = false,
        secretKey = "",
        removeAuthenticationFromUrl = "https://download.kiwix.org/zim/wikipedia_fr_test.zim"
      ),
      Scenario(
        description = "an empty url",
        url = "",
        isAuthenticationUrl = false,
        secretKey = "",
        removeAuthenticationFromUrl = ""
      ),
      Scenario(
        description = "a url without authentication",
        url = "https://example.com/kiwix/f/somefile.zim",
        isAuthenticationUrl = false,
        secretKey = "",
        removeAuthenticationFromUrl = "https://example.com/kiwix/f/somefile.zim"
      ),
      Scenario(
        description = "a url with a different placeholder name",
        url = "https://{{ANOTHER_AUTH_KEY}}@example.com/kiwix/f/somefile.zim",
        isAuthenticationUrl = true,
        secretKey = "ANOTHER_AUTH_KEY",
        removeAuthenticationFromUrl = "https://example.com/kiwix/f/somefile.zim"
      ),
      Scenario(
        description = "a url with extra spaces around the placeholder",
        url = "https:// {{ BASIC_AUTH_KEY }} @example.com/kiwix/f/somefile.zim",
        isAuthenticationUrl = true,
        secretKey = "BASIC_AUTH_KEY",
        removeAuthenticationFromUrl =
          "https:// {{ BASIC_AUTH_KEY }} @example.com/kiwix/f/somefile.zim"
      ),
      Scenario(
        description = "a url with trailing whitespace",
        url = "https://{{BASIC_AUTH_KEY}}@example.com/kiwix/f/somefile.zim  ",
        isAuthenticationUrl = true,
        secretKey = "BASIC_AUTH_KEY",
        removeAuthenticationFromUrl = "https://example.com/kiwix/f/somefile.zim"
      ),
      Scenario(
        description = "a non-zim (.txt) url, which is not an authentication url" +
          " but still exposes its secret key and strips the placeholder",
        url = "https://{{BASIC_AUTH_KEY}}@example.com/kiwix/f/somefile.txt",
        isAuthenticationUrl = false,
        secretKey = "BASIC_AUTH_KEY",
        removeAuthenticationFromUrl = "https://example.com/kiwix/f/somefile.txt"
      ),
      Scenario(
        description = "a non-zim (.html) url",
        url = "https://{{BASIC_AUTH_KEY}}@example.com/kiwix/f/somefile.html",
        isAuthenticationUrl = false,
        secretKey = "BASIC_AUTH_KEY",
        removeAuthenticationFromUrl = "https://example.com/kiwix/f/somefile.html"
      ),
      Scenario(
        description = "a url with multiple placeholders, only the first is the secret key",
        url = "https://{{BASIC_AUTH_KEY}}@example.com/kiwix/f/{{ANOTHER_KEY}}.zim",
        isAuthenticationUrl = true,
        secretKey = "BASIC_AUTH_KEY",
        removeAuthenticationFromUrl = "https://example.com/kiwix/f/{{ANOTHER_KEY}}.zim"
      ),
      Scenario(
        description = "a percent-encoded url",
        url = "https%3A%2F%2F%7B%7BBASIC_AUTH_KEY%7D%7D%40www.dwds.de/kiwix/" +
          "dwds_de_dictionary_nopic.zim",
        isAuthenticationUrl = true,
        secretKey = "BASIC_AUTH_KEY",
        removeAuthenticationFromUrl = "https://www.dwds.de/kiwix/dwds_de_dictionary_nopic.zim"
      ),
      Scenario(
        // decodeUrl keeps a literal '+' as '+' instead of decoding it to a space (see
        // ZimFileReaderTest.decodeUrlDecodesOnlyPercentEscapes), so a '+' in the file name
        // of a download url must survive unchanged instead of becoming a space.
        description = "a url with a literal '+' in the file name",
        url = "https://{{BASIC_AUTH_KEY}}@example.com/kiwix/f/some+file+name.zim",
        isAuthenticationUrl = true,
        secretKey = "BASIC_AUTH_KEY",
        removeAuthenticationFromUrl = "https://example.com/kiwix/f/some+file+name.zim"
      )
    )

    scenarios.forEach { scenario ->
      assertEquals(
        scenario.isAuthenticationUrl,
        scenario.url.isAuthenticationUrl,
        "isAuthenticationUrl for ${scenario.description}"
      )
      assertEquals(
        scenario.secretKey,
        scenario.url.secretKey,
        "secretKey for ${scenario.description}"
      )
      assertEquals(
        scenario.removeAuthenticationFromUrl,
        scenario.url.removeAuthenticationFromUrl,
        "removeAuthenticationFromUrl for ${scenario.description}"
      )
    }
  }
}
