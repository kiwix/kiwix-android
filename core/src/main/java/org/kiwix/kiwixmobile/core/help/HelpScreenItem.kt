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

import android.content.Context
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.Gravity
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.util.LinkifyCompat
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.theme.MineShaftGray900
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.HELP_SCREEN_ARROW_ICON_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.HELP_SCREEN_ITEM_TITLE_LETTER_SPACING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.HELP_SCREEN_ITEM_TITLE_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP

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

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = EIGHT_DP, horizontal = SIXTEEN_DP),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    HelpItemHeader(data.title, isOpen) { isOpen = !isOpen }
    AnimatedVisibility(visible = isOpen) {
      Spacer(modifier = Modifier.height(EIGHT_DP))
      HelpItemDescription(LocalContext.current, data.description)
    }
  }
}

@Composable
fun HelpItemHeader(
  title: String,
  isOpen: Boolean,
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
      .testTag(HELP_SCREEN_ITEM_TITLE_TESTING_TAG)
  ) {
    Text(
      text = title,
      fontSize = HELP_SCREEN_ITEM_TITLE_TEXT_SIZE,
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
      letterSpacing = HELP_SCREEN_ITEM_TITLE_LETTER_SPACING,
      modifier = Modifier.minimumInteractiveComponentSize()
    )
    Image(
      imageVector = Icons.Default.KeyboardArrowDown,
      contentDescription = stringResource(R.string.expand),
      modifier = Modifier
        .graphicsLayer {
          rotationZ = arrowRotation
        }
        .defaultMinSize(
          minWidth = HELP_SCREEN_ARROW_ICON_SIZE,
          minHeight = HELP_SCREEN_ARROW_ICON_SIZE
        )
        .minimumInteractiveComponentSize(),
      colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onSurface)
    )
  }
}

@Composable
fun HelpItemDescription(context: Context, description: String) {
  val textColor = if (isSystemInDarkTheme()) {
    Color.LightGray
  } else {
    MineShaftGray900
  }
  val helpItemDescription = remember { TextView(context) }
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = SIXTEEN_DP)
  ) {
    AndroidView(
      factory = { helpItemDescription },
      modifier = Modifier.padding(bottom = SIXTEEN_DP)
        .testTag(HELP_SCREEN_ITEM_DESCRIPTION_TESTING_TAG)
        .semantics { contentDescription = description }
    ) { textView ->
      textView.apply {
        text = description
        setTextAppearance(R.style.TextAppearance_KiwixTheme_Subtitle2)
        setTextColor(textColor.toArgb())
        minHeight =
          context.resources.getDimensionPixelSize(R.dimen.material_minimum_height_and_width)
        gravity = Gravity.CENTER or Gravity.START
        LinkifyCompat.addLinks(this, Linkify.WEB_URLS)
        movementMethod = LinkMovementMethod.getInstance()
      }
    }
  }
}
