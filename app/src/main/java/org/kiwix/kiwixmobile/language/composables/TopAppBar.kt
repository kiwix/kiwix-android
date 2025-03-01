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

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kiwix.kiwixmobile.core.R

@Composable
fun TopBar(
  title: String,
  searchText: String,
  isSearchActive: Boolean,
  onNavigationClick: () -> Unit,
  onSearchClick: () -> Unit,
  onBackClick: () -> Unit,
  onSearchTextSubmit: () -> Unit,
  onSearchTextClear: () -> Unit,
  onSearchTextChanged: (String) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(56.dp)
      .background(color = Color.Black),
    horizontalArrangement = Arrangement.Start,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Navigation icon
    if (isSearchActive) {
      IconButton(onClick = onBackClick) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.toolbar_back_button_content_description),
          tint = Color.White
        )
      }
    } else {
      IconButton(onClick = onNavigationClick) {
        Icon(
          painter = painterResource(R.drawable.ic_clear_white_24dp),
          contentDescription = R.string.toolbar_back_button_content_description.toString(),
          tint = Color.White
        )
      }
    }

    // Title
    if (isSearchActive) {
      AppBarTextField(
        value = searchText,
        onValueChange = onSearchTextChanged,
        hint = stringResource(R.string.search_label),
        modifier = Modifier
          .width(200.dp)
          .padding(start = 20.dp),
        keyboardOptions = KeyboardOptions.Default,
        keyboardActions = KeyboardActions.Default
      )
    } else {
      Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
          .padding(horizontal = 16.dp)
      )
    }

    Spacer(modifier = Modifier.weight(1f))
    // Action items
    Row(
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically
    ) {
      val whiteTint = Color.White
      if (!isSearchActive) {
        IconButton(onClick = onSearchClick) {
          Icon(
            painter = painterResource(R.drawable.action_search),
            contentDescription = stringResource(R.string.search_label),
            tint = whiteTint
          )
        }
      } else if (searchText.isNotEmpty()) {
        IconButton(onClick = onSearchTextClear) {
          Icon(
            painter = painterResource(R.drawable.ic_clear_white_24dp),
            contentDescription = null,
            tint = whiteTint
          )
        }
      }
      IconButton(onClick = onSearchTextSubmit) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = stringResource(R.string.save_languages),
          tint = whiteTint
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarTextField(
  modifier: Modifier = Modifier,
  value: String,
  hint: String,
  onValueChange: (String) -> Unit,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
  val interactionSource = remember(::MutableInteractionSource)
  val textStyle = LocalTextStyle.current

  val colors = TextFieldDefaults.colors(
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
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
      value = textFieldValue,
      onValueChange = {
        textFieldValue = it
        onValueChange(it.text.replace("\n", ""))
      },
      textStyle = textStyle.copy(color = Color.White),
      cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
      modifier = modifier
        .fillMaxWidth()
        .indicatorLine(
          enabled = true,
          isError = false,
          interactionSource = interactionSource,
          colors = colors
        )
        .focusRequester(focusRequester),
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
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
          isError = false,
          placeholder = {
            Text(
              text = hint,
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
