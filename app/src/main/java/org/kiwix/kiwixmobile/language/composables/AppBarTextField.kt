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

package org.kiwix.kiwixmobile.language.composables

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.language.SEARCH_FIELD_TESTING_TAG

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarTextField(
  value: String,
  onValueChange: (String) -> Unit
) {
  val interactionSource = remember(::MutableInteractionSource)
  val textStyle = LocalTextStyle.current

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
  var textFieldValue by remember {
    mutableStateOf(TextFieldValue(value, TextRange(value.length)))
  }
  textFieldValue = textFieldValue.copy(text = value)
  CompositionLocalProvider(
    LocalTextSelectionColors provides LocalTextSelectionColors.current
  ) {
    BasicTextField(
      modifier = Modifier
        .testTag(SEARCH_FIELD_TESTING_TAG)
        .width(200.dp)
        .padding(start = 20.dp)
        .focusRequester(focusRequester),
      value = textFieldValue,
      onValueChange = {
        textFieldValue = it
        onValueChange(it.text.replace("\n", ""))
      },
      textStyle = textStyle.copy(color = Color.White),
      cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
      interactionSource = interactionSource,
      singleLine = true,
      decorationBox = { innerTextField ->
        // places text field with placeholder and appropriate bottom padding
        TextFieldDefaults.DecorationBox(
          value = value,
          innerTextField = innerTextField,
          enabled = true,
          singleLine = true,
          visualTransformation = VisualTransformation.None,
          interactionSource = interactionSource,
          placeholder = {
            Text(
              text = stringResource(R.string.search_label),
              color = Color.LightGray
            )
          },
          colors = colors,
          contentPadding = PaddingValues(bottom = 4.dp),
        )
      }
    )
  }
}
