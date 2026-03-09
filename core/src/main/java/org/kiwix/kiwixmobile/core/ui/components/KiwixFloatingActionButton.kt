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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalView

/**
 * A reusable Floating Action Button (FAB) composable used across multiple screens
 * (e.g., ReaderScreen, OnlineLibrary).
 *
 * @param icon The [Painter] to be displayed as the FAB icon.
 * @param onClick Callback triggered when the FAB is clicked.
 * @param contentDescription Accessibility description for the FAB icon.
 * @param modifier Optional [Modifier] for customizing the FAB's layout and appearance.
 *        Defaults to [Modifier] (no modifications).
 * @param tint The color used to tint the icon. Defaults to
 *        [MaterialTheme.colorScheme.onSurface].
 */
@Composable
fun KiwixFloatingActionButton(
  icon: Painter,
  onClick: () -> Unit,
  contentDescription: String,
  modifier: Modifier = Modifier,
  tint: Color = MaterialTheme.colorScheme.onSurface,
) {
  val view = LocalView.current
  val interactionSource = remember { MutableInteractionSource() }
  LaunchedEffect(interactionSource) {
    interactionSource.interactions.collect { interaction ->
      if (interaction is PressInteraction.Press) {
        view.performHapticFeedback(
          android.view.HapticFeedbackConstants.CONTEXT_CLICK
        )
      }
    }
  }
  FloatingActionButton(
    onClick = {
      view.performHapticFeedback(
        android.view.HapticFeedbackConstants.CONTEXT_CLICK
      )
      onClick()
    },
    modifier = modifier,
    interactionSource = interactionSource,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = CircleShape
  ) {
    Icon(
      painter = icon,
      contentDescription = contentDescription,
      tint = tint
    )
  }
}
