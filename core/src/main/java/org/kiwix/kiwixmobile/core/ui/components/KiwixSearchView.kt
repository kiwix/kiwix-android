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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import kotlinx.coroutines.flow.distinctUntilChanged
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.ComposeDimens

@Suppress("LongMethod")
@Composable
fun KiwixSearchView(
  modifier: Modifier = Modifier,
  value: String,
  placeholder: String = stringResource(R.string.search_label),
  searchViewTextFiledTestTag: String = "",
  clearButtonTestTag: String = "",
  onValueChange: (String) -> Unit,
  onClearClick: () -> Unit,
  onKeyboardSubmitButtonClick: (String) -> Unit = {}
) {
  val hintColor = if (isSystemInDarkTheme()) {
    Color.LightGray
  } else {
    Color.DarkGray
  }
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(focusRequester) {
    focusRequester.requestFocus()
  }

  val textFieldValue = rememberTextFieldState()

  // For OnSearchKiwix and Voice Search Set Cursor at the end
  LaunchedEffect(value) {
    val current = textFieldValue.text.toString()

    if (current != value) {
      textFieldValue.edit {
        replace(0, length, value)
      }
    }
  }

  LaunchedEffect(Unit) {
    snapshotFlow { textFieldValue.text.toString() }
      .distinctUntilChanged()
      .collect { newText ->
        if (newText != value) {
          onValueChange(newText)
        }
      }
  }

  BasicTextField(
    state = textFieldValue,
    modifier = modifier
      .testTag(searchViewTextFiledTestTag)
      .focusRequester(focusRequester)
      .semantics { contentDescription = placeholder },
    lineLimits = TextFieldLineLimits.SingleLine,
    decorator = { innerTextField ->

      Row(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier.weight(1f),
          contentAlignment = Alignment.CenterStart
        ) {
          innerTextField()

          if (textFieldValue.text.isEmpty()) {
            Text(
              text = placeholder,
              color = hintColor,
              fontSize = ComposeDimens.EIGHTEEN_SP,
              maxLines = ONE,
              overflow = Ellipsis
            )
          }
        }

        if (textFieldValue.text.isNotEmpty()) {
          IconButton(
            onClick = {
              textFieldValue.edit {
                replace(0, length, "")
              }
              onClearClick()
            },
            modifier = Modifier.testTag(clearButtonTestTag)
          ) {
            Icon(
              painter = painterResource(R.drawable.ic_clear_white_24dp),
              tint = MaterialTheme.colorScheme.onBackground,
              contentDescription = stringResource(R.string.searchview_description_clear)
            )
          }
        }
      }
    },
    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
    textStyle = TextStyle.Default.copy(
      fontSize = ComposeDimens.EIGHTEEN_SP,
      color = MaterialTheme.colorScheme.onBackground
    ),
    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
    onKeyboardAction = {
      keyboardController?.hide()
      onKeyboardSubmitButtonClick(textFieldValue.text.toString())
    }
  )
}
