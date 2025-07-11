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

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.ComposeDimens

@Composable
fun HeaderText(
  modifier: Modifier,
  item: LanguageListItem.HeaderItem
) {
  val context = LocalContext.current
  Text(
    text = when (item.id) {
      LanguageListItem.HeaderItem.SELECTED -> stringResource(
        R.string.your_language,
        context.getString(R.string.empty_string)
      )

      LanguageListItem.HeaderItem.OTHER -> stringResource(R.string.other_languages)
      else -> ""
    },
    modifier = modifier
      .padding(horizontal = ComposeDimens.SIXTEEN_DP, vertical = ComposeDimens.EIGHT_DP),
    fontSize = ComposeDimens.FOURTEEN_SP,
    style = MaterialTheme.typography.headlineMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant
  )
}
