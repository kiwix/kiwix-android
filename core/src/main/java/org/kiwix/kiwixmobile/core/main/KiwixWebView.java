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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
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
import io.reactivex.disposables.CompositeDisposable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.core.BuildConfig;
import org.kiwix.kiwixmobile.core.CoreApp;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer;
import org.kiwix.kiwixmobile.core.utils.LanguageUtils;
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil;

@SuppressLint("ViewConstructor")
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
  private final WebViewCallback callback;
  private CompositeDisposable compositeDisposable = new CompositeDisposable();

  @SuppressLint("SetJavaScriptEnabled")
  public KiwixWebView(Context context, WebViewCallback callback, AttributeSet attrs,
    ViewGroup nonVideoView, ViewGroup videoView, CoreWebViewClient webViewClient) {
    super(context, attrs);
    if (BuildConfig.DEBUG) {
      setWebContentsDebuggingEnabled(true);
    }
    this.callback = callback;
    CoreApp.getCoreComponent().inject(this);
    // Set the user agent to the current locale so it can be read with navigator.userAgent
    final WebSettings settings = getSettings();
    settings.setUserAgentString(LanguageUtils.getCurrentLocale(context).toString());
    settings.setDomStorageEnabled(true);
    settings.setJavaScriptEnabled(true);
    settings.setLoadWithOverviewMode(true);
    settings.setUseWideViewPort(true);
    setInitialScale(100);
    settings.setBuiltInZoomControls(true);
    settings.setDisplayZoomControls(false);
    clearCache(true);
    settings.setAllowUniversalAccessFromFileURLs(true);
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
        Message msg = new SaveHandler(zimReaderContainer).obtainMessage();
        requestFocusNodeHref(msg);
        return true;
      });
    }
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    compositeDisposable.add(
      sharedPreferenceUtil.getTextZooms()
        .subscribe(textZoom -> getSettings().setTextZoom(textZoom))
    );
  }

  @Override protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    compositeDisposable.clear();
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

  static class SaveHandler extends Handler {
    private final ZimReaderContainer zimReaderContainer;

    public SaveHandler(ZimReaderContainer zimReaderContainer) {
      this.zimReaderContainer = zimReaderContainer;
    }

    private String getDecodedFileName(String url, String src) {
      String fileName = "";
      if (url != null) {
        fileName = url.substring(url.lastIndexOf('/') + 1);
      }
      // If url is not a valid file name use src if it isn't null
      if (!fileName.contains(".") && src != null) {
        fileName = src.substring(src.lastIndexOf('/') + 1);
      }
      return fileName.substring(fileName.indexOf("%3A") + 1);
    }

    @Override
    public void handleMessage(Message msg) {
      String url = (String) msg.getData().get("url");
      String src = (String) msg.getData().get("src");

      if (url != null || src != null) {
        String fileName = getDecodedFileName(url, src);
        int dotIndex = fileName.lastIndexOf('.');

        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        if (CoreApp.getInstance().getExternalMediaDirs().length > 0) {
          root = CoreApp.getInstance().getExternalMediaDirs()[0];
        }

        File storageDir = new File(root, fileName);
        String newUrl = fileName;
        for (int i = 2; storageDir.exists(); i++) {
          newUrl = fileName.substring(0, dotIndex) + "_" + i + fileName.substring(dotIndex);
          storageDir = new File(root, newUrl);
        }

        Uri source = Uri.parse(src);
        String toastText;

        try {
          InputStream input =
            zimReaderContainer.load(source.toString()).getData();
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
