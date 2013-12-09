package org.kiwix.kiwixmobile;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebView;

/**
 * Created by sakchham on 20/11/13.
 */
/*
 * Custom version of link{@android.webkit.WebView}
 * to get scroll positions for implimenting the Back to top
 */
public class KiwixWebView extends WebView
{
    OnPageChangeListener changeListener = null;

    public KiwixWebView(Context context) {
        super(context);
    }

    public KiwixWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public KiwixWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        int windowHeight = getMeasuredHeight();
        int pages = getContentHeight()/windowHeight;
        int page = t/windowHeight;
        //alert the listeners
        if (changeListener != null);
            changeListener.onPageChanged(page,pages);
    }

    public interface OnPageChangeListener {
        public void onPageChanged(int page,int maxPages);
    }

    /*
     * We wouldn't be needing more than one so doesn't matter if it wont work for more
     * than one.
     */
    public void registerOnPageChangedListener(OnPageChangeListener listener)
    {
        changeListener = listener;
    }

}
