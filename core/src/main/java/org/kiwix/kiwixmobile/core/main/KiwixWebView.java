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

import android.app.Activity;
import android.content.Context;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.widget.Toast;
import com.cprcrack.videowebview.VideoEnabledWebView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.core.CoreApp;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.utils.LanguageUtils;
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil;
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer;

public class KiwixWebView extends VideoEnabledWebView {
  public static final float[] NIGHT_MODE_COLORS = {
    -1.0f, 0, 0, 0, 255, // red
    0, -1.0f, 0, 0, 255, // green
    0, 0, -1.0f, 0, 255, // blue
    0, 0, 0, 1.0f, 0 // alpha
  };
  @Inject
  SharedPreferenceUtil sharedPreferenceUtil;
  @Inject
  ZimReaderContainer zimReaderContainer;
  private WebViewCallback callback;

  public KiwixWebView(Context context) {
    super(context);
  }

  public KiwixWebView(Context context, WebViewCallback callback, AttributeSet attrs,
    ViewGroup nonVideoView, ViewGroup videoView, CoreWebViewClient webViewClient) {
    super(context, attrs);
    this.callback = callback;
    CoreApp.getCoreComponent().inject(this);
    // Set the user agent to the current locale so it can be read with navigator.userAgent
    final WebSettings settings = getSettings();
    settings.setUserAgentString(LanguageUtils.getCurrentLocale(context).toString());
    settings.setDomStorageEnabled(true);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      settings.setAllowUniversalAccessFromFileURLs(true);
    }
    setWebViewClient(webViewClient);
    final KiwixWebChromeClient client =
      new KiwixWebChromeClient(callback, nonVideoView, videoView, this);
    client.setOnToggledFullscreen(fullscreen ->
      setWindowVisibility(fullscreen ? SYSTEM_UI_FLAG_LOW_PROFILE : SYSTEM_UI_FLAG_VISIBLE));
    setWebChromeClient(client);
  }

  private void setWindowVisibility(int systemUiVisibility) {
    ((Activity) getContext()).getWindow().getDecorView().setSystemUiVisibility(systemUiVisibility);
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
    setLayerType(LAYER_TYPE_NONE, null);
  }

  public void toggleNightMode() {
    Paint paint = new Paint();
    ColorMatrixColorFilter filterInvert = new ColorMatrixColorFilter(NIGHT_MODE_COLORS);
    paint.setColorFilter(filterInvert);
    setLayerType(LAYER_TYPE_HARDWARE, paint);
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

  static class SaveHandler extends Handler {

    @Override
    public void handleMessage(Message msg) {
      String url = (String) msg.getData().get("url");
      String src = (String) msg.getData().get("src");

      if (url != null || src != null) {
        url = url == null ? src : url;
        url = url.substring(url.lastIndexOf('/') + 1);
        url = url.substring(url.indexOf("%3A") + 1);
        int dotIndex = url.lastIndexOf('.');

        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP
          && CoreApp.getInstance().getExternalMediaDirs().length > 0) {
          root = CoreApp.getInstance().getExternalMediaDirs()[0];
        }

        File storageDir = new File(root, url);
        String newUrl = url;
        for (int i = 2; storageDir.exists(); i++) {
          newUrl = url.substring(0, dotIndex) + "_" + i + url.substring(dotIndex);
          storageDir = new File(root, newUrl);
        }

        Uri source = Uri.parse(src);
        String toastText;

        try {
          InputStream input =
            CoreApp.getInstance().getContentResolver().openInputStream(source);
          OutputStream output = new FileOutputStream(storageDir);

          byte[] buffer = new byte[1024];
          int len;
          if (input != null) {
            while ((len = input.read(buffer)) > 0) {
              output.write(buffer, 0, len);
            }
            input.close();
          }
          output.close();

          String imageSaved = CoreApp.getInstance().getString(R.string.save_media_saved);
          toastText = String.format(imageSaved, newUrl);
        } catch (IOException e) {
          Log.w("kiwix", "Couldn't save image", e);
          toastText = CoreApp.getInstance().getString(R.string.save_media_error);
        }

        Toast.makeText(CoreApp.getInstance(), toastText, Toast.LENGTH_LONG).show();
      }
    }
  }
}
