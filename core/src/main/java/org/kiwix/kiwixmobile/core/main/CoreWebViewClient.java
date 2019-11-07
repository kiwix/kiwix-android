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

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import java.util.HashMap;
import org.kiwix.kiwixmobile.core.CoreApp;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil;
import org.kiwix.kiwixmobile.core.reader.ZimFileReader;
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer;

import static org.kiwix.kiwixmobile.core.utils.Constants.EXTRA_EXTERNAL_LINK;
import static org.kiwix.kiwixmobile.core.utils.Constants.TAG_KIWIX;

public abstract class CoreWebViewClient extends WebViewClient {
  private static final HashMap<String, String> DOCUMENT_TYPES = new HashMap<String, String>() {{
    put("epub", "application/epub+zip");
    put("pdf", "application/pdf");
  }};
  protected final WebViewCallback callback;
  protected final ZimReaderContainer zimReaderContainer;
  private final SharedPreferenceUtil sharedPreferenceUtil =
    new SharedPreferenceUtil(CoreApp.getInstance());
  private View home;

  public CoreWebViewClient(
    WebViewCallback callback, ZimReaderContainer zimReaderContainer) {
    this.callback = callback;
    this.zimReaderContainer = zimReaderContainer;
  }

  @Override
  public boolean shouldOverrideUrlLoading(WebView view, String url) {
    callback.webViewUrlLoading();

    if (zimReaderContainer.isRedirect(url)) {
      if (handleEpubAndPdf(url)) {
        return true;
      }
      view.loadUrl(zimReaderContainer.getRedirect(url));
      return true;
    }
    if (url.startsWith(ZimFileReader.CONTENT_URI.toString())) {
      return handleEpubAndPdf(url);
    }
    if (url.startsWith("file://")) {
      // To handle home page (loaded from resources)
      return true;
    }
    if (url.startsWith("javascript:")) {
      // Allow javascript for HTML functions and code execution (EX: night mode)
      return true;
    }
    if (url.startsWith(ZimFileReader.UI_URI.toString())) {
      Log.e("KiwixWebViewClient", "UI Url " + url + " not supported.");
      //TODO: Document this code - what's a UI_URL?
      return true;
    }

    // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    intent.putExtra(EXTRA_EXTERNAL_LINK, true);
    callback.openExternalUrl(intent);
    return true;
  }

  private boolean handleEpubAndPdf(String url) {
    String extension = MimeTypeMap.getFileExtensionFromUrl(url);
    if (DOCUMENT_TYPES.containsKey(extension)) {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      Uri uri = Uri.parse(url);
      intent.setDataAndType(uri, DOCUMENT_TYPES.get(extension));
      intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
      callback.openExternalUrl(intent);
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
    boolean invalidUrl =
      url.equals("content://" + CoreApp.getInstance().getPackageName() + ".zim.base/null");
    Log.d(TAG_KIWIX, "invalidUrl = " + invalidUrl);

    if (invalidUrl) {
      onInvalidUrl(view);
      return;
    }

    if (!url.equals("file:///android_asset/home.html")) {
      view.removeView(home);
    } else{
      onUrlEqualToHome(view);
    }
    callback.webViewUrlFinishedLoading();
  }

  protected abstract void onUrlEqualToHome(WebView view);

  protected abstract void onInvalidUrl(WebView view);

  protected void inflateHomeView(WebView view) {
    LayoutInflater inflater = LayoutInflater.from(view.getContext());
    home = inflater.inflate(R.layout.content_main, view, false);
    callback.setHomePage(home);
    if (sharedPreferenceUtil.nightMode()) {
      ImageView cardImage = home.findViewById(R.id.content_main_card_image);
      cardImage.setImageResource(R.drawable.ic_home_kiwix_banner_night);
    }
    view.removeAllViews();
    view.addView(home);
  }
}
