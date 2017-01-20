package org.kiwix.kiwixmobile.views.web;

import android.content.Context;
import android.view.ViewGroup;

import org.kiwix.kiwixmobile.WebViewCallback;
import org.kiwix.kiwixmobile.utils.DimenUtils;

/**
 * Created by gmon on 1/14/17.
 */

public class ToolbarStaticKiwixWebView extends KiwixWebView {
  public ToolbarStaticKiwixWebView(Context context, WebViewCallback callback) {
    super(context, callback);

    float heightDifference = DimenUtils.getToolbarAndStatusBarHeight(context);
    setTranslationY(heightDifference);

    ViewGroup.LayoutParams layoutParams = getLayoutParams();
    layoutParams.height -= heightDifference;
    setLayoutParams(layoutParams);
  }
}
