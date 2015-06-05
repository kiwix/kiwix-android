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
import android.util.AttributeSet;
import android.webkit.WebView;
import android.widget.ZoomButtonsController;


public class KiwixWebView extends WebView {

    private OnPageChangeListener mChangeListener;

    private ZoomButtonsController zoomControll = null;

    public KiwixWebView(Context context) {
        super(context);
        init();
    }

    public KiwixWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public KiwixWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setSupportMultipleWindows(true);
        getSettings().setSupportZoom(true);
        disableZoomControlls();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        int windowHeight = getMeasuredHeight();

        // It seems that in a few cases, getMeasuredHeight() returns 0
        if (windowHeight > 0) {
            int pages = getContentHeight() / windowHeight;
            int page = t / windowHeight;
            if (mChangeListener != null) {
                mChangeListener.onPageChanged(page, pages);
            }
        }
    }

    public void disableZoomControlls() {

        getSettings().setBuiltInZoomControls(true);
        getSettings().setDisplayZoomControls(false);
    }

    public void setOnPageChangedListener(OnPageChangeListener listener) {
        mChangeListener = listener;
    }


    public interface OnPageChangeListener {

        void onPageChanged(int page, int maxPages);
    }
}
