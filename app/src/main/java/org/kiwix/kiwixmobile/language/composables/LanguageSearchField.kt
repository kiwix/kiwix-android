/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.search.SEARCH_FIELD_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.ComposeDimens

@Composable
fun LanguageSearchField(
  searchText: String,
  onSearchTextChange: (String) -> Unit,
  onClearClick: () -> Unit,
  modifier: Modifier = Modifier,
  onCloseClick: () -> Unit = {}
) {
  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        start = ComposeDimens.SIXTEEN_DP,
        end = ComposeDimens.SIXTEEN_DP,
        bottom = ComposeDimens.EIGHT_DP
      )
      .height(40.dp),
    shape = RoundedCornerShape(ComposeDimens.TWENTY_DP),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = ComposeDimens.TWELVE_DP),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = stringResource(R.string.search_label),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(ComposeDimens.EIGHT_DP))
      SearchInputBox(
        searchText = searchText,
        onSearchTextChange = onSearchTextChange,
        focusRequester = focusRequester,
        onSearch = { focusManager.clearFocus() },
        modifier = Modifier.weight(1f)
      )
      ClearOrCloseButton(
        searchText = searchText,
        onSearchTextChange = onSearchTextChange,
        onClearClick = onClearClick,
        onCloseClick = onCloseClick
      )
    }
  }
}

@Composable
private fun SearchInputBox(
  searchText: String,
  onSearchTextChange: (String) -> Unit,
  focusRequester: FocusRequester,
  onSearch: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.CenterStart
  ) {
    if (searchText.isEmpty()) {
      Text(
        text = stringResource(R.string.search_label),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ComposeDimens.DEFAULT_TEXT_ALPHA)
      )
    }
    BasicTextField(
      value = searchText,
      onValueChange = onSearchTextChange,
      modifier = Modifier
        .fillMaxWidth()
        .focusRequester(focusRequester)
        .testTag(SEARCH_FIELD_TESTING_TAG),
      singleLine = true,
      textStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface
      ),
      cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = { onSearch() })
    )
  }
}

@Composable
private fun ClearOrCloseButton(
  searchText: String,
  onSearchTextChange: (String) -> Unit,
  onClearClick: () -> Unit,
  onCloseClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  IconButton(
    onClick = {
      if (searchText.isNotEmpty()) {
        onSearchTextChange("")
        onClearClick()
      } else {
        onCloseClick()
      }
    },
    modifier = modifier.size(28.dp)
  ) {
    Icon(
      painter = painterResource(R.drawable.ic_clear_white_24dp),
      contentDescription = stringResource(R.string.searchview_description_clear),
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(16.dp)
    )
  }
}
