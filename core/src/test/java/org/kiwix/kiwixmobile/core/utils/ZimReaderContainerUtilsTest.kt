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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer

class ZimReaderContainerUtilsTest {
  private lateinit var container: ZimReaderContainer
  private val articleUrl = "https://kiwix.app/A/Kiwix"
  private val redirectUrl = "https://kiwix.app/A/KiwixAndroid"

  @BeforeEach
  fun setup() {
    container = mockk<ZimReaderContainer>()
  }

  @Test
  fun titleToUrl_whenTitleStartsWithArticlePrefix_returnsSameTitle() {
    val result = container.titleToUrl("A/Kiwix")

    assertEquals(
      "A/Kiwix",
      result
    )

    verify(exactly = 0) {
      container.getPageUrlFromTitle(any())
    }
  }

  @Test
  fun titleToUrl_resolvesArticleTitle() {
    every {
      container.getPageUrlFromTitle("Kiwix")
    } returns "A/Kiwix"

    val result = container.titleToUrl("Kiwix")

    assertEquals(
      "A/Kiwix",
      result
    )

    verify {
      container.getPageUrlFromTitle("Kiwix")
    }
  }

  @Test
  fun titleToUrl_whenArticleDoesNotExist_returnsNull() {
    every {
      container.getPageUrlFromTitle("Unknown")
    } returns null

    val result = container.titleToUrl("Unknown")

    assertNull(result)
  }

  @Test
  fun titleToUrl_whenTitleIsEmpty_delegatesToPageUrlLookup() {
    every {
      container.getPageUrlFromTitle("")
    } returns null

    val result = container.titleToUrl("")

    assertNull(result)

    verify(exactly = 1) {
      container.getPageUrlFromTitle("")
    }
  }

  @Test
  fun urlSuffixToParsableUrl_whenNotRedirect_returnsOriginalUrl() {
    every {
      container.isRedirect(articleUrl)
    } returns false

    val result = container.urlSuffixToParsableUrl("A/Kiwix")

    assertEquals(
      articleUrl,
      result
    )

    verify {
      container.isRedirect(articleUrl)
    }
  }

  @Test
  fun urlSuffixToParsableUrl_whenIsRedirect_returnsRedirectUrl() {
    every {
      container.isRedirect(articleUrl)
    } returns true

    every {
      container.getRedirect(articleUrl)
    } returns redirectUrl

    val result = container.urlSuffixToParsableUrl("A/Kiwix")

    assertEquals(
      redirectUrl,
      result
    )

    verify {
      container.isRedirect(articleUrl)
      container.getRedirect(articleUrl)
    }
  }
}
