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
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.COMPOSE_BOTTOM_APP_BAR_DEFAULT_HEIGHT
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.COMPOSE_TOOLBAR_DEFAULT_HEIGHT
import org.kiwix.kiwixmobile.core.utils.DimenUtils.dpToPx
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import kotlin.math.max
import kotlin.math.min

@SuppressLint("ViewConstructor")
@Suppress("UnusedPrivateProperty")
class ToolbarScrollingKiwixWebView @JvmOverloads constructor(
  context: Context,
  callback: WebViewCallback,
  attrs: AttributeSet,
  videoView: ViewGroup?,
  webViewClient: CoreWebViewClient,
  private val onToolbarOffsetChanged: ((Float) -> Unit)? = null,
  private val onBottomAppBarOffsetChanged: ((Float) -> Unit)? = null,
  sharedPreferenceUtil: SharedPreferenceUtil,
  private val parentNavigationBar: View? = null
) : KiwixWebView(
    context,
    callback,
    attrs,
    videoView,
    webViewClient,
    sharedPreferenceUtil
  ) {
  private val toolbarHeight = context.dpToPx(COMPOSE_TOOLBAR_DEFAULT_HEIGHT)
  private val bottomAppBarHeightPx = context.dpToPx(COMPOSE_BOTTOM_APP_BAR_DEFAULT_HEIGHT)

  private var startY = 0f
  private var currentOffset = 0f

  init {
    fixInitalScrollingIssue()
  }

  /**
   * Adjusts the internal offset of the WebView based on scroll delta.
   *
   * Positive scrollDelta = user scrolling down (hide UI)
   * Negative scrollDelta = user scrolling up (show UI)
   */

  private fun moveToolbar(scrollDelta: Int): Boolean {
    val newOffset = when {
      scrollDelta > 0 -> max(-toolbarHeight.toFloat(), currentOffset - scrollDelta)
      else -> min(0f, currentOffset - scrollDelta)
    }

    if (newOffset != currentOffset) {
      currentOffset = newOffset
      notifyOffsetChanged(newOffset)
      return true
    }

    return false
  }

  /**
   * Notifies Compose UI about toolbar offset.
   */
  private fun notifyOffsetChanged(offset: Float) {
    onToolbarOffsetChanged?.invoke(offset)

    // Compute offset for bottomAppBar using height ratio
    val bottomOffset = offset * -1 * (bottomAppBarHeightPx.toFloat() / toolbarHeight)
    onBottomAppBarOffsetChanged?.invoke(bottomOffset)

    // Optional: Animate parent navigation bar (if still using it)
    parentNavigationBar?.let { view ->
      val offsetFactor = view.height / toolbarHeight.toFloat()
      view.translationY = offset * -1 * offsetFactor
    }

    // Adjust WebView position to prevent layout jump
    this.translationY = offset
  }

  /**
   * The webview needs to be scrolled with 0 to not be slightly hidden on startup.
   * See https://github.com/kiwix/kiwix-android/issues/2304 for issue description.
   */
  private fun fixInitalScrollingIssue() {
    moveToolbar(0)
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (sharedPreferenceUtil.prefFullScreen) return super.onTouchEvent(event)

    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        startY = event.rawY
      }

      MotionEvent.ACTION_MOVE -> {
        if (event.pointerCount == 1) {
          val diffY = (event.rawY - startY).toInt()
          startY = event.rawY
          if (moveToolbar(-diffY)) {
            event.offsetLocation(0f, -diffY.toFloat())
            return super.onTouchEvent(event)
          }
        }
      }
    }

    return super.onTouchEvent(event)
  }
}
