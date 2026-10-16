/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer

class ZimReaderContainerUtilsTest {
  private lateinit var container: ZimReaderContainer

  private data class TitleToEntryPathScenario(
    val description: String,
    val title: String,
    val lookupResult: String?,
    val expectedResult: String?,
    val callsLookup: Boolean
  )

  private data class UrlSuffixScenario(
    val description: String,
    val suffix: String,
    val contentUrl: String,
    val isRedirect: Boolean,
    val redirectUrl: String? = null
  ) {
    val expectedResult: String get() = if (isRedirect) redirectUrl!! else contentUrl
  }

  @BeforeEach
  fun setup() {
    container = mockk<ZimReaderContainer>()
  }

  @Test
  fun titleToEntryPathScenarios() {
    val scenarios = listOf(
      TitleToEntryPathScenario(
        description = "a title that already looks like an entry path is returned unchanged" +
          " and skips the lookup",
        title = "A/Kiwix",
        lookupResult = null,
        expectedResult = "A/Kiwix",
        callsLookup = false
      ),
      TitleToEntryPathScenario(
        description = "a plain title resolves through the entry-path lookup",
        title = "Kiwix",
        lookupResult = "A/Kiwix",
        expectedResult = "A/Kiwix",
        callsLookup = true
      ),
      TitleToEntryPathScenario(
        description = "a title with no matching article resolves to null",
        title = "Unknown",
        lookupResult = null,
        expectedResult = null,
        callsLookup = true
      ),
      TitleToEntryPathScenario(
        description = "an empty title still delegates to the lookup",
        title = "",
        lookupResult = null,
        expectedResult = null,
        callsLookup = true
      )
    )

    scenarios.forEach { scenario ->
      if (scenario.callsLookup) {
        every { container.getPageEntryPathFromTitle(scenario.title) } returns scenario.lookupResult
      }

      val result = container.titleToEntryPath(scenario.title)

      assertEquals(scenario.expectedResult, result, scenario.description)
      verify(exactly = if (scenario.callsLookup) 1 else 0) {
        container.getPageEntryPathFromTitle(scenario.title)
      }
    }
  }

  @Test
  fun urlSuffixToParsableUrlScenarios() {
    val scenarios = listOf(
      UrlSuffixScenario(
        description = "a suffix that is not a redirect is returned as its content url",
        suffix = "A/Kiwix",
        contentUrl = "${ZimFileReader.CONTENT_PREFIX}A/Kiwix",
        isRedirect = false
      ),
      UrlSuffixScenario(
        description = "a suffix that redirects resolves to the redirect's url",
        suffix = "A/OldKiwix",
        contentUrl = "${ZimFileReader.CONTENT_PREFIX}A/OldKiwix",
        isRedirect = true,
        redirectUrl = "${ZimFileReader.CONTENT_PREFIX}A/KiwixAndroid"
      ),
      UrlSuffixScenario(
        description = "unicode characters in the suffix are percent-encoded in the content url",
        suffix = "A/Café_日本",
        contentUrl = "${ZimFileReader.CONTENT_PREFIX}A/Caf%C3%A9_%E6%97%A5%E6%9C%AC",
        isRedirect = false
      ),
      UrlSuffixScenario(
        description = "special characters ('+', '?', parentheses) in the suffix are" +
          " percent-encoded in the content url",
        suffix = "A/Is_C++_(programming_language)_hard?",
        contentUrl = "${ZimFileReader.CONTENT_PREFIX}A/Is_C%2B%2B_(programming_language)_hard%3F",
        isRedirect = false
      ),
      UrlSuffixScenario(
        description = "a suffix with a literal '+' looks up the redirect of its encoded url",
        suffix = "A/C++",
        contentUrl = "${ZimFileReader.CONTENT_PREFIX}A/C%2B%2B",
        isRedirect = true,
        redirectUrl = "${ZimFileReader.CONTENT_PREFIX}A/C%2B%2B_(programming_language)"
      )
    )

    scenarios.forEach { scenario ->
      every { container.isRedirect(scenario.contentUrl) } returns scenario.isRedirect
      if (scenario.isRedirect) {
        every { container.getRedirect(scenario.contentUrl) } returns scenario.redirectUrl!!
      }

      val result = container.urlSuffixToParsableUrl(scenario.suffix)

      assertEquals(scenario.expectedResult, result, scenario.description)
      if (scenario.isRedirect) {
        verify { container.getRedirect(scenario.contentUrl) }
      } else {
        verify(exactly = 0) { container.getRedirect(scenario.contentUrl) }
      }
    }
  }
}
