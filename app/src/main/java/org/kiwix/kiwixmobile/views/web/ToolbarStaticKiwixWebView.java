/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.views.web;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.WebViewCallback;
import org.kiwix.kiwixmobile.utils.DimenUtils;

/**
 * Created by gmon on 1/14/17.
 */

public class ToolbarStaticKiwixWebView extends KiwixWebView {

  private int heightDifference;

  public ToolbarStaticKiwixWebView(Context context, WebViewCallback callback, ViewGroup toolbarLayout, AttributeSet attrs) {
    super(context, callback, attrs);
    int statusBarHeight = DimenUtils.getTranslucentStatusBarHeight(context);
    toolbarLayout.setTranslationY(statusBarHeight);
    heightDifference = DimenUtils.getToolbarAndStatusBarHeight(context);
    setTranslationY(heightDifference);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if ((KiwixMobileActivity.isFullscreenOpened)) {
      setTranslationY(0);
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    } else {
      setTranslationY(heightDifference);
      super.onMeasure(widthMeasureSpec, heightMeasureSpec - heightDifference);
    }
  }
}
