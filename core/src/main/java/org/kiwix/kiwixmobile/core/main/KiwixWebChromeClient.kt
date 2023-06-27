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

import android.view.ViewGroup
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import org.kiwix.videowebview.VideoEnabledWebChromeClient

class KiwixWebChromeClient(
  private val callback: WebViewCallback,
  nonVideoView: ViewGroup?,
  videoView: ViewGroup?,
  webView: KiwixWebView?
) : VideoEnabledWebChromeClient(nonVideoView, videoView, null, webView) {
  override fun onProgressChanged(view: WebView, progress: Int) {
    callback.webViewProgressChanged(progress)
    (view.context as AppCompatActivity).invalidateOptionsMenu()
  }

  override fun onReceivedTitle(view: WebView, title: String) {
    super.onReceivedTitle(view, title)
    callback.webViewTitleUpdated(title)
  }
}
