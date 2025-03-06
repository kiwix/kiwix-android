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

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.kiwix.kiwixmobile.core.ui.theme.DenimBlue800
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.BUTTON_DEFAULT_ELEVATION
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.BUTTON_DEFAULT_PADDING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.BUTTON_PRESSED_ELEVATION
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DEFAULT_LETTER_SPACING

/**
 * This is our custom compose button according to our theme.
 */
@Composable
fun KiwixButton(
  @StringRes buttonTextId: Int,
  clickListener: () -> Unit
) {
  Button(
    onClick = { clickListener.invoke() },
    colors = ButtonDefaults.buttonColors(
      containerColor = DenimBlue800,
      contentColor = White
    ),
    modifier = Modifier.padding(BUTTON_DEFAULT_PADDING),
    shape = MaterialTheme.shapes.extraSmall,
    elevation = ButtonDefaults.buttonElevation(
      defaultElevation = BUTTON_DEFAULT_ELEVATION,
      pressedElevation = BUTTON_PRESSED_ELEVATION
    )
  ) {
    Text(
      text = stringResource(id = buttonTextId).uppercase(),
      letterSpacing = DEFAULT_LETTER_SPACING
    )
  }
}
