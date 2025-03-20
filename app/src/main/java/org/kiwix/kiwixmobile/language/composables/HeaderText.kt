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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kiwix.kiwixmobile.core.R

@Composable
fun HeaderText(
  modifier: Modifier,
  item: LanguageListItem.HeaderItem
) {
  Text(
    text = when (item.id) {
      LanguageListItem.HeaderItem.SELECTED -> stringResource(R.string.your_languages)
      LanguageListItem.HeaderItem.OTHER -> stringResource(R.string.other_languages)
      else -> ""
    },
    modifier = modifier
      .padding(horizontal = 16.dp, vertical = 8.dp),
    fontSize = 16.sp,
    style = MaterialTheme.typography.headlineMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant
  )
}
