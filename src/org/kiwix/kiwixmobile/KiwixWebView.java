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

package org.kiwix.kiwixmobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class KiwixWebView extends WebView {

    private static final String PREF_ZOOM = "pref_zoom_slider";

    private static final String PREF_ZOOM_ENABLED = "pref_zoom_enabled";

    private static final float[] mNegativeColorArray = {-1.0f, 0, 0, 0, 255, // red
            0, -1.0f, 0, 0, 255, // green
            0, 0, -1.0f, 0, 255, // blue
            0, 0, 0, 1.0f, 0 // alpha
    };

    private OnPageChangeListener mChangeListener;

    private OnLongClickListener mOnLongClickListener;

    private Handler saveHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            String url = (String) msg.getData().get("url");
            String src = (String) msg.getData().get("src");

            if (url != null || src != null) {
                url = url == null ? src : url;
                url = url.substring(url.lastIndexOf('/') + 1, url.length());
                url = url.substring(url.indexOf("%3A") + 3, url.length());
                int dotIndex = url.lastIndexOf('.');

                File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), url);

                String newUrl = url;
                for (int i = 2; storageDir.exists(); i++) {
                    newUrl = url.substring(0, dotIndex) + "_" + i
                            + url.substring(dotIndex, url.length());
                    storageDir = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES), newUrl);
                }

                Uri source = Uri.parse(src);
                String toastText;

                try {
                    InputStream input = getContext().getContentResolver().openInputStream(source);
                    OutputStream output = new FileOutputStream(storageDir);

                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = input.read(buffer)) > 0) {
                        output.write(buffer, 0, len);
                    }
                    input.close();
                    output.close();

                    String imageSaved = getResources().getString(R.string.save_media_saved);
                    toastText = String.format(imageSaved, newUrl);
                } catch (IOException e) {
                    Log.d("kiwix", "Couldn't save image", e);
                    toastText = getResources().getString(R.string.save_media_error);
                }

                Toast.makeText(getContext(), toastText, Toast.LENGTH_LONG).show();
            }
        }
    };

    public KiwixWebView(Context context) {
        super(context);
    }

    public KiwixWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public KiwixWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void loadPrefs() {
        disableZoomControls();

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getContext());
        boolean zoomEnabled = sharedPreferences.getBoolean(PREF_ZOOM_ENABLED, false);

        if (zoomEnabled) {
            int zoomScale = (int) sharedPreferences.getFloat(PREF_ZOOM, 100.0f);
            setInitialScale(zoomScale);
        } else {
            setInitialScale(0);
        }
    }

    public void deactiviateNightMode() {
        setLayerType(View.LAYER_TYPE_NONE, null);
    }

    public void toggleNightMode() {

        Paint paint = new Paint();
        ColorMatrixColorFilter filterInvert = new ColorMatrixColorFilter(mNegativeColorArray);
        paint.setColorFilter(filterInvert);

        setLayerType(View.LAYER_TYPE_HARDWARE, paint);
        try {
            InputStream stream = getContext().getAssets().open("invertcode.js");
            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            String JSInvert = new String(buffer);

            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                evaluateJavascript("javascript:" + JSInvert, null);
            } else {
                //loadUrl("javascript:" + JSInvert);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean performLongClick() {
        HitTestResult result = getHitTestResult();

        if (result.getType() == HitTestResult.SRC_ANCHOR_TYPE) {
            mOnLongClickListener.onLongClick(result.getExtra());
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
            saveMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                public boolean onMenuItemClick(android.view.MenuItem item) {
                    Message msg = saveHandler.obtainMessage();
                    requestFocusNodeHref(msg);
                    return true;
                }
            });
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        int windowHeight = getMeasuredHeight();
        int pages = getContentHeight() / windowHeight;
        int page = t / windowHeight;

        // Alert the listener
        if (mChangeListener != null) {
            mChangeListener.onPageChanged(page, pages);
        }
    }

    public void disableZoomControls() {

        getSettings().setBuiltInZoomControls(true);
        getSettings().setDisplayZoomControls(false);
    }

    public void setOnPageChangedListener(OnPageChangeListener listener) {
        mChangeListener = listener;
    }

    public void setOnLongClickListener(OnLongClickListener listener) {
        mOnLongClickListener = listener;
    }

    public interface OnPageChangeListener {

        void onPageChanged(int page, int maxPages);
    }

    public interface OnLongClickListener {

        void onLongClick(String url);
    }
}

