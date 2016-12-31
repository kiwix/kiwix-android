package org.kiwix.kiwixmobile;

import android.webkit.WebChromeClient;
import android.webkit.WebView;

class KiwixWebChromeClient extends WebChromeClient {

  private WebViewCallback callback;

  public KiwixWebChromeClient(WebViewCallback callback) {
    this.callback = callback;
  }

  @Override
  public void onProgressChanged(WebView view, int progress) {
    callback.webViewProgressChanged(progress);
  }
}
