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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import org.kiwix.kiwixmobile.core.CoreApp.Companion.instance
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.downloadFileFromUrl

open class CoreWebViewClient(
  protected val callback: WebViewCallback,
  protected val zimReaderContainer: ZimReaderContainer,
  private val sharedPreferenceUtil: SharedPreferenceUtil
) : WebViewClient() {
  private var urlWithAnchor: String? = null

  @Suppress("ReturnCount")
  override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
    var url = request.url.toString()
    callback.webViewUrlLoading()
    url = convertLegacyUrl(url)
    urlWithAnchor = if (url.contains("#")) url else null
    if (zimReaderContainer.isRedirect(url)) {
      if (handleEpubAndPdf(url)) {
        return true
      }
      view.loadUrl(zimReaderContainer.getRedirect(url))
      return true
    }
    if (url.startsWith(ZimFileReader.CONTENT_PREFIX)) {
      return handleEpubAndPdf(url)
    }
    if (url.startsWith("javascript:")) {
      // Allow javascript for HTML functions and code execution (EX: night mode)
      return true
    }
    if (url.startsWith(ZimFileReader.UI_URI.toString())) {
      Log.e("KiwixWebViewClient", "UI Url $url not supported.")
      // Document this code - what's a UI_URL?
      return true
    }

    // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    callback.openExternalUrl(intent)
    return true
  }

  private fun convertLegacyUrl(url: String): String {
    return LEGACY_CONTENT_PREFIXES
      .firstOrNull(url::startsWith)
      ?.let { url.replace(it, ZimFileReader.CONTENT_PREFIX) }
      ?: url
  }

  @Suppress("NestedBlockDepth")
  private fun handleEpubAndPdf(url: String): Boolean {
    val extension = MimeTypeMap.getFileExtensionFromUrl(url)
    if (DOCUMENT_TYPES.containsKey(extension)) {
      downloadFileFromUrl(
        url,
        null,
        zimReaderContainer,
        sharedPreferenceUtil
      )?.let {
        if (it.exists()) {
          val context: Context = instance
          val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider", it
          )
          val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, DOCUMENT_TYPES[extension])
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          callback.openExternalUrl(intent)
        }
      }
      return true
    }
    return false
  }

  override fun onReceivedError(
    view: WebView?,
    request: WebResourceRequest?,
    error: WebResourceError?
  ) {
    callback.webViewFailedLoading(request?.url.toString())
  }

  override fun onPageFinished(view: WebView, url: String) {
    val invalidUrl = url == ZimFileReader.CONTENT_PREFIX + "null"
    Log.d(TAG_KIWIX, "invalidUrl = $invalidUrl")
    if (invalidUrl) {
      return
    }
    jumpToAnchor(view, url)
    callback.webViewUrlFinishedLoading()
  }

  /*
   * If 2 urls are the same aside from the `#` component then calling load
   * does not trigger our loading code and the webview will go to the anchor
   * */
  private fun jumpToAnchor(view: WebView, loadedUrl: String) {
    urlWithAnchor?.let {
      if (it.startsWith(loadedUrl)) {
        view.loadUrl(it)
        urlWithAnchor = null
      }
    }
  }

  override fun shouldInterceptRequest(
    view: WebView,
    request: WebResourceRequest
  ): WebResourceResponse? {
    val url = convertLegacyUrl(request.url.toString())
    return if (url.startsWith(ZimFileReader.CONTENT_PREFIX)) {
      zimReaderContainer.load(url, request.requestHeaders)
    } else {
      super.shouldInterceptRequest(view, request)
    }
  }

  companion object {
    private val DOCUMENT_TYPES: HashMap<String?, String?> = object : HashMap<String?, String?>() {
      init {
        put("epub", "application/epub+zip")
        put("pdf", "application/pdf")
      }
    }
    private val LEGACY_CONTENT_PREFIXES = arrayOf(
      "zim://content/",
      Uri.parse("content://" + instance.packageName + ".zim.base/").toString()
    )
  }
}
