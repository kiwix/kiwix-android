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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kiwix.kiwixmobile.core.ui.theme.CornflowerBlue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun KiwixShowCaseView(
  targets: SnapshotStateMap<String, ShowcaseProperty>,
  onShowCaseCompleted: () -> Unit
) {
  val uniqueTargets = targets.values.sortedBy { it.index }
  var currentTargetIndex by remember { mutableStateOf(0) }
  val currentTarget = if (uniqueTargets.isNotEmpty() && currentTargetIndex < uniqueTargets.size) {
    uniqueTargets[currentTargetIndex]
  } else {
    null
  }

  currentTarget?.let {
    AnimatedShowCase(targets = it) {
      if (++currentTargetIndex >= uniqueTargets.size) {
        onShowCaseCompleted()
      }
    }
  }
}

@Suppress("LongMethod", "MagicNumber")
@Composable
fun AnimatedShowCase(
  targets: ShowcaseProperty,
  onShowCaseCompleted: () -> Unit
) {
  val targetRect = targets.coordinates.boundsInRoot()
  val targetRadius = targetRect.maxDimension / 2f + 20

  // Animation setup for rounded animation
  val animationSpec = infiniteRepeatable<Float>(
    animation = tween(2000, easing = FastOutLinearInEasing),
    repeatMode = RepeatMode.Reverse
  )
  val animaTable = remember { Animatable(0f) }

  LaunchedEffect(animaTable) {
    animaTable.animateTo(1f, animationSpec = animationSpec)
  }

  val outerAnimaTable = remember { Animatable(0.6f) }

  LaunchedEffect(targets) {
    outerAnimaTable.snapTo(0.6f)
    outerAnimaTable.animateTo(
      targetValue = 1f,
      animationSpec = tween(500)
    )
  }

  // Map animation to y position of the components
  val dys = animaTable.value

  // Text coordinates and outer radius
  var textCoordinate: LayoutCoordinates? by remember { mutableStateOf(null) }
  var outerRadius by remember { mutableStateOf(0f) }
  val screenHeight = LocalConfiguration.current.screenHeightDp
  val textYOffset = with(LocalDensity.current) {
    targets.coordinates.positionInRoot().y.toDp()
  }
  var outerOffset by remember { mutableStateOf(Offset(0f, 0f)) }

  textCoordinate?.let {
    val textRect = it.boundsInRoot()
    val textHeight = it.size.height
    val isInGutter = textYOffset > screenHeight.dp
    outerOffset = getOuterCircleCenter(targetRect, textRect, targetRadius, textHeight, isInGutter)
    outerRadius = getOuterRadius(textRect, targetRect) + targetRadius
  }

  Canvas(
    modifier = Modifier
      .fillMaxSize()
      .pointerInput(targets) {
        detectTapGestures { tapOffset ->
          if (targetRect.contains(tapOffset)) {
            onShowCaseCompleted()
          }
        }
      }
      .graphicsLayer(alpha = 0.99f)
  ) {
    // Animated Rounded ShowCaseView
    drawRect(
      color = CornflowerBlue.copy(alpha = 0.8f),
      size = size
    )
    // draw circle with animation
    drawCircle(
      color = Color.White,
      radius = targetRect.maxDimension * dys * 2f,
      center = targetRect.center,
      alpha = 1 - dys
    )
    drawCircle(
      color = Color.White,
      radius = targetRadius,
      center = targetRect.center,
      blendMode = BlendMode.Clear
    )
  }

  ShowText(currentTarget = targets, targetRect = targetRect, targetRadius = targetRadius) {
    textCoordinate = it
  }

  // Next Button at the bottom center
  NextButton(onShowCaseCompleted = onShowCaseCompleted)
}

@Composable
fun NextButton(onShowCaseCompleted: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom,
    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
  ) {
    androidx.compose.material3.Button(
      onClick = {
        onShowCaseCompleted()
      },
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(text = "Next", fontWeight = FontWeight.Bold)
    }
  }
}

@Suppress("MagicNumber")
@Composable
fun ShowText(
  currentTarget: ShowcaseProperty,
  targetRect: Rect,
  targetRadius: Float,
  updateCoordinates: (LayoutCoordinates) -> Unit
) {
  var txtOffsetY by remember { mutableStateOf(0f) }
  var txtOffsetX by remember { mutableStateOf(0f) }
  var txtRightOffSet by remember { mutableStateOf(0f) }
  val configuration = LocalConfiguration.current
  val screenWidth = configuration.screenWidthDp.toFloat()

  Column(
    modifier = Modifier
      .offset(
        x = with(LocalDensity.current) { txtOffsetX.toDp() },
        y = with(LocalDensity.current) { txtOffsetY.toDp() }
      )
      .onGloballyPositioned {
        updateCoordinates(it)
        val textHeight = it.size.height
        val possibleTop =
          targetRect.center.y - targetRadius - textHeight
        val possibleLeft = targetRect.topLeft.x
        txtOffsetY = if (possibleTop > 0) {
          possibleTop
        } else {
          targetRect.center.y + targetRadius - 140
        }
        txtRightOffSet = it.boundsInRoot().topRight.x
        txtOffsetX = it.boundsInRoot().topRight.x - it.size.width
        txtOffsetX = if (possibleLeft >= screenWidth / 2) {
          screenWidth / 2 + targetRadius
        } else {
          possibleLeft
        }
        txtRightOffSet += targetRadius
      }
      .padding(2.dp)
  ) {
    Text(
      text = currentTarget.showCaseMessage,
      fontSize = 14.sp,
      color = currentTarget.showCaseMessageColor
    )
  }
}

fun getOuterCircleCenter(
  targetRect: Rect,
  textRect: Rect,
  targetRadius: Float,
  textHeight: Int,
  isInGutter: Boolean
): Offset {
  val outerCenterX: Float
  var outerCenterY: Float

  val onTop = targetRect.center.y - targetRadius - textHeight > 0
  val left = min(textRect.left, targetRect.left - targetRadius)
  val right = max(textRect.right, targetRect.right + targetRadius)

  val centerY = if (onTop) {
    targetRect.center.y - targetRadius - textHeight
  } else {
    targetRect.center.y + targetRadius + textHeight
  }

  outerCenterY = centerY
  outerCenterX = (left + right) / 2

  // If the text is in the gutter, adjust the vertical position
  if (isInGutter) {
    outerCenterY = targetRect.center.y
  }

  return Offset(outerCenterX, outerCenterY)
}

fun getOuterRadius(textRect: Rect, targetRect: Rect): Float {
  // Get outer rect that covers both target and text rect
  val topLeftX = min(textRect.topLeft.x, targetRect.topLeft.x)
  val topLeftY = min(textRect.topLeft.y, targetRect.topLeft.y)
  val bottomRightX = max(textRect.bottomRight.x, targetRect.bottomRight.x)
  val bottomRightY = max(textRect.bottomRight.y, targetRect.bottomRight.y)
  val newBounds = Rect(topLeftX, topLeftY, bottomRightX, bottomRightY)

  // Calculate the diagonal distance of the new bounding box
  val distance =
    sqrt(newBounds.height.toDouble().pow(2.0) + newBounds.width.toDouble().pow(2.0)).toFloat()

  // Return the radius (half of the diagonal distance)
  return (distance / 2f)
}

data class ShowcaseProperty(
  val index: Int,
  val coordinates: LayoutCoordinates,
  val showCaseMessage: String,
  val showCaseMessageColor: Color = Color.White,
  val blurOpacity: Float = 0.8f,
  val customWidth: Dp? = null,
  val customHeight: Dp? = null,
)
