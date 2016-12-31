package org.kiwix.kiwixmobile;

import android.content.Intent;

public interface WebViewCallback {
  void webViewUrlLoading();

  void webViewUrlFinishedLoading();

  void webViewFailedLoading(String failingUrl);

  void showWelcomePage();

  void openExternalUrl(Intent intent);

  void manageZimFiles(int tab);

  void webViewProgressChanged(int progress);
}
