package org.kiwix.kiwixmobile;

import android.content.Intent;

public interface WebViewCallback {
  void webViewUrlLoading();

  void webViewUrlFinishedLoading();

  void webViewFailedLoading(String failingUrl);

  void showHelpPage();

  void sendContactEmail();

  void openExternalUrl(Intent intent);

  void manageZimFiles(int tab);

  void webViewProgressChanged(int progress);

  void webViewTitleUpdated(String title);

  void webViewPageChanged(int page, int maxPages);

  void webViewLongClick(String url);
}
