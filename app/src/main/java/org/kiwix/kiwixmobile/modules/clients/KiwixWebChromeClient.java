package org.kiwix.kiwixmobile.modules.clients;

import android.webkit.WebChromeClient;
import android.webkit.WebView;

import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.common.views.web.WebViewCallback;

public class KiwixWebChromeClient extends WebChromeClient {

  private WebViewCallback callback;

  public KiwixWebChromeClient(WebViewCallback callback) {
    this.callback = callback;
  }

  @Override
  public void onProgressChanged(WebView view, int progress) {
    callback.webViewProgressChanged(progress);
    ((KiwixMobileActivity) view.getContext()).supportInvalidateOptionsMenu();
  }

  @Override public void onReceivedTitle(WebView view, String title) {
    super.onReceivedTitle(view, title);
    callback.webViewTitleUpdated(title);
  }
}
