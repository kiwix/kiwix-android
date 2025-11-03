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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.utils.ComposeDimens
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MINIMUM_HEIGHT_OF_SEARCH_ITEM
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SEARCH_ITEM_TEXT_SIZE
import kotlin.math.roundToInt

@Composable
fun KiwixSearchView(
  modifier: Modifier = Modifier,
  value: String,
  placeholder: String = stringResource(R.string.search_label),
  searchViewTextFiledTestTag: String = "",
  clearButtonTestTag: String = "",
  suggestionText: String? = null,
  onSuggestedWordClick: (String) -> Unit = {},
  onValueChange: (String) -> Unit,
  onClearClick: () -> Unit,
  onKeyboardSubmitButtonClick: (String) -> Unit = {}
) {
  val textFieldBounds = remember { mutableStateOf(Rect.Zero) }
  Column {
    SearchViewTextFiled(
      modifier,
      searchViewTextFiledTestTag,
      value,
      placeholder,
      onValueChange,
      onClearClick,
      clearButtonTestTag,
      onKeyboardSubmitButtonClick,
      textFieldBounds
    )
    ShowCorrectWordSuggestion(suggestionText, onSuggestedWordClick, textFieldBounds)
  }
}

@Composable
private fun ShowCorrectWordSuggestion(
  suggestionText: String?,
  onSuggestedWordClick: (String) -> Unit,
  textFieldBounds: MutableState<Rect>
) {
  if (suggestionText.isNullOrBlank()) return
  Popup(
    alignment = Alignment.TopStart,
    offset = IntOffset(
      x = textFieldBounds.value.left.roundToInt(),
      y = textFieldBounds.value.bottom.roundToInt()
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = MINIMUM_HEIGHT_OF_SEARCH_ITEM)
        .background(MaterialTheme.colorScheme.background)
        .clickable { onSuggestedWordClick(suggestionText) },
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = getSuggestedHighlightedText(suggestionText),
        modifier = Modifier
          .padding(horizontal = EIGHT_DP)
          .weight(1f),
        fontSize = SEARCH_ITEM_TEXT_SIZE
      )
      Icon(
        painter = painterResource(id = R.drawable.ic_open_in_new_24dp),
        contentDescription = stringResource(id = R.string.suggested_search_icon_description),
        modifier = Modifier.padding(horizontal = EIGHT_DP)
      )
    }
  }
}

@Composable
private fun getSuggestedHighlightedText(suggestionText: String): AnnotatedString {
  val rawString = stringResource(R.string.suggest_search_text)
  val parts = rawString.split("%s")
  val before = parts.getOrNull(ZERO).orEmpty()
  val after = parts.getOrNull(ONE).orEmpty()
  return buildAnnotatedString {
    append(before)
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
      append(suggestionText)
    }
    append(after)
  }
}

@Suppress("LongParameterList", "LongMethod")
@Composable
private fun SearchViewTextFiled(
  modifier: Modifier,
  searchViewTextFiledTestTag: String,
  value: String,
  placeholder: String,
  onValueChange: (String) -> Unit,
  onClearClick: () -> Unit,
  clearButtonTestTag: String,
  onKeyboardSubmitButtonClick: (String) -> Unit,
  textFieldBounds: MutableState<Rect>
) {
  val hintColor = if (isSystemInDarkTheme()) {
    Color.LightGray
  } else {
    Color.DarkGray
  }
  val keyboardController = LocalSoftwareKeyboardController.current
  val colors = TextFieldDefaults.colors(
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedTextColor = MaterialTheme.colorScheme.onBackground
  )
  val focusRequester = FocusRequester()
  SideEffect(focusRequester::requestFocus)
  TextField(
    modifier = modifier
      .testTag(searchViewTextFiledTestTag)
      .minimumInteractiveComponentSize()
      .focusRequester(focusRequester)
      .semantics { contentDescription = placeholder }
      .onGloballyPositioned { coordinates ->
        val position = coordinates.positionInRoot()
        val size = coordinates.size.toSize()
        textFieldBounds.value = Rect(
          position.x,
          position.y,
          position.x + size.width,
          position.y + size.height
        )
      },
    singleLine = true,
    value = value,
    placeholder = {
      Text(
        text = placeholder,
        color = hintColor,
        fontSize = ComposeDimens.EIGHTEEN_SP,
        maxLines = ONE,
        overflow = Ellipsis
      )
    },
    colors = colors,
    textStyle = TextStyle.Default.copy(fontSize = ComposeDimens.EIGHTEEN_SP),
    onValueChange = { onValueChange(it.replace("\n", "")) },
    trailingIcon = {
      if (value.isNotEmpty()) {
        IconButton(onClick = onClearClick, modifier = Modifier.testTag(clearButtonTestTag)) {
          Icon(
            painter = painterResource(R.drawable.ic_clear_white_24dp),
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = stringResource(R.string.searchview_description_clear)
          )
        }
      }
    },
    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
    keyboardActions = KeyboardActions(
      onDone = {
        keyboardController?.hide()
        onKeyboardSubmitButtonClick.invoke(value)
      }
    )
  )
}
