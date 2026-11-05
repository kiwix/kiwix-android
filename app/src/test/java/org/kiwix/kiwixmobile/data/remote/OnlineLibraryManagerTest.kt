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

package org.kiwix.kiwixmobile.data.remote

import android.os.Build
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.sharedFunctions.MainDispatcherRule
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class OnlineLibraryManagerTest {
  @Rule
  @JvmField
  val mainDispatcherRule = MainDispatcherRule()
  private val onlineLibraryManager = OnlineLibraryManager(mainDispatcherRule.dispatcher)

  @Test
  fun `buildLibraryUrl includes non-blank category`() {
    val url = onlineLibraryManager.buildLibraryUrl(
      "https://opds.library.kiwix.org",
      start = 0,
      count = 24,
      category = "wikipedia"
    )
    assertTrue(url.contains("category=wikipedia"))
  }

  @Test
  fun `buildLibraryUrl omits blank category so no category filter is sent`() {
    val url = onlineLibraryManager.buildLibraryUrl(
      "https://opds.library.kiwix.org",
      start = 0,
      count = 24,
      query = "minecraft",
      lang = "eng",
      category = ""
    )
    assertFalse(url.contains("category="))
    assertTrue(url.contains("q=minecraft"))
    assertTrue(url.contains("lang=eng"))
  }

  @Test
  fun `buildLibraryUrl omits whitespace-only category`() {
    val url = onlineLibraryManager.buildLibraryUrl(
      "https://opds.library.kiwix.org",
      start = 0,
      count = 24,
      category = "   "
    )
    assertFalse(url.contains("category="))
  }

  @Test
  fun `buildLibraryUrl omits blank query and blank lang`() {
    val url = onlineLibraryManager.buildLibraryUrl(
      "https://opds.library.kiwix.org",
      start = 0,
      count = 24,
      query = "   ",
      lang = ""
    )
    assertFalse(url.contains("q="))
    assertFalse(url.contains("lang="))
  }

  @Test
  fun `buildLibraryUrl omits all optional filters when none are provided`() {
    val url = onlineLibraryManager.buildLibraryUrl(
      "https://opds.library.kiwix.org",
      start = 0,
      count = 24
    )
    assertFalse(url.contains("q="))
    assertFalse(url.contains("lang="))
    assertFalse(url.contains("category="))
    assertTrue(url.contains("start=0"))
    assertTrue(url.contains("count=24"))
  }

  @Test
  fun `buildLibraryUrl includes every filter when all are provided`() {
    val url = onlineLibraryManager.buildLibraryUrl(
      "https://opds.library.kiwix.org",
      start = 100,
      count = 50,
      query = "minecraft",
      lang = "eng",
      category = "wikipedia"
    )
    assertTrue(url.contains("start=100"))
    assertTrue(url.contains("count=50"))
    assertTrue(url.contains("q=minecraft"))
    assertTrue(url.contains("lang=eng"))
    assertTrue(url.contains("category=wikipedia"))
  }

  @Test
  fun `buildLibraryUrl does not duplicate the slash when baseUrl already ends with one`() {
    val withSlash = onlineLibraryManager.buildLibraryUrl(
      "https://opds.library.kiwix.org/",
      start = 0,
      count = 24
    )
    val withoutSlash = onlineLibraryManager.buildLibraryUrl(
      "https://opds.library.kiwix.org",
      start = 0,
      count = 24
    )
    assertFalse(withSlash.contains("//v2"))
    assertEquals(withoutSlash, withSlash)
  }

  @Test
  fun `calculateTotalPages divides evenly when results are an exact multiple of the page size`() {
    assertEquals(2, onlineLibraryManager.calculateTotalPages(100, 50))
  }

  @Test
  fun `calculateTotalPages rounds up when results leave a remainder`() {
    assertEquals(3, onlineLibraryManager.calculateTotalPages(101, 50))
  }

  @Test
  fun `calculateTotalPages returns zero when there are no results`() {
    assertEquals(0, onlineLibraryManager.calculateTotalPages(0, 50))
  }

  @Test
  fun `calculateTotalPages returns one page when results are fewer than the page size`() {
    assertEquals(1, onlineLibraryManager.calculateTotalPages(10, 50))
  }

  @Test
  fun `getStartOffset returns zero for the first page`() {
    assertEquals(0, onlineLibraryManager.getStartOffset(0, 50))
  }

  @Test
  fun `getStartOffset multiplies page index by page size for later pages`() {
    assertEquals(150, onlineLibraryManager.getStartOffset(3, 50))
  }

  @Test
  fun `parseOPDSStreamAndGetBooks returns null when content is null`() = runTest {
    val result = onlineLibraryManager.parseOPDSStreamAndGetBooks(null, HOST)

    assertNull(result)
    assertEquals(ZERO, onlineLibraryManager.totalResult)
  }

  @Test
  fun `parseOPDSStreamAndGetBooks swallows malformed XML and returns null`() = runTest {
    val result = onlineLibraryManager.parseOPDSStreamAndGetBooks("not valid xml <<<", HOST)

    assertNull(result)
    assertEquals(ZERO, onlineLibraryManager.totalResult)
  }

  @Test
  fun `parseOPDSStreamAndGetBooks extracts totalResults before failing to parse books`() = runTest {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <feed xmlns="http://www.w3.org/2005/Atom">
        <totalResults>5</totalResults>
      </feed>
    """.trimIndent()

    val result = onlineLibraryManager.parseOPDSStreamAndGetBooks(xml, HOST)

    assertNull(result)
    assertEquals(5, onlineLibraryManager.totalResult)
  }

  @Test
  fun `parseOPDSStreamAndGetBooks defaults totalResults to zero when tag is missing`() = runTest {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <feed xmlns="http://www.w3.org/2005/Atom" />
    """.trimIndent()

    val result = onlineLibraryManager.parseOPDSStreamAndGetBooks(xml, HOST)

    assertNull(result)
    assertEquals(ZERO, onlineLibraryManager.totalResult)
  }

  private companion object {
    private const val HOST = "https://opds.library.kiwix.org"
  }
}
