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
package org.kiwix.kiwixmobile.core.main

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import org.kiwix.kiwixmobile.core.utils.DimenUtils.getToolbarHeight
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

@SuppressLint("ViewConstructor")
class ToolbarScrollingKiwixWebView(
  context: Context,
  callback: WebViewCallback,
  attrs: AttributeSet,
  nonVideoView: ViewGroup,
  videoView: ViewGroup,
  webViewClient: CoreWebViewClient,
  private val toolbarView: View,
  private val bottomBarView: View,
  override var sharedPreferenceUtil: SharedPreferenceUtil
) : KiwixWebView(context, callback, attrs, nonVideoView, videoView, webViewClient) {
  private val toolbarHeight = getContext().getToolbarHeight()
  private var parentNavigationBar: View? = null
  private var startY = 0f

  constructor(
    context: Context,
    callback: WebViewCallback,
    attrs: AttributeSet,
    nonVideoView: ViewGroup,
    videoView: ViewGroup,
    webViewClient: CoreWebViewClient,
    toolbarView: View,
    bottomBarView: View,
    parentNavigationBar: View?,
    sharedPreferenceUtil: SharedPreferenceUtil
  ) : this(
    context, callback, attrs, nonVideoView, videoView, webViewClient, toolbarView,
    bottomBarView, sharedPreferenceUtil
  ) {
    this.parentNavigationBar = parentNavigationBar
  }

  init {
    fixInitalScrollingIssue()
  }

  /**
   * The webview needs to be scrolled with 0 to not be slightly hidden on startup.
   * See https://github.com/kiwix/kiwix-android/issues/2304 for issue description.
   */
  private fun fixInitalScrollingIssue() {
    moveToolbar(0)
  }

  private fun moveToolbar(scrollDelta: Int): Boolean {
    val originalTranslation = toolbarView.translationY
    val newTranslation = if (scrollDelta > 0) {
      // scroll down
      Math.max(-toolbarHeight.toFloat(), originalTranslation - scrollDelta)
    } else {
      // scroll up
      Math.min(0f, originalTranslation - scrollDelta)
    }
    toolbarView.translationY = newTranslation
    bottomBarView.translationY =
      newTranslation * -1 * (bottomBarView.height / toolbarHeight.toFloat())
    parentNavigationBar?.let {
      it.translationY = newTranslation * -1 * (it.height / toolbarHeight.toFloat())
    }
    this.translationY = newTranslation + toolbarHeight
    return toolbarHeight + newTranslation != 0f && newTranslation != 0f
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    val transY = toolbarView.translationY.toInt()
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> startY = event.rawY
      MotionEvent.ACTION_MOVE -> {
        // If we are in fullscreen don't scroll bar
        if (sharedPreferenceUtil.prefFullScreen) {
          return super.onTouchEvent(event)
        }
        // Filter out zooms since we don't want to affect the toolbar when zooming
        if (event.pointerCount == 1) {
          val diffY = (event.rawY - startY).toInt()
          startY = event.rawY
          if (moveToolbar(-diffY)) {
            event.offsetLocation(0f, -diffY.toFloat())
            return super.onTouchEvent(event)
          }
        }
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (transY != 0 && transY > -toolbarHeight) {
        if (transY > -toolbarHeight / 2) {
          ensureToolbarDisplayed()
        } else {
          ensureToolbarHidden()
        }
      }
      else -> {
      }
    }
    return super.onTouchEvent(event)
  }

  private fun ensureToolbarDisplayed() {
    moveToolbar(-toolbarHeight)
  }

  private fun ensureToolbarHidden() {
    moveToolbar(toolbarHeight)
  }
}
