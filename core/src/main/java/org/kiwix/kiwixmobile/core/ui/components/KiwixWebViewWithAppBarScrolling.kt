/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.ui.components

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import kotlin.math.abs

// Minimum scroll to trigger AppBar movement
private const val MIN_SCROLL_DELTA = 4f

// Scale down accumulated scroll
private const val SCROLL_CONSUME_FACTOR = 0.35f

// Delay before settling AppBars in ms
private const val DEFAULT_SETTLE_DELAY = 100L
private const val COLLAPSED_FRACTION_MIN = 0.01f
private const val COLLAPSED_FRACTION_MAX = 1f
private const val COLLAPSED_FRACTION_MID = 0.5f
private const val HEIGHT_OFFSET_EXPANDED = 0f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KiwixWebViewWithAppBarScrolling(
  kiwixWebView: KiwixWebView,
  topAppBarScrollBehavior: TopAppBarScrollBehavior,
  bottomAppBarScrollBehavior: BottomAppBarScrollBehavior,
  shouldUpdateAppBars: MutableState<Boolean>
) {
  var accumulatedScroll by remember { mutableFloatStateOf(0f) }
  val scope = rememberCoroutineScope()
  val settleJob = remember { mutableStateOf<Job?>(null) }

  key(kiwixWebView) {
    DisposableEffect(Unit) {
      val listener = createTouchListener(
        topAppBarScrollBehavior,
        bottomAppBarScrollBehavior,
        shouldUpdateAppBars,
        { accumulatedScroll },
        { accumulatedScroll = it },
        scope,
        settleJob
      )

      kiwixWebView.setOnTouchListener(listener)
      onDispose { kiwixWebView.setOnTouchListener(null) }
    }

    AndroidView(
      factory = { context ->
        FrameLayout(context).apply {
          (kiwixWebView.parent as? ViewGroup)?.removeView(kiwixWebView)
          kiwixWebView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
          )
          addView(kiwixWebView)
        }
      },
      modifier = Modifier.fillMaxSize()
    )
  }
}

@SuppressLint("ClickableViewAccessibility")
@OptIn(ExperimentalMaterial3Api::class)
private fun createTouchListener(
  topAppBarScrollBehavior: TopAppBarScrollBehavior,
  bottomAppBarScrollBehavior: BottomAppBarScrollBehavior,
  shouldUpdateScroll: MutableState<Boolean>,
  accumulatedScroll: () -> Float,
  updateAccumulatedScroll: (Float) -> Unit,
  scope: CoroutineScope,
  settleJob: MutableState<Job?>
): View.OnTouchListener {
  var lastY = 0f

  return View.OnTouchListener { _, event ->
    if (!shouldUpdateScroll.value) return@OnTouchListener false

    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        lastY = event.y
        settleJob.value?.cancel()
      }

      MotionEvent.ACTION_MOVE -> {
        val deltaY = event.y - lastY
        lastY = event.y
        val accumulated = accumulatedScroll() + deltaY

        if (abs(accumulated) < MIN_SCROLL_DELTA) {
          updateAccumulatedScroll(accumulated)
          return@OnTouchListener false
        }

        val scroll = accumulated * SCROLL_CONSUME_FACTOR
        updateAccumulatedScroll(0f)

        consumeScroll(
          scroll,
          topAppBarScrollBehavior,
          bottomAppBarScrollBehavior
        )
      }

      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_CANCEL -> {
        settleJob.value?.cancel()
        settleJob.value = scope.launch {
          delay(DEFAULT_SETTLE_DELAY)
          settleTopAppBarWithoutVelocity(topAppBarScrollBehavior)
          settleBottomAppBarWithoutVelocity(bottomAppBarScrollBehavior)
        }
      }
    }

    false
  }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun consumeScroll(
  scroll: Float,
  topAppBarScrollBehavior: TopAppBarScrollBehavior,
  bottomAppBarScrollBehavior: BottomAppBarScrollBehavior
) {
  val topState = topAppBarScrollBehavior.state
  val bottomState = bottomAppBarScrollBehavior.state

  val topCanConsume =
    (scroll < 0 && topState.heightOffset > topState.heightOffsetLimit) ||
      (scroll > 0 && topState.heightOffset < 0f)

  if (topCanConsume) {
    topState.heightOffset =
      (topState.heightOffset + scroll)
        .coerceIn(topState.heightOffsetLimit, HEIGHT_OFFSET_EXPANDED)
  } else {
    bottomState.heightOffset =
      (bottomState.heightOffset + scroll)
        .coerceIn(bottomState.heightOffsetLimit, HEIGHT_OFFSET_EXPANDED)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
private suspend fun settleTopAppBarWithoutVelocity(scrollBehavior: TopAppBarScrollBehavior) {
  val state = scrollBehavior.state
  if (state.collapsedFraction < COLLAPSED_FRACTION_MIN ||
    state.collapsedFraction == COLLAPSED_FRACTION_MAX
  ) {
    return
  }

  val snapSpec = scrollBehavior.snapAnimationSpec ?: return
  if (state.heightOffset < 0f && state.heightOffset > state.heightOffsetLimit) {
    val targetOffset =
      if (state.collapsedFraction < COLLAPSED_FRACTION_MID) {
        0f
      } else {
        state.heightOffsetLimit
      }

    AnimationState(initialValue = state.heightOffset)
      .animateTo(targetOffset, animationSpec = snapSpec) {
        state.heightOffset = value
      }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
private suspend fun settleBottomAppBarWithoutVelocity(scrollBehavior: BottomAppBarScrollBehavior) {
  val state = scrollBehavior.state
  val snapSpec = scrollBehavior.snapAnimationSpec ?: return

  if (state.collapsedFraction < COLLAPSED_FRACTION_MIN ||
    state.collapsedFraction == COLLAPSED_FRACTION_MAX
  ) {
    return
  }

  if (state.heightOffset < 0f && state.heightOffset > state.heightOffsetLimit) {
    val target =
      if (state.collapsedFraction < COLLAPSED_FRACTION_MID) {
        0f
      } else {
        state.heightOffsetLimit
      }

    AnimationState(initialValue = state.heightOffset)
      .animateTo(target, snapSpec) {
        state.heightOffset = value
      }
  }
}
