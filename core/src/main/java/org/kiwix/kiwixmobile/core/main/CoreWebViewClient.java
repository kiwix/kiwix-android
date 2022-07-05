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

package org.kiwix.kiwixmobile.core.main;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.HashMap;
import org.kiwix.kiwixmobile.core.CoreApp;
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer;
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil;
import org.kiwix.kiwixmobile.core.utils.files.FileUtils;

import static org.kiwix.kiwixmobile.core.reader.ZimFileReader.CONTENT_PREFIX;
import static org.kiwix.kiwixmobile.core.reader.ZimFileReader.UI_URI;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.TAG_KIWIX;

public class CoreWebViewClient extends WebViewClient {
  private static final HashMap<String, String> DOCUMENT_TYPES = new HashMap<String, String>() {{
    put("epub", "application/epub+zip");
    put("pdf", "application/pdf");
  }};
  protected final WebViewCallback callback;
  protected final ZimReaderContainer zimReaderContainer;
  private final SharedPreferenceUtil sharedPreferenceUtil;
  private static String[] LEGACY_CONTENT_PREFIXES = new String[] {
    "zim://content/",
    Uri.parse("content://" + CoreApp.getInstance().getPackageName() + ".zim.base/").toString()
  };
  private String urlWithAnchor;

  public CoreWebViewClient(
    WebViewCallback callback,
    ZimReaderContainer zimReaderContainer,
    SharedPreferenceUtil sharedPreferenceUtil) {
    this.callback = callback;
    this.zimReaderContainer = zimReaderContainer;
    this.sharedPreferenceUtil = sharedPreferenceUtil;
  }

  @Override
  public boolean shouldOverrideUrlLoading(WebView view, String url) {
    callback.webViewUrlLoading();
    url = convertLegacyUrl(url);
    urlWithAnchor = url.contains("#") ? url : null;
    if (zimReaderContainer.isRedirect(url)) {
      if (handleEpubAndPdf(url)) {
        return true;
      }
      view.loadUrl(zimReaderContainer.getRedirect(url));
      return true;
    }
    if (url.startsWith(CONTENT_PREFIX)) {
      return handleEpubAndPdf(url);
    }
    if (url.startsWith("javascript:")) {
      // Allow javascript for HTML functions and code execution (EX: night mode)
      return true;
    }
    if (url.startsWith(UI_URI.toString())) {
      Log.e("KiwixWebViewClient", "UI Url " + url + " not supported.");
      //TODO: Document this code - what's a UI_URL?
      return true;
    }

    // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    callback.openExternalUrl(intent);
    return true;
  }

  private String convertLegacyUrl(String url) {
    for (String legacyContentPrefix : LEGACY_CONTENT_PREFIXES) {
      if (url.startsWith(legacyContentPrefix)) {
        return url.replace(legacyContentPrefix, CONTENT_PREFIX);
      }
    }
    return url;
  }

  private boolean handleEpubAndPdf(String url) {
    String extension = MimeTypeMap.getFileExtensionFromUrl(url);
    if (DOCUMENT_TYPES.containsKey(extension)) {
      File savedFile =
        FileUtils.downloadFileFromUrl(url, null, zimReaderContainer, sharedPreferenceUtil);
      if (savedFile != null && savedFile.exists()) {
        Context context = CoreApp.getInstance();
        Uri uri = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
          ? FileProvider.getUriForFile(
          context,
          context.getPackageName() + ".fileprovider", savedFile)
          : Uri.fromFile(savedFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, DOCUMENT_TYPES.get(extension));
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        callback.openExternalUrl(intent);
      }
      return true;
    }
    return false;
  }

  @Override
  public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
    callback.webViewFailedLoading(failingUrl);
  }

  @Override
  public void onPageFinished(WebView view, String url) {
    boolean invalidUrl = url.equals(CONTENT_PREFIX + "null");

    Log.d(TAG_KIWIX, "invalidUrl = " + invalidUrl);

    if (invalidUrl) {
      return;
    }

    jumpToAnchor(view, url);
    callback.webViewUrlFinishedLoading();
  }

  /*
   * If 2 urls are the same aside from the `#` component then calling load
   * does not trigger our loading code and the webview will go to the anchor
   * */
  private void jumpToAnchor(WebView view, String loadedUrl) {
    if (urlWithAnchor != null && urlWithAnchor.startsWith(loadedUrl)) {
      view.loadUrl(urlWithAnchor);
      urlWithAnchor = null;
    }
  }

  @Nullable
  @Override
  public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
    String url = convertLegacyUrl(request.getUrl().toString());
    if (url.startsWith(CONTENT_PREFIX)) {
      return zimReaderContainer.load(url, request.getRequestHeaders());
    } else {
      return super.shouldInterceptRequest(view, request);
    }
  }
}
