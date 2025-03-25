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

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.ComposeDimens

@Composable
fun KiwixSearchView(
  modifier: Modifier,
  value: String,
  testTag: String = "",
  onValueChange: (String) -> Unit,
  onClearClick: () -> Unit
) {
  val colors = TextFieldDefaults.colors(
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedTextColor = Color.White
  )
  val focusRequester = FocusRequester()
  SideEffect(focusRequester::requestFocus)

  TextField(
    modifier = modifier
      .testTag(testTag)
      .focusRequester(focusRequester),
    singleLine = true,
    value = value,
    placeholder = {
      Text(
        text = stringResource(R.string.search_label),
        color = Color.LightGray,
        fontSize = ComposeDimens.EIGHTEEN_SP
      )
    },
    colors = colors,
    textStyle = TextStyle.Default.copy(
      fontSize = ComposeDimens.EIGHTEEN_SP
    ),
    onValueChange = {
      onValueChange(it.replace("\n", ""))
    },
    trailingIcon = {
      if (value.isNotEmpty()) {
        IconButton(onClick = onClearClick) {
          Icon(
            painter = painterResource(R.drawable.ic_clear_white_24dp),
            tint = Color.White,
            contentDescription = null
          )
        }
      }
    }
  )
}
