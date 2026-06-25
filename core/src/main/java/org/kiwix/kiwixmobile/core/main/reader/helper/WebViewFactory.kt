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

package org.kiwix.kiwixmobile.core.main.reader.helper

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.CoreWebViewClient
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.WebViewCallback
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.StyleUtils.getAttributes
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import javax.inject.Inject

class WebViewFactory @Inject constructor(
  private val context: Context,
  private val zimReaderContainer: ZimReaderContainer,
  private val kiwixDataStore: KiwixDataStore
) {
  /**
   * Initializes a new instance of `KiwixWebView`.
   *
   * @param callback A callback that attached to webView and provides the webView callbacks.
   * @param videoView A frameLayout, in which videos will play.
   * @return The initialized `KiwixWebView` instance.
   */
  fun create(callback: WebViewCallback, videoView: FrameLayout): KiwixWebView {
    val attrs = context.getAttributes(R.xml.webview)
    return createWebView(attrs, callback, videoView)
  }

  private fun createWebView(
    attrs: AttributeSet,
    callback: WebViewCallback,
    videoView: FrameLayout
  ): KiwixWebView {
    return KiwixWebView(
      context,
      callback,
      attrs,
      videoView,
      CoreWebViewClient(callback, zimReaderContainer),
      kiwixDataStore
    )
  }
}
