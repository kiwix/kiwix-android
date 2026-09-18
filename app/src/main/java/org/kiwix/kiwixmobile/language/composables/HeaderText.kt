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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.ComposeDimens

const val LANGUAGE_HEADER_TESTING_TAG = "languageHeaderTestingTag"

@Composable
fun HeaderText(
  modifier: Modifier = Modifier,
  item: LanguageListItem.HeaderItem
) {
  val title = stringResource(
    if (item.id == LanguageListItem.HeaderItem.SELECTED) {
      R.string.your_languages
    } else {
      R.string.other_languages
    }
  ).trimEnd(':', ' ', '：')

  Text(
    text = title,
    modifier = modifier
      .padding(
        start = ComposeDimens.SIXTEEN_DP,
        end = ComposeDimens.SIXTEEN_DP,
        top = ComposeDimens.SIXTEEN_DP,
        bottom = ComposeDimens.EIGHT_DP
      )
      .semantics { testTag = "$LANGUAGE_HEADER_TESTING_TAG${item.id}" },
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant
  )
}
