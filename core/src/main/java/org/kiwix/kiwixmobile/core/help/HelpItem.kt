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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
Added the Values Here because of Linter configured according to XML Project
and the Composable names are in Camel case for the linter rules needed to be changed to Capital
*/

private val Padding_Top = 12.dp
private val Padding_Bottom = 8.dp
private val Padding_Horizontal = 16.dp
private const val ARROW_ANIMATION_DURATION = 300
private val Ico_Size = 46.dp
private val TitleFontSize = 18.sp
private val DescriptionFontSize = 16.sp
private const val ANIMATION_END = 180f
private const val ANIMATION_START = 0f

@Composable
fun HelpItem(
  modifier: Modifier = Modifier,
  data: HelpScreenItemDataClass,
  initiallyOpened: Boolean = false
) {
  var isOpen by remember { mutableStateOf(initiallyOpened) }
  val isDarkTheme = isSystemInDarkTheme()
  val itemColor = if (isDarkTheme) Color.White else Color.Black
  val arrowRotation by
  animateFloatAsState(
    targetValue = if (isOpen) ANIMATION_END else ANIMATION_START,
    animationSpec = tween(ARROW_ANIMATION_DURATION),
    label = "arrowRotation"
  )
  val interactionSource = remember(::MutableInteractionSource)
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(top = Padding_Top),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    TopHeader(
      headerData = HeaderData(
        title = data.title,
        arrowRotation = arrowRotation,
        itemColor = itemColor
      ),
      interactionSource = interactionSource,
      onClick = {
        isOpen = !isOpen
      }
    )
    Spacer(modifier = Modifier.height(Padding_Top))
    AnimatedVisibility(visible = isOpen) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = Padding_Horizontal)
      ) {
        Text(
          text = data.description,
          fontSize = DescriptionFontSize,
          textAlign = TextAlign.Left,
          color = itemColor,
          modifier = Modifier.padding(bottom = Padding_Bottom)
        )
      }
    }
  }
}

@Composable
fun TopHeader(
  modifier: Modifier = Modifier,
  headerData: HeaderData,
  interactionSource: MutableInteractionSource,
  onClick: () -> Unit
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
      .padding(horizontal = 16.dp)
  ) {
    Text(
      text = headerData.title,
      fontSize = TitleFontSize,
      color = headerData.itemColor,
      fontWeight = FontWeight.SemiBold
    )
    Icon(
      imageVector = Icons.Default.KeyboardArrowDown,
      contentDescription = "Open or Close DropDown",
      modifier = Modifier
        .graphicsLayer { rotationZ = headerData.arrowRotation }
        .size(Ico_Size),
      tint = headerData.itemColor
    )
  }
}

data class HeaderData(
  val title: String,
  val arrowRotation: Float,
  val itemColor: Color
)
