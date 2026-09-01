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

package org.kiwix.kiwixmobile.core.main.reader.helper.intent

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.main.CoreSearchWidget
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class PendingIntentParserTest {
  private lateinit var parser: PendingIntentParser

  @Before
  fun setup() {
    parser = PendingIntentParser()
  }

  @Test
  fun `ACTION_PROCESS_TEXT returns OpenSearch`() {
    val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
      putExtra(Intent.EXTRA_PROCESS_TEXT, "hello")
    }

    val result = parser.parse(intent)

    assertEquals(
      PendingIntentParser.ReaderIntentAction.OpenSearch(
        query = "hello",
        isVoice = false,
        isOpenedFromTabView = false
      ),
      result
    )
  }

  @Test
  fun `TEXT_CLICKED returns empty OpenSearch`() {
    val intent = Intent(CoreSearchWidget.TEXT_CLICKED)

    val result = parser.parse(intent)

    assertEquals(
      PendingIntentParser.ReaderIntentAction.OpenSearch(
        "",
        isVoice = false,
        false
      ),
      result
    )
  }

  @Test
  fun `MIC_CLICKED returns voice OpenSearch`() {
    val intent = Intent(CoreSearchWidget.MIC_CLICKED)

    val result = parser.parse(intent)

    assertEquals(
      PendingIntentParser.ReaderIntentAction.OpenSearch(
        "",
        isVoice = true,
        false
      ),
      result
    )
  }

  @Test
  fun `STAR_CLICKED returns OpenBookmarks`() {
    val intent = Intent(CoreSearchWidget.STAR_CLICKED)

    val result = parser.parse(intent)

    assertEquals(
      PendingIntentParser.ReaderIntentAction.OpenBookmarks,
      result
    )
  }

  @Test
  fun `unknown action returns None`() {
    val intent = Intent("unknown")

    val result = parser.parse(intent)

    assertEquals(
      PendingIntentParser.ReaderIntentAction.None,
      result
    )
  }

  @Test
  fun `ACTION_VIEW with zim extra returns None`() {
    val intent = Intent(Intent.ACTION_VIEW).apply {
      putExtra(ZIM_FILE_URI_KEY, "abc")
    }

    assertEquals(
      PendingIntentParser.ReaderIntentAction.None,
      parser.parse(intent)
    )
  }

  @Test
  fun `ACTION_VIEW with file scheme returns None`() {
    val intent = mockk<Intent>()
    every { intent.action } returns Intent.ACTION_VIEW
    every { intent.hasExtra(ZIM_FILE_URI_KEY) } returns false
    every { intent.data } returns null
    every { intent.scheme } returns "file"
    every { intent.type } returns "text/plain"

    assertEquals(
      PendingIntentParser.ReaderIntentAction.None,
      parser.parse(intent)
    )
  }

  @Test
  fun `ACTION_VIEW with octet stream returns None`() {
    val intent = mockk<Intent>()
    every { intent.action } returns Intent.ACTION_VIEW
    every { intent.hasExtra(ZIM_FILE_URI_KEY) } returns false
    every { intent.data } returns null
    every { intent.scheme } returns "https"
    every { intent.type } returns "application/octet-stream"

    assertEquals(
      PendingIntentParser.ReaderIntentAction.None,
      parser.parse(intent)
    )
  }

  @Test
  fun `ACTION_VIEW with null type returns None`() {
    val intent = Intent(Intent.ACTION_VIEW)

    assertEquals(
      PendingIntentParser.ReaderIntentAction.None,
      parser.parse(intent)
    )
  }

  @Test
  fun `ACTION_VIEW with search uri returns OpenSearch`() {
    val uri = mockk<Uri>()
    every { uri.host } returns "example.com"
    every { uri.lastPathSegment } returns "Albert"
    val intent = mockk<Intent>()
    every { intent.action } returns Intent.ACTION_VIEW
    every { intent.hasExtra(ZIM_FILE_URI_KEY) } returns false
    every { intent.scheme } returns "https"
    every { intent.type } returns "text/html"
    every { intent.data } returns uri

    assertEquals(
      PendingIntentParser.ReaderIntentAction.OpenSearch(
        "Albert",
        isVoice = false,
        false
      ),
      parser.parse(intent)
    )
  }

  @Test
  fun `ACTION_WEB_SEARCH returns OpenSearch with the query extra`() {
    val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
      putExtra(SearchManager.QUERY, "hello world")
    }

    assertEquals(
      PendingIntentParser.ReaderIntentAction.OpenSearch(
        "hello world",
        isVoice = false,
        isOpenedFromTabView = false
      ),
      parser.parse(intent)
    )
  }

  @Test
  fun `ACTION_WEB_SEARCH with no query extra returns empty OpenSearch`() {
    val intent = Intent(Intent.ACTION_WEB_SEARCH)

    assertEquals(
      PendingIntentParser.ReaderIntentAction.OpenSearch(
        "",
        isVoice = false,
        isOpenedFromTabView = false
      ),
      parser.parse(intent)
    )
  }

  @Test
  fun `wikipedia org article link with wiki path returns OpenSearch with decoded title`() {
    val uri = mockk<Uri>()
    every { uri.host } returns "en.wikipedia.org"
    every { uri.path } returns "/wiki/Albert_Einstein"
    every { uri.getQueryParameter("search") } returns null
    every { uri.getQueryParameter("q") } returns null
    val intent = mockk<Intent>()
    every { intent.action } returns Intent.ACTION_VIEW
    every { intent.hasExtra(ZIM_FILE_URI_KEY) } returns false
    every { intent.data } returns uri

    assertEquals(
      PendingIntentParser.ReaderIntentAction.OpenSearch(
        "Albert Einstein",
        isVoice = false,
        isOpenedFromTabView = false
      ),
      parser.parse(intent)
    )
  }

  @Test
  fun `wikipedia com search link with search query param returns OpenSearch with query`() {
    val uri = mockk<Uri>()
    every { uri.host } returns "www.wikipedia.com"
    every { uri.path } returns "/wiki/Special:Search"
    every { uri.getQueryParameter("search") } returns "gravity"
    every { uri.getQueryParameter("q") } returns null
    val intent = mockk<Intent>()
    every { intent.action } returns Intent.ACTION_VIEW
    every { intent.hasExtra(ZIM_FILE_URI_KEY) } returns false
    every { intent.data } returns uri

    assertEquals(
      PendingIntentParser.ReaderIntentAction.OpenSearch(
        "gravity",
        isVoice = false,
        isOpenedFromTabView = false
      ),
      parser.parse(intent)
    )
  }

  @Test
  fun `wikipedia fr subdomain link with q query param returns OpenSearch with query`() {
    val uri = mockk<Uri>()
    every { uri.host } returns "fr.wikipedia.org"
    every { uri.path } returns "/w/index.php"
    every { uri.getQueryParameter("search") } returns null
    every { uri.getQueryParameter("q") } returns "chat"
    val intent = mockk<Intent>()
    every { intent.action } returns Intent.ACTION_VIEW
    every { intent.hasExtra(ZIM_FILE_URI_KEY) } returns false
    every { intent.data } returns uri

    assertEquals(
      PendingIntentParser.ReaderIntentAction.OpenSearch(
        "chat",
        isVoice = false,
        isOpenedFromTabView = false
      ),
      parser.parse(intent)
    )
  }

  @Test
  fun `wikipedia de subdomain article link returns OpenSearch with decoded title`() {
    val uri = mockk<Uri>()
    every { uri.host } returns "de.wikipedia.org"
    every { uri.path } returns "/wiki/Berlin"
    every { uri.getQueryParameter("search") } returns null
    every { uri.getQueryParameter("q") } returns null
    val intent = mockk<Intent>()
    every { intent.action } returns Intent.ACTION_VIEW
    every { intent.hasExtra(ZIM_FILE_URI_KEY) } returns false
    every { intent.data } returns uri

    assertEquals(
      PendingIntentParser.ReaderIntentAction.OpenSearch(
        "Berlin",
        isVoice = false,
        isOpenedFromTabView = false
      ),
      parser.parse(intent)
    )
  }

  @Test
  fun `wikipedia link is recognised even without a MIME type`() {
    // Regression guard: links delivered from another app/browser typically carry
    // no MIME type, which the generic ACTION_VIEW fallback treats as "unknown".
    val uri = mockk<Uri>()
    every { uri.host } returns "en.wikipedia.org"
    every { uri.path } returns "/wiki/Kotlin_(programming_language)"
    every { uri.getQueryParameter("search") } returns null
    every { uri.getQueryParameter("q") } returns null
    val intent = mockk<Intent>()
    every { intent.action } returns Intent.ACTION_VIEW
    every { intent.hasExtra(ZIM_FILE_URI_KEY) } returns false
    every { intent.type } returns null
    every { intent.scheme } returns "https"
    every { intent.data } returns uri

    assertEquals(
      PendingIntentParser.ReaderIntentAction.OpenSearch(
        "Kotlin (programming language)",
        isVoice = false,
        isOpenedFromTabView = false
      ),
      parser.parse(intent)
    )
  }
}
