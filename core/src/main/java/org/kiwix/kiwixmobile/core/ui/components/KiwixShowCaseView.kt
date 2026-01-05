/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.ui.theme.DodgerBlue
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.PULSE_ALPHA
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.PULSE_ANIMATION_END
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.PULSE_ANIMATION_START
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.PULSE_RADIUS_EXTRA
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SHOWCASE_MESSAGE_SHADOW_BLUR_RADIUS
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SHOWCASE_MESSAGE_SHADOW_COLOR_ALPHA
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SHOWCASE_VIEW_BACKGROUND_COLOR_ALPHA
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SHOWCASE_VIEW_MESSAGE_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SHOWCASE_VIEW_NEXT_BUTTON_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import kotlin.math.max
import kotlin.math.roundToInt

const val SHOWCASE_VIEW_ROUND_ANIMATION_DURATION = 2000
const val ONE = 1
const val TWO = 2
const val SIXTEEN = 16
const val TWELVE = 12

const val SHOWCASE_VIEW_NEXT_BUTTON_TESTING_TAG = "showcaseViewNextButtonTestingTag"
const val SHOWCASE_VIEW_MESSAGE_TESTING_TAG = "showCaseViewMessageTestingTag"

@Composable
fun KiwixShowCaseView(
  targets: SnapshotStateMap<String, ShowcaseProperty>,
  onShowCaseCompleted: () -> Unit
) {
  val orderedTargets = targets.values.sortedBy { it.index }
  var currentIndex by rememberSaveable { mutableStateOf(ZERO) }
  val currentTarget = orderedTargets.getOrNull(currentIndex)

  currentTarget?.let {
    AnimatedShowCase(target = it) {
      currentIndex++
      if (currentIndex >= orderedTargets.size) onShowCaseCompleted()
    }
  }
}

@Composable
private fun AnimatedShowCase(
  target: ShowcaseProperty,
  onShowCaseCompleted: () -> Unit
) {
  val targetRect = target.coordinates.boundsInRoot()
  val innerAnimation = remember { Animatable(PULSE_ANIMATION_START) }
  val density = LocalDensity.current

  val (width, height) = with(density) {
    val size = target.customSizeForShowcaseViewCircle?.toPx()
    Pair(size ?: targetRect.width, size ?: targetRect.height)
  }

  val radiusBase = max(width, height) / TWO.toFloat()
  val pulseRadius by innerAnimation.asState()

  LaunchedEffect(Unit) {
    innerAnimation.animateTo(
      targetValue = PULSE_ANIMATION_END,
      animationSpec = infiniteRepeatable(
        animation = tween(SHOWCASE_VIEW_ROUND_ANIMATION_DURATION, easing = FastOutLinearInEasing),
        repeatMode = RepeatMode.Restart
      )
    )
  }

  Canvas(
    modifier = Modifier
      .fillMaxSize()
      .pointerInput(target) {
        detectTapGestures {
          if (targetRect.contains(it)) onShowCaseCompleted()
        }
      }
      .graphicsLayer(alpha = PULSE_ALPHA)
  ) {
    drawOverlay(targetRect, radiusBase, pulseRadius)
  }

  ShowCaseMessage(target, targetRect, radiusBase)
  NextButton(onShowCaseCompleted)
}

/**
 * Draws the overlay and animated spotlight.
 */
