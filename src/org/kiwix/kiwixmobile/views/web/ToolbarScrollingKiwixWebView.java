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

package org.kiwix.kiwixmobile.views.web;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import org.kiwix.kiwixmobile.WebViewCallback;
import org.kiwix.kiwixmobile.utils.DimenUtils;

public class ToolbarScrollingKiwixWebView extends KiwixWebView {

  private View toolbarView;

  private boolean isZooming = false;
  private float startY;

  public ToolbarScrollingKiwixWebView(Context context, WebViewCallback callback, View toolbarView) {
    super(context, callback);

    this.toolbarView = toolbarView;
  }

  @Override
  protected void onScrollChanged(int l, int t, int oldl, int oldt) {
    moveToolbar(t, oldt);
    super.onScrollChanged(l, t, oldl, oldt);
  }

  protected void moveToolbar(int t, int oldt) {
    if (!isZooming) {
      int animMargin = 0;
      int toolbarHeight = DimenUtils.getToolbarHeight(getContext()) +
              DimenUtils.getTranslucentStatusBarHeight(getContext());
      int scrollDelta = t - oldt;
      if (oldt > t) {
        // scroll up
        animMargin = Math.min(0, (int) toolbarView.getTranslationY() - scrollDelta);
      } else {
        // scroll down
        animMargin = Math.max(-toolbarHeight,
                (int) toolbarView.getTranslationY() - scrollDelta);
      }

      toolbarView.setTranslationY(animMargin);
      if (getScrollY() == 0) this.setTranslationY(toolbarView.getY() + toolbarHeight);
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    isZooming = false;
    int transY = (int) toolbarView.getTranslationY();

    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        startY = event.getRawY();
        break;
      case MotionEvent.ACTION_MOVE:
        // Filter out zooms since we don't want to affect the toolbar when zooming
        if (event.getPointerCount() > 1) isZooming = true;
        // Handle toolbar appearance when the KiwixWebView has reached the top
        else if (this.getScrollY() == 0) {
          int diffY = (int) (event.getRawY() - startY);
          moveToolbar(0, diffY);
        }
        break;
      // If the toolbar is half-visible,
      // either open or close it entirely depending on how far it is visible
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        int height = DimenUtils.getToolbarHeight(getContext());
        if (transY != 0 && transY > -height) {
          if (transY > -height / 2) {
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
    moveToolbar(0, DimenUtils.getToolbarHeight(getContext()));
  }

  public void ensureToolbarHidden() {
    moveToolbar(DimenUtils.getToolbarHeight(getContext()), 0);
  }
}
