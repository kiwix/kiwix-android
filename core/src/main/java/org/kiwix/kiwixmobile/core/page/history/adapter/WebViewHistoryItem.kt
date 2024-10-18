/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.page.history.adapter

import org.kiwix.kiwixmobile.core.dao.entities.WebViewHistoryEntity

data class WebViewHistoryItem(
  val databaseId: Long = 0L,
  val zimId: String,
  val title: String,
  val pageUrl: String,
  val isForward: Boolean,
  val timeStamp: Long
) {
  constructor(
    zimId: String,
    title: String,
    pageUrl: String,
    isForward: Boolean,
    timeStamp: Long
  ) : this(
    0L,
    zimId,
    title,
    pageUrl,
    isForward,
    timeStamp
  )

  constructor(webViewHistoryEntity: WebViewHistoryEntity) : this(
    webViewHistoryEntity.id,
    webViewHistoryEntity.zimId,
    webViewHistoryEntity.title,
    webViewHistoryEntity.pageUrl,
    webViewHistoryEntity.isForward,
    webViewHistoryEntity.timeStamp
  )
}

interface WebViewHistoryCallback {
  fun onDataFetched(pageHistory: List<WebViewHistoryItem>)
  fun onError(error: Throwable)
}
