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

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import org.kiwix.kiwixmobile.core.utils.DimenUtils;
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil;

public class ToolbarScrollingKiwixWebView extends KiwixWebView {

  private final int toolbarHeight = DimenUtils.getToolbarHeight(getContext());
  private View toolbarView;
  private View bottomBarView;
  private SharedPreferenceUtil sharedPreferenceUtil;
  private float startY;

  public ToolbarScrollingKiwixWebView(Context context) {
    super(context);
  }

  public ToolbarScrollingKiwixWebView(Context context, WebViewCallback callback, AttributeSet attrs,
    ViewGroup nonVideoView, ViewGroup videoView, CoreWebViewClient webViewClient, View toolbarView,
    View bottomBarView, SharedPreferenceUtil sharedPreferenceUtil) {
    super(context, callback, attrs, nonVideoView, videoView, webViewClient);
    this.toolbarView = toolbarView;
    this.bottomBarView = bottomBarView;
    this.sharedPreferenceUtil = sharedPreferenceUtil;
  }

  private boolean moveToolbar(int scrollDelta) {
    float newTranslation;
    float originalTranslation = toolbarView.getTranslationY();
    if (scrollDelta > 0) {
      // scroll down
      newTranslation = Math.max(-toolbarHeight, originalTranslation - scrollDelta);
    } else {
      // scroll up
      newTranslation = Math.min(0, originalTranslation - scrollDelta);
    }

    toolbarView.setTranslationY(newTranslation);
    bottomBarView.setTranslationY(
      newTranslation * -1 * (bottomBarView.getHeight() / (float) (toolbarHeight)));
    this.setTranslationY(newTranslation + toolbarHeight);
    return toolbarHeight + newTranslation != 0 && newTranslation != 0;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int transY = (int) toolbarView.getTranslationY();

    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        startY = event.getRawY();
        break;
      case MotionEvent.ACTION_MOVE:
        // If we are in fullscreen don't scroll bar
        if (sharedPreferenceUtil.getPrefFullScreen()) {
          return super.onTouchEvent(event);
        }
        // Filter out zooms since we don't want to affect the toolbar when zooming
        if (event.getPointerCount() == 1) {
          int diffY = (int) (event.getRawY() - startY);
          startY = event.getRawY();
          if (moveToolbar(-diffY)) {
            event.offsetLocation(0, -diffY);
            return super.onTouchEvent(event);
          }
        }
        break;
      // If the toolbar is half-visible,
      // either open or close it entirely depending on how far it is visible
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        if (transY != 0 && transY > -toolbarHeight) {
          if (transY > (-toolbarHeight) / 2) {
            ensureToolbarDisplayed();
          } else {
            ensureToolbarHidden();
          }
        }
        break;
      default:
        // Do nothing for all the other things
        break;
    }
    return super.onTouchEvent(event);
  }

  public void ensureToolbarDisplayed() {
    moveToolbar(-toolbarHeight);
  }

  private void ensureToolbarHidden() {
    moveToolbar(toolbarHeight);
  }
}
