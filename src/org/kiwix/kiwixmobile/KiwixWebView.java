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
import android.view.MotionEvent;
import android.webkit.WebView;
import android.widget.ZoomButtonsController;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.os.Build;
import java.lang.reflect.Method;

import static org.kiwix.kiwixmobile.BackwardsCompatibilityTools.newApi;

public class KiwixWebView extends WebView {

    private OnPageChangeListener mChangeListener;

    private OnLongClickListener mOnLongClickListener;

    private ZoomButtonsController zoomControll = null;

    private boolean mDisableZoomControlls;

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
	getSettings().setSupportMultipleWindows(true);
	getSettings().setSupportZoom(true);
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
	    getSettings().setLayoutAlgorithm(LayoutAlgorithm.TEXT_AUTOSIZING);
	}
    }

    @Override
    public boolean performLongClick() {
        WebView.HitTestResult result = getHitTestResult();

        if (result.getType() == HitTestResult.SRC_ANCHOR_TYPE) {
            mOnLongClickListener.onLongClick(result.getExtra());
            return true;
        }
        return super.performLongClick();
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

    public void disableZoomControlls(boolean disable) {

        mDisableZoomControlls = disable;

        if (newApi()) {
            getSettings().setBuiltInZoomControls(true);
            getSettings().setDisplayZoomControls(disable);
        } else {
            getZoomControlls();
        }
    }

    // Use reflection to hide the zoom controlls
    private void getZoomControlls() {
        try {
            Class webview = Class.forName("android.webkit.WebView");
            Method method = webview.getMethod("getZoomButtonsController");
            zoomControll = (ZoomButtonsController) method.invoke(this, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);

        if (zoomControll != null) {
            zoomControll.setVisible(mDisableZoomControlls);
        }
        return true;
    }

    public void setOnPageChangedListener(OnPageChangeListener listener) {
        mChangeListener = listener;
    }

    public void setOnLongClickListener(OnLongClickListener listener) {
        mOnLongClickListener = listener;
    }

    public interface OnPageChangeListener {
        public void onPageChanged(int page, int maxPages);
    }

    public interface OnLongClickListener {

        public void onLongClick(String url);
    }
}
