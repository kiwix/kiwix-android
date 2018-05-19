/*
 * Copyright 2013
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.kiwix.kiwixmobile.common.views.web;

import android.content.Context;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.modules.clients.KiwixWebChromeClient;
import org.kiwix.kiwixmobile.modules.clients.KiwixWebViewClient;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.common.utils.LanguageUtils;
import org.kiwix.kiwixmobile.common.utils.SharedPreferenceUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;

public class KiwixWebView extends WebView {

  private static final String PREF_ZOOM = "pref_zoom_slider";

  private static final String PREF_ZOOM_ENABLED = "pref_zoom_enabled";

  private static final float[] NIGHT_MODE_COLORS = {
      -1.0f, 0, 0, 0, 255, // red
      0, -1.0f, 0, 0, 255, // green
      0, 0, -1.0f, 0, 255, // blue
      0, 0, 0, 1.0f, 0 // alpha
  };
  private WebViewCallback callback;
  @Inject SharedPreferenceUtil sharedPreferenceUtil;

  static class SaveHandler extends Handler {

    @Override
    public void handleMessage(Message msg) {
      String url = (String) msg.getData().get("url");
      String src = (String) msg.getData().get("src");

      if (url != null || src != null) {
        url = url == null ? src : url;
        url = url.substring(url.lastIndexOf('/') + 1, url.length());
        url = url.substring(url.indexOf("%3A") + 1, url.length());
        int dotIndex = url.lastIndexOf('.');

        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP
            && KiwixApplication.getInstance().getExternalMediaDirs().length > 0) {
          root = KiwixApplication.getInstance().getExternalMediaDirs()[0];
        }

        File storageDir = new File(root, url);
        String newUrl = url;
        for (int i = 2; storageDir.exists(); i++) {
          newUrl = url.substring(0, dotIndex) + "_" + i + url.substring(dotIndex, url.length());
          storageDir = new File(root, newUrl);
        }

        Uri source = Uri.parse(src);
        String toastText;

        try {
          InputStream input = KiwixApplication.getInstance().getContentResolver().openInputStream(source);
          OutputStream output = new FileOutputStream(storageDir);

          byte[] buffer = new byte[1024];
          int len;
          while ((len = input.read(buffer)) > 0) {
            output.write(buffer, 0, len);
          }
          input.close();
          output.close();

          String imageSaved = KiwixApplication.getInstance().getString(R.string.save_media_saved);
          toastText = String.format(imageSaved, newUrl);
        } catch (IOException e) {
          Log.w("kiwix", "Couldn't save image", e);
          toastText = KiwixApplication.getInstance().getString(R.string.save_media_error);
        }

        Toast.makeText(KiwixApplication.getInstance(), toastText, Toast.LENGTH_LONG).show();
      }
    }
  }

  public KiwixWebView(Context context, WebViewCallback callback, AttributeSet attrs) {
    super(context, attrs);
    this.callback = callback;
    KiwixApplication.getApplicationComponent().inject(this);
    // Set the user agent to the current locale so it can be read with navigator.userAgent
    getSettings().setUserAgentString(LanguageUtils.getCurrentLocale(context).toString());
    setWebViewClient(new KiwixWebViewClient(callback));
    setWebChromeClient(new KiwixWebChromeClient(callback));
    getSettings().setDomStorageEnabled(true);
  }

  public void loadPrefs() {
    disableZoomControls();

    boolean zoomEnabled = sharedPreferenceUtil.getPrefZoomEnabled();

    if (zoomEnabled) {
      int zoomScale = (int) sharedPreferenceUtil.getPrefZoom();
      setInitialScale(zoomScale);
    } else {
      setInitialScale(0);
    }
  }

  public void deactivateNightMode() {
    setLayerType(View.LAYER_TYPE_NONE, null);
  }

  public void toggleNightMode() {
    Paint paint = new Paint();
    ColorMatrixColorFilter filterInvert = new ColorMatrixColorFilter(NIGHT_MODE_COLORS);
    paint.setColorFilter(filterInvert);
    setLayerType(View.LAYER_TYPE_HARDWARE, paint);
  }

  @Override
  public boolean performLongClick() {
    HitTestResult result = getHitTestResult();

    if (result.getType() == HitTestResult.SRC_ANCHOR_TYPE) {
      callback.webViewLongClick(result.getExtra());
      return true;
    }
    return super.performLongClick();
  }

  @Override
  protected void onCreateContextMenu(ContextMenu menu) {
    super.onCreateContextMenu(menu);
    final HitTestResult result = getHitTestResult();
    if (result.getType() == HitTestResult.IMAGE_ANCHOR_TYPE
        || result.getType() == HitTestResult.IMAGE_TYPE
        || result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
      MenuItem saveMenu = menu.add(0, 1, 0, getResources().getString(R.string.save_media));
      saveMenu.setOnMenuItemClickListener(item -> {
        Message msg = new SaveHandler().obtainMessage();
        requestFocusNodeHref(msg);
        return true;
      });
    }
  }

  @Override
  protected void onScrollChanged(int l, int t, int oldl, int oldt) {
    super.onScrollChanged(l, t, oldl, oldt);
    int windowHeight;
    if (getMeasuredHeight() > 0) {
      windowHeight = getMeasuredHeight();
    } else {
      windowHeight = 1;
    }
    int pages = getContentHeight() / windowHeight;
    int page = t / windowHeight;

    callback.webViewPageChanged(page, pages);
  }

  public void disableZoomControls() {
    getSettings().setBuiltInZoomControls(true);
    getSettings().setDisplayZoomControls(false);
  }


}

