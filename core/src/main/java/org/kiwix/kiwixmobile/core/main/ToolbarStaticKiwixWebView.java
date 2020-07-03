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
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * {@link KiwixWebView} which keeps the app bar fixed.
 */

@SuppressLint("ViewConstructor")
public class ToolbarStaticKiwixWebView extends KiwixWebView {
  public ToolbarStaticKiwixWebView(Context context,
    WebViewCallback callback, AttributeSet attrs, ViewGroup nonVideoView,
    ViewGroup videoView,
    CoreWebViewClient webViewClient) {
    super(context, callback, attrs, nonVideoView, videoView, webViewClient);
  }

  //private final int heightDifference;
  //private final SharedPreferenceUtil sharedPreferenceUtil;
  //
  //public ToolbarStaticKiwixWebView(Context context, WebViewCallback callback,
  //  AttributeSet attrs, ViewGroup nonVideoView, ViewGroup videoView,
  //  CoreWebViewClient webViewClient,
  //  SharedPreferenceUtil sharedPreferenceUtil) {
  //  super(context, callback, attrs, nonVideoView, videoView, webViewClient);
  //  heightDifference = DimenUtils.getToolbarHeight(context);
  //  this.sharedPreferenceUtil = sharedPreferenceUtil;
  //  setTranslationY(heightDifference);
  //}
  //
  //@Override
  //protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
  //  if (sharedPreferenceUtil.getPrefFullScreen()) {
  //    setTranslationY(0);
  //    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  //  } else {
  //    setTranslationY(heightDifference);
  //    super.onMeasure(widthMeasureSpec, heightMeasureSpec - heightDifference);
  //  }
  //}
}