private fun DrawScope.drawOverlay(
  targetRect: Rect,
  baseRadius: Float,
  animatedFraction: Float
) {
  drawRect(color = DodgerBlue.copy(alpha = SHOWCASE_VIEW_BACKGROUND_COLOR_ALPHA), size = size)
  drawCircle(
    color = Color.White,
    radius = baseRadius * (ONE + animatedFraction),
    center = targetRect.center,
    alpha = ONE - animatedFraction
  )
  drawCircle(
    color = Color.White,
    radius = baseRadius + PULSE_RADIUS_EXTRA,
    center = targetRect.center,
    blendMode = BlendMode.Clear
  )
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun ShowCaseMessage(
  target: ShowcaseProperty,
  targetRect: Rect,
  targetRadius: Float
) {
  val density = LocalDensity.current
  var offset by remember { mutableStateOf(Offset.Zero) }
  var calculated by remember { mutableStateOf(false) }

  BoxWithConstraints(Modifier.fillMaxSize()) {
    val screenWidth = with(density) { maxWidth.toPx() }
    val screenHeight = with(density) { maxHeight.toPx() }

    if (calculated) {
      Box(
        modifier = Modifier.offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
      ) {
        Text(
          text = target.showCaseMessage,
          color = target.showCaseMessageColor,
          style = TextStyle(
            fontSize = SHOWCASE_VIEW_MESSAGE_TEXT_SIZE,
            shadow = Shadow(
              Color.Black.copy(alpha = SHOWCASE_MESSAGE_SHADOW_COLOR_ALPHA),
              defaultBlurOffsetForMessageAndNextButton(),
              blurRadius = SHOWCASE_MESSAGE_SHADOW_BLUR_RADIUS
            )
          ),
          modifier = Modifier.semantics { testTag = SHOWCASE_VIEW_MESSAGE_TESTING_TAG }
        )
      }
    }

    Text(
      text = target.showCaseMessage,
      modifier = Modifier
        .alpha(PULSE_ANIMATION_START)
        .onGloballyPositioned {
          val size = it.size
          val width = size.width.toFloat()
          val height = size.height.toFloat()
          val center = targetRect.center

          val posY = when {
            screenHeight - (center.y + targetRadius) > height + SIXTEEN -> center.y + targetRadius + SIXTEEN
            center.y - targetRadius > height + SIXTEEN -> center.y - targetRadius - height - SIXTEEN
            else -> screenHeight / TWO - height / TWO
          }

          val posX = when {
            screenWidth - targetRect.right > width + SIXTEEN -> targetRect.right + SIXTEEN
            targetRect.left > width + SIXTEEN -> targetRect.left - width - SIXTEEN
            else -> screenWidth / TWO - width / TWO
          }

          offset = Offset(posX, posY)
          calculated = true
        }
    )
  }
}

private fun defaultBlurOffsetForMessageAndNextButton() =
  Offset(SHOWCASE_MESSAGE_SHADOW_COLOR_ALPHA, SHOWCASE_MESSAGE_SHADOW_COLOR_ALPHA)

/**
 * Composable for the "Next" button in the showcase.
 */
@Composable
private fun NextButton(onClick: () -> Unit) {
  val context = LocalContext.current
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(SIXTEEN_DP),
    verticalArrangement = Arrangement.Bottom,
    horizontalAlignment = Alignment.End
  ) {
    TextButton(
      onClick = onClick,
      modifier = Modifier.semantics { testTag = SHOWCASE_VIEW_NEXT_BUTTON_TESTING_TAG }
    ) {
      Text(
        text = context.getString(R.string.next),
        style = LocalTextStyle.current.copy(
          fontSize = SHOWCASE_VIEW_NEXT_BUTTON_TEXT_SIZE,
          fontWeight = FontWeight.Bold,
          color = White,
          shadow = Shadow(
            Color.Black.copy(alpha = SHOWCASE_MESSAGE_SHADOW_COLOR_ALPHA),
            defaultBlurOffsetForMessageAndNextButton(),
            blurRadius = SHOWCASE_MESSAGE_SHADOW_BLUR_RADIUS
          )
        )
      )
    }
  }
}

/**
 * Represents a single item in the showcase view sequence.
 *
 * @param index The order in which this target should be shown in the showcase flow.
 * @param coordinates Layout coordinates used to determine position and size of the target view on screen.
 * @param showCaseMessage Message to be displayed near the highlighted target.
 * @param showCaseMessageColor Optional color for the message text (default is white).
 * @param blurOpacity Controls the opacity of the background overlay behind the highlight (default is 0.8).
 * @param customSizeForShowcaseViewCircle Optional custom size for the radius of the highlight circle.
 *        If null, it uses the size of the target's bounds.
 */
data class ShowcaseProperty(
  val index: Int,
  val coordinates: LayoutCoordinates,
  val showCaseMessage: String,
  val showCaseMessageColor: Color = Color.White,
  val blurOpacity: Float = SHOWCASE_VIEW_BACKGROUND_COLOR_ALPHA,
  val customSizeForShowcaseViewCircle: Dp? = null,
)
