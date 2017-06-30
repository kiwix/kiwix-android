package org.kiwix.kiwixmobile;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.kiwix.kiwixmobile.utils.StyleUtils;

import java.util.HashMap;

import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.builders.Actions;
import com.google.firebase.appindexing.builders.Indexables;

public class KiwixWebViewClient extends WebViewClient {

  private static final HashMap<String, String> DOCUMENT_TYPES = new HashMap<String, String>() {{
    put("epub", "application/epub+zip");
    put("pdf", "application/pdf");
  }};
  private LinearLayout help;
  private WebViewCallback callback;

  public KiwixWebViewClient(WebViewCallback callback) {
    this.callback = callback;
  }

  @Override
  public boolean shouldOverrideUrlLoading(WebView view, String url) {
    callback.webViewUrlLoading();

    if (url.startsWith(ZimContentProvider.CONTENT_URI.toString())) {
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
    } else if (url.startsWith("file://")) {
      // To handle help page (loaded from resources)
      return true;
    } else if (url.startsWith("javascript:")) {
      // Allow javascript for HTML functions and code execution (EX: night mode)
      return true;
    } else if (url.startsWith(ZimContentProvider.UI_URI.toString())) {
      Log.e("KiwixWevViewClient", "UI Url " + url + " not supported.");
      return true;
    }

    // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    callback.openExternalUrl(intent);
    return true;
  }

  @Override
  public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
    callback.webViewFailedLoading(failingUrl);
  }

  @Override
  public void onPageFinished(WebView view, String url) {
      {
          String title = view.getTitle();
          // TODO: Convert to deep linked URL.
          String deepLink = url;
          Log.i("KIWIXTEST", "url: " + deepLink + "  title: " + title);

          FirebaseAppIndex.getInstance().update(Indexables.newSimple(title, deepLink))
                  .addOnCompleteListener(task -> Log.i("KIWIXTEST", "update: " + deepLink + " " + task.isSuccessful()));
          FirebaseUserActions.getInstance().start(
                  new Action.Builder(Action.Builder.VIEW_ACTION)
                          .setObject(title, deepLink)
                          .setMetadata(new Action.Metadata.Builder().setUpload(false))
                          .build()
          ).addOnCompleteListener(task -> Log.i("KIWIXTEST", "start: " + deepLink + " " + task.isSuccessful()));
      }

    if ((url.equals("content://" + BuildConfig.APPLICATION_ID + ".zim.base/null")) && !BuildConfig.IS_CUSTOM_APP) {
      callback.showHelpPage();
      return;
    }
    if (!url.equals("file:///android_res/raw/help.html")) {
      view.removeView(help);
    } else if (!BuildConfig.IS_CUSTOM_APP) {
      if (view.findViewById(R.id.get_content_card) == null) {
        LayoutInflater inflater = LayoutInflater.from(view.getContext());
        help = (LinearLayout) inflater.inflate(R.layout.help, null);
        help.findViewById(R.id.get_content_card)
            .setOnClickListener(card -> callback.manageZimFiles(1));
        view.addView(help);
        TextView contact = (TextView) help.findViewById(R.id.welcome21);
        contact.setText(StyleUtils.highlightUrl(contact.getText().toString(),
            KiwixMobileActivity.contactEmailAddress));
        contact.setOnClickListener(v -> {
          callback.sendContactEmail();
        });
      }
    }
    callback.webViewUrlFinishedLoading();
  }
}
