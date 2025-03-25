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

package org.kiwix.kiwixmobile.core.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kiwix.kiwixmobile.core.R

// Define constants for spacing, font sizes, etc.

private val HelpItemTitleFontSize = 22.sp
private val HelpItemDescriptionFontSize = 17.sp
private val IconSize = 36.dp
private const val HELP_ITEM_ANIMATION_DURATION = 300
private const val HELP_ITEM_ARROW_ROTATION_OPEN = 180f
private const val HELP_ITEM_ARROW_ROTATION_CLOSE = 0f

@Composable
fun HelpScreenItem(
  modifier: Modifier = Modifier,
  data: HelpScreenItemDataClass,
  initiallyOpened: Boolean = false
) {
  var isOpen by remember { mutableStateOf(initiallyOpened) }
  val itemColor = if (isSystemInDarkTheme()) Color.White else Color.Black

  val topPadding: Dp = dimensionResource(id = R.dimen.dimen_medium_padding)
  val horizontalPadding: Dp = dimensionResource(id = R.dimen.activity_horizontal_margin)

  Column(
    modifier = modifier
      .fillMaxWidth(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    HelpItemHeader(data.title, isOpen, itemColor, horizontalPadding) { isOpen = !isOpen }
    AnimatedVisibility(visible = isOpen) {
      Spacer(modifier = Modifier.height(topPadding))
      HelpItemDescription(data.description, itemColor, horizontalPadding)
    }
  }
}

@Composable
fun HelpItemHeader(
  title: String,
  isOpen: Boolean,
  itemColor: Color,
  horizontalPadding: Dp,
  onToggle: () -> Unit
) {
  val arrowRotation by animateFloatAsState(
    targetValue = if (isOpen) HELP_ITEM_ARROW_ROTATION_OPEN else HELP_ITEM_ARROW_ROTATION_CLOSE,
    animationSpec = tween(HELP_ITEM_ANIMATION_DURATION),
    label = "arrowRotation"
  )
  val interactionSource = remember(::MutableInteractionSource)

  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
      .padding(horizontal = horizontalPadding, vertical = horizontalPadding)
  ) {
    Text(
      text = title,
      fontSize = HelpItemTitleFontSize,
      color = itemColor,
      fontWeight = FontWeight.Normal
    )
    Icon(
      imageVector = Icons.Default.KeyboardArrowDown,
      contentDescription = stringResource(R.string.expand),
      modifier = Modifier
        .graphicsLayer {
          rotationZ = arrowRotation
        }
        .size(IconSize),
      tint = itemColor
    )
  }
}

@Composable
fun HelpItemDescription(description: String, itemColor: Color, horizontalPadding: Dp) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = horizontalPadding, end = horizontalPadding)
  ) {
    Text(
      text = description,
      fontSize = HelpItemDescriptionFontSize,
      textAlign = TextAlign.Left,
      color = itemColor,
      modifier = Modifier.padding(bottom = horizontalPadding),
      fontWeight = FontWeight.Normal
    )
  }
}
