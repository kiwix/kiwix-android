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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.WebViewCallback;
import org.kiwix.kiwixmobile.utils.DimenUtils;

public class ToolbarScrollingKiwixWebView extends KiwixWebView {

  private final int toolbarHeight = DimenUtils.getToolbarHeight(getContext());
  private View toolbarView;
  private View bottombarView;
  private OnToolbarVisibilityChangeListener listener;
  private float startY;

  public ToolbarScrollingKiwixWebView(Context context, WebViewCallback callback, View toolbarView, View bottombarView, AttributeSet attrs) {
    super(context, callback, attrs);
    this.toolbarView = toolbarView;
    this.bottombarView = bottombarView;
  }

  protected boolean moveToolbar(int scrollDelta) {
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
    bottombarView.setTranslationY(newTranslation * -1 * (bottombarView.getHeight() / (float) (toolbarHeight)));
    this.setTranslationY(newTranslation + toolbarHeight);
    if (listener != null && newTranslation != originalTranslation) {
      if (newTranslation == -toolbarHeight) {
        listener.onToolbarHidden();
      } else if (newTranslation == 0) {
        listener.onToolbarDisplayed();
      }
    }

    return newTranslation != -toolbarHeight && newTranslation != 0;
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
        if (KiwixMobileActivity.isFullscreenOpened) {
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

  public void ensureToolbarHidden() {
    moveToolbar(toolbarHeight);
  }

  public void setOnToolbarVisibilityChangeListener(OnToolbarVisibilityChangeListener listener) {
    this.listener = listener;
  }

  public interface OnToolbarVisibilityChangeListener {
    void onToolbarDisplayed();
    void onToolbarHidden();
  }
}
