/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.kiwix.kiwixmobile.utils.StyleUtils;

import java.util.HashMap;

import static org.kiwix.kiwixmobile.utils.Constants.CONTACT_EMAIL_ADDRESS;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_EXTERNAL_LINK;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_BOTTOM_TOOLBAR;

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

  @Override
  public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
    callback.webViewFailedLoading(failingUrl);
  }

  @Override
  public void onPageFinished(WebView view, String url) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(view.getContext());

    if ((url.equals("content://" + BuildConfig.APPLICATION_ID + ".zim.base/null")) && !BuildConfig.IS_CUSTOM_APP) {
      callback.showHelpPage();
      return;
    }
    if (!url.equals("file:///android_asset/help.html")) {
      view.removeView(help);
    } else if (!BuildConfig.IS_CUSTOM_APP) {
      if (view.findViewById(R.id.get_content_card) == null) {
        LayoutInflater inflater = LayoutInflater.from(view.getContext());

        help = (LinearLayout) inflater.inflate(R.layout.help, null);

        CardView cardView=help.findViewById(R.id.feedback_card);
        if (settings.getBoolean(PREF_BOTTOM_TOOLBAR, false)) {
          LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
          params.setMargins(0,0,0,dpToPx(56));
          cardView.setLayoutParams(params);
        }else{
          LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
          params.setMargins(0,0,0,0);
          cardView.setLayoutParams(params);
        }
        help.findViewById(R.id.get_content_card)
            .setOnClickListener(card -> {
                  help.findViewById(R.id.get_content_card).setEnabled(false);
                  callback.manageZimFiles(1);
            }
            );
        view.addView(help);

        ImageView welcome_image = help.findViewById(R.id.welcome_image);
        if (KiwixMobileActivity.nightMode) {
          welcome_image.setImageResource(R.drawable.kiwix_welcome_night);
        } else {
          welcome_image.setImageResource(R.drawable.kiwix_welcome);
        }
        TextView contact = help.findViewById(R.id.welcome21);
        contact.setText(StyleUtils.highlightUrl(contact.getText().toString(), CONTACT_EMAIL_ADDRESS));
        contact.setOnClickListener(v -> callback.sendContactEmail());
      }
    }
    callback.webViewUrlFinishedLoading();
  }
  public static int dpToPx(int dp)
  {
    return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
  }
}
