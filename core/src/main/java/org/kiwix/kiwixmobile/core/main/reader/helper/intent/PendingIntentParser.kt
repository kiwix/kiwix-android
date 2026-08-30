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
import org.kiwix.kiwixmobile.core.main.CoreSearchWidget
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.main.ZIM_HOST_DEEP_LINK_SCHEME
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser.ReaderIntentAction.None
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser.ReaderIntentAction.OpenBookmarks
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser.ReaderIntentAction.OpenSearch
import java.net.URLDecoder
import javax.inject.Inject

class PendingIntentParser @Inject constructor() {
  sealed interface ReaderIntentAction {
    data class OpenSearch(
      val query: String,
      val isVoice: Boolean,
      val isOpenedFromTabView: Boolean
    ) : ReaderIntentAction

    data class OpenZim(val zimFileUri: String, val pageUrl: String) : ReaderIntentAction
    data object OpenBookmarks : ReaderIntentAction
    data object None : ReaderIntentAction
  }

  fun parse(intent: Intent): ReaderIntentAction {
    return when (intent.action) {
      Intent.ACTION_PROCESS_TEXT ->
        OpenSearch(
          query = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT).orEmpty(),
          isVoice = false,
          isOpenedFromTabView = false
        )

      CoreSearchWidget.TEXT_CLICKED -> OpenSearch("", isVoice = false, isOpenedFromTabView = false)

      CoreSearchWidget.MIC_CLICKED -> OpenSearch("", true, isOpenedFromTabView = false)

      CoreSearchWidget.STAR_CLICKED -> OpenBookmarks

      // Android routes a system-wide "web search" request here (e.g. from the
      // assistant or another app asking to search the web). We can't search the
      // web offline, so we redirect the query into our own, currently-open-book
      // search instead of doing nothing with it.
      Intent.ACTION_WEB_SEARCH ->
        OpenSearch(
          query = intent.getStringExtra(SearchManager.QUERY).orEmpty(),
          isVoice = false,
          isOpenedFromTabView = false
        )

      Intent.ACTION_VIEW -> parseActionViewIntent(intent)

      else -> None
    }
  }

  @Suppress("ReturnCount")
  private fun parseActionViewIntent(intent: Intent): ReaderIntentAction {
    if (intent.hasExtra(ZIM_FILE_URI_KEY)) return None

    // Wikipedia links are recognised ahead of the generic scheme/type checks below,
    // because the OS typically delivers them with no MIME type at all (which the
    // generic checks below treat as "not a link we understand").
    intent.data?.takeIf { isWikipediaHost(it.host) }?.let { return parseWikipediaUri(it) }

    val hasValidScheme =
      intent.scheme in listOf("file", "content", "zim", ZIM_HOST_DEEP_LINK_SCHEME)
    // Added condition to handle ZIM files. When opening from storage, the intent may
    // return null for the type, triggering the search unintentionally. This condition
    // prevents such occurrences.
    val isOctetStream = intent.type == null || intent.type == "application/octet-stream"

    if (isOctetStream || hasValidScheme) return None

    val searchString = if (intent.data == null) "" else intent.data?.lastPathSegment
    return OpenSearch(searchString.orEmpty(), false, isOpenedFromTabView = false)
  }

  private fun isWikipediaHost(host: String?): Boolean {
    if (host == null) return false
    val lowerHost = host.lowercase()
    return WIKIPEDIA_DOMAINS.any { domain -> lowerHost == domain || lowerHost.endsWith(".$domain") }
  }

  /**
   * Converts a recognised Wikipedia URL into a search query against the currently
   * open book, matching the (only) working scope confirmed for this app today --
   * see kiwix-android#731. We deliberately do not attempt to resolve the link to an
   * exact page in a specific, possibly-not-open ZIM: there is no known-good way to
   * pick which locally available ZIM contains a given Wikipedia article, and
   * treating this as a plain search re-uses the reader's existing, working
   * `OpenSearch` path instead.
   */
  private fun parseWikipediaUri(uri: Uri): ReaderIntentAction {
    val query = uri.getQueryParameter("search") ?: uri.getQueryParameter("q")
    val searchQuery = query ?: extractArticleTitleFromPath(uri.path)
    return OpenSearch(searchQuery.orEmpty(), isVoice = false, isOpenedFromTabView = false)
  }

  private fun extractArticleTitleFromPath(path: String?): String? {
    val marker = "/wiki/"
    val markerIndex = path?.indexOf(marker) ?: -1
    val rawTitle = if (markerIndex == -1) null else path?.substring(markerIndex + marker.length)
    return rawTitle?.takeIf { it.isNotBlank() }
      ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
      ?.replace('_', ' ')
  }

  companion object {
    // wikipedia.fr / wikipedia.de are Wikimedia chapter (fundraising/organizational)
    // sites, not Wikipedia itself -- all language editions of Wikipedia are
    // subdomains of wikipedia.org (e.g. de.wikipedia.org, fr.wikipedia.org),
    // already covered by the ".wikipedia.org" suffix match below.
    private val WIKIPEDIA_DOMAINS = listOf("wikipedia.org", "wikipedia.com")
  }
}
