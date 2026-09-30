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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.search.SEARCH_FIELD_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.KiwixSearchView
import org.kiwix.kiwixmobile.core.utils.ComposeDimens

@Composable
fun LanguageSearchField(
  searchText: String,
  onSearchTextChange: (String) -> Unit,
  onClearClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val focusManager = LocalFocusManager.current

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        start = ComposeDimens.SIXTEEN_DP,
        end = ComposeDimens.SIXTEEN_DP,
        bottom = ComposeDimens.EIGHT_DP
      )
      .height(ComposeDimens.FORTY_EIGHT_DP),
    shape = RoundedCornerShape(ComposeDimens.TWENTY_FOUR_DP),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(
          start = ComposeDimens.FOURTEEN_DP,
          end = ComposeDimens.FOUR_DP
        ),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = stringResource(R.string.search_label),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(ComposeDimens.TWENTY_DP)
      )
      Spacer(modifier = Modifier.width(ComposeDimens.TEN_DP))
      KiwixSearchView(
        modifier = Modifier.weight(1f),
        value = searchText,
        searchViewTextFiledTestTag = SEARCH_FIELD_TESTING_TAG,
        onValueChange = onSearchTextChange,
        onClearClick = onClearClick,
        onKeyboardSubmitButtonClick = { focusManager.clearFocus() },
        imeAction = ImeAction.Search,
        autoFocus = false,
        textColor = MaterialTheme.colorScheme.onSurface,
        hintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
          alpha = ComposeDimens.DEFAULT_TEXT_ALPHA
        ),
        tintColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSurface
        )
      )
    }
  }
}
