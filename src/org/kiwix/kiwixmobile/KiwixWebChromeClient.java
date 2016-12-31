package org.kiwix.kiwixmobile;

import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

class KiwixWebChromeClient extends WebChromeClient {

  private KiwixMobileActivity kiwixMobileActivity;

  public KiwixWebChromeClient(KiwixMobileActivity kiwixMobileActivity) {
    this.kiwixMobileActivity = kiwixMobileActivity;
  }

  @Override
  public void onProgressChanged(WebView view, int progress) {
    kiwixMobileActivity.progressBar.setProgress(progress);

    if (progress == 100) {
      Log.d(KiwixMobileActivity.TAG_KIWIX, "Loading article finished.");
      if (kiwixMobileActivity.requestClearHistoryAfterLoad) {
        Log.d(KiwixMobileActivity.TAG_KIWIX,
            "Loading article finished and requestClearHistoryAfterLoad -> clearHistory");
        kiwixMobileActivity.getCurrentWebView().clearHistory();
        kiwixMobileActivity.requestClearHistoryAfterLoad = false;
      }

      Log.d(KiwixMobileActivity.TAG_KIWIX, "Loaded URL: " + kiwixMobileActivity.getCurrentWebView().getUrl());
    }
  }
}
