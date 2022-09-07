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
package org.kiwix.kiwixmobile.core.main

import android.content.Intent

interface WebViewCallback {
  fun webViewUrlLoading()
  fun webViewUrlFinishedLoading()
  fun webViewFailedLoading(failingUrl: String)
  fun openExternalUrl(intent: Intent)
  fun webViewProgressChanged(progress: Int)
  fun webViewTitleUpdated(title: String)
  fun webViewPageChanged(page: Int, maxPages: Int)
  fun webViewLongClick(url: String)
  fun onFullscreenVideoToggled(isFullScreen: Boolean)
}
