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

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.ComposeDimens
import org.kiwix.kiwixmobile.language.composables.LanguageListItem.LanguageItem

const val LANGUAGE_ITEM_CHECKBOX_TESTING_TAG = "languageItemCheckboxTestingTag"

@Composable
fun LanguageItemRow(
  context: Context,
  modifier: Modifier,
  item: LanguageItem,
  onCheckedChange: (LanguageItem) -> Unit
) {
  val language = item.language
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(ComposeDimens.SIXTY_FOUR_DP)
      .semantics {
        contentDescription = context.getString(R.string.select_language_content_description)
      }
      .clickable {
        onCheckedChange(item)
      },
    verticalAlignment = Alignment.CenterVertically
  ) {
    Checkbox(
      modifier = Modifier
        .padding(ComposeDimens.SIXTEEN_DP)
        .semantics {
          testTag = "$LANGUAGE_ITEM_CHECKBOX_TESTING_TAG${language.language}"
        },
      checked = language.active,
      onCheckedChange = {
        onCheckedChange(item)
      }
    )
    Column {
      Text(
        text = language.language,
        style = MaterialTheme.typography.bodyLarge
      )
      Text(
        text = language.languageLocalized,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSecondary
      )
    }
    Spacer(modifier = Modifier.weight(1f))
    Text(
      text = stringResource(R.string.books_count, language.occurencesOfLanguage),
      modifier = Modifier.padding(ComposeDimens.SIXTEEN_DP),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSecondary
    )
  }
}
