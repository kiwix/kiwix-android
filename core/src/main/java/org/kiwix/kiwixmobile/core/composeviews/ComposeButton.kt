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

package org.kiwix.kiwixmobile.core.composeviews

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import org.kiwix.kiwixmobile.core.R

/**
 * This is our custom composable button according to our theme.
 */
@Composable
fun ComposeButton(
  @StringRes buttonTextId: Int,
  clickListener: () -> Unit
) {
  Button(
    onClick = { clickListener.invoke() },
    colors = ButtonDefaults.buttonColors(
      containerColor = colorResource(id = R.color.denim_blue800),
      contentColor = Color.White
    ),
    modifier = Modifier.padding(4.dp),
    shape = RoundedCornerShape(5.dp),
    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
  ) {
    Text(
      text = stringResource(id = buttonTextId).uppercase(),
      letterSpacing = 0.0333.em
    )
  }
}
