package org.kiwix.kiwixmobile.views.web;

import android.content.Context;
import android.view.ViewGroup;

import org.kiwix.kiwixmobile.WebViewCallback;
import org.kiwix.kiwixmobile.utils.DimenUtils;

/**
 * Created by gmon on 1/14/17.
 */

public class ToolbarStaticKiwixWebView extends KiwixWebView {

  private int heightDifference;

  public ToolbarStaticKiwixWebView(Context context, WebViewCallback callback, ViewGroup toolbarLayout) {
    super(context, callback);
    toolbarLayout.setTranslationY(DimenUtils.getTranslucentStatusBarHeight(context));
    heightDifference = DimenUtils.getToolbarAndStatusBarHeight(context);
    setTranslationY(heightDifference);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec - heightDifference);
  }
}
