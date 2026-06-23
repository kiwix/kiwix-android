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

import android.content.Intent
import org.kiwix.kiwixmobile.core.main.CoreSearchWidget
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.main.ZIM_HOST_DEEP_LINK_SCHEME
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser.ReaderIntentAction.None
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser.ReaderIntentAction.OpenBookmarks
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser.ReaderIntentAction.OpenSearch
import javax.inject.Inject

class PendingIntentParser @Inject constructor() {
  sealed interface ReaderIntentAction {
    data class OpenSearch(
      val query: String,
      val isVoice: Boolean,
      val isOpenedFromTabView: Boolean
    ) : ReaderIntentAction

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

      Intent.ACTION_VIEW -> parseActionViewIntent(intent)

      else -> None
    }
  }

  private fun parseActionViewIntent(intent: Intent): ReaderIntentAction {
    if (intent.hasExtra(ZIM_FILE_URI_KEY)) return None
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
}
