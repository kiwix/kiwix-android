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

package org.kiwix.kiwixmobile.intro.composable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import org.kiwix.kiwixmobile.core.ui.theme.DenimBlue200
import org.kiwix.kiwixmobile.core.ui.theme.DenimBlue800
import org.kiwix.kiwixmobile.core.utils.ComposeDimens

private const val ANIMATION_DURATION: Int = 100

@Composable
fun PageIndicatorView(
  isSelected: Boolean,
  selectedColor: Color,
  defaultColor: Color,
  defaultRadius: Dp,
  selectedLength: Dp,
  animationDurationInMillis: Int,
  modifier: Modifier = Modifier,
) {
  val color = animateColorAsState(
    targetValue = if (isSelected) {
      selectedColor
    } else {
      defaultColor
    },
    animationSpec = tween(
      durationMillis = animationDurationInMillis,
    )
  )
  val width = animateDpAsState(
    targetValue = if (isSelected) {
      selectedLength
    } else {
      defaultRadius
    },
    animationSpec = tween(
      durationMillis = animationDurationInMillis,
    )
  )

  Canvas(
    modifier = modifier
      .size(
        width = width.value,
        height = defaultRadius,
      ),
  ) {
    drawRoundRect(
      color = color.value,
      topLeft = Offset.Zero,
      size = Size(
        width = width.value.toPx(),
        height = defaultRadius.toPx(),
      ),
      cornerRadius = CornerRadius(
        x = defaultRadius.toPx(),
        y = defaultRadius.toPx(),
      ),
    )
  }
}

@Composable
fun PageIndicator(
  pagerState: PagerState
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(ComposeDimens.TEN_DP),
    modifier = Modifier.padding(bottom = ComposeDimens.SIXTEEN_DP),
  ) {
    for (i in 0 until pagerState.pageCount) {
      val isSelected = i == pagerState.currentPage
      PageIndicatorView(
        isSelected = isSelected,
        selectedColor = DenimBlue800,
        defaultColor = DenimBlue200,
        defaultRadius = ComposeDimens.TEN_DP,
        selectedLength = ComposeDimens.TWENTY_TWO_DP,
        animationDurationInMillis = ANIMATION_DURATION
      )
    }
  }
}
