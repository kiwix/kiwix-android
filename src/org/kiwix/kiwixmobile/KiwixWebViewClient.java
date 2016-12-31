package org.kiwix.kiwixmobile;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.util.HashMap;
import org.kiwix.kiwixmobile.settings.Constants;

class KiwixWebViewClient extends WebViewClient {

  private KiwixMobileActivity kiwixMobileActivity;
  private HashMap<String, String> documentTypes = new HashMap<String, String>() {{
    put("epub", "application/epub+zip");
    put("pdf", "application/pdf");
  }};

  private LinearLayout help;

  private TabDrawerAdapter mAdapter;

  public KiwixWebViewClient(KiwixMobileActivity kiwixMobileActivity, TabDrawerAdapter adapter) {
    this.kiwixMobileActivity = kiwixMobileActivity;
    mAdapter = adapter;
  }

  @Override
  public boolean shouldOverrideUrlLoading(WebView view, String url) {
    if (kiwixMobileActivity.isFirstRun) {
      kiwixMobileActivity.contentsDrawerHint();
      SharedPreferences.Editor editor = kiwixMobileActivity.settings.edit();
      editor.putBoolean("isFirstRun", false); // It is no longer the first run
      kiwixMobileActivity.isFirstRun = false;
      editor.apply();
    }

    if (url.startsWith(ZimContentProvider.CONTENT_URI.toString())) {

      String extension = MimeTypeMap.getFileExtensionFromUrl(url);
      if (documentTypes.containsKey(extension)) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(url);
        intent.setDataAndType(uri, documentTypes.get(extension));
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        try {
          kiwixMobileActivity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
          Toast.makeText(kiwixMobileActivity,
              kiwixMobileActivity.getString(R.string.no_reader_application_installed), Toast.LENGTH_LONG).show();
        }

        return true;
      }

      return false;
    } else if (url.startsWith("file://")) {
      // To handle help page (loaded from resources)
      return true;
    } else if (url.startsWith("javascript:")) {
      // Allow javascript for HTML functions and code execution (EX: night mode)
      return true;
    } else if (url.startsWith(ZimContentProvider.UI_URI.toString())) {
      // To handle links which access user interface (i.p. used in help page)
      if (url.equals(ZimContentProvider.UI_URI.toString() + "selectzimfile")) {
        kiwixMobileActivity.manageZimFiles(1);
      } else {
        Log.e(KiwixMobileActivity.TAG_KIWIX, "UI Url " + url + " not supported.");
      }
      return true;
    }

    // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

    kiwixMobileActivity.startActivity(intent);

    return true;
  }

  @Override
  public void onReceivedError(WebView view, int errorCode, String description,
      String failingUrl) {

    String errorString =
        String.format(kiwixMobileActivity.getResources().getString(R.string.error_articleurlnotfound), failingUrl);
    // TODO apparently screws up back/forward
    kiwixMobileActivity.getCurrentWebView().loadDataWithBaseURL("file://error",
        "<html><body>" + errorString + "</body></html>", "text/html", "utf-8", failingUrl);
    String title = kiwixMobileActivity.getResources().getString(R.string.app_name);
    kiwixMobileActivity.updateTitle(title);
  }

  @Override
  public void onPageFinished(WebView view, String url) {
    if ((url.equals("content://org.kiwix.zim.base/null")) && !Constants.IS_CUSTOM_APP) {
      kiwixMobileActivity.showWelcome();
      return;
    }
    if (!url.equals("file:///android_res/raw/welcome.html")) {
      view.removeView(help);
    } else if (!Constants.IS_CUSTOM_APP) {
      help = (LinearLayout) kiwixMobileActivity.getLayoutInflater().inflate(R.layout.help, null);
      help.findViewById(R.id.get_content_card).setOnClickListener(card -> kiwixMobileActivity.manageZimFiles(1));
      view.addView(help);
    }
    mAdapter.notifyDataSetChanged();
    kiwixMobileActivity.updateTableOfContents();
  }
}
