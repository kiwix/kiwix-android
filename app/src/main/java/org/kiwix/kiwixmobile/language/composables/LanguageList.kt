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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.language.composables.LanguageListItem.HeaderItem
import org.kiwix.kiwixmobile.language.composables.LanguageListItem.LanguageItem

@Composable
fun LanguageList(
  context: Context,
  listState: LazyListState,
  viewItem: List<LanguageListItem>,
  selectLanguageItem: (LanguageItem) -> Unit,
) {
  LazyColumn(
    state = listState
  ) {
    items(
      items = viewItem,
      key = { item ->
        when (item) {
          is HeaderItem -> "header_${item.id}"
          is LanguageItem -> "language_${item.language.id}"
        }
      }
    ) { item ->
      when (item) {
        is HeaderItem -> HeaderText(item)
        is LanguageItem -> LanguageItemRow(
          context = context,
          modifier = Modifier
            .animateItem(
              fadeInSpec = tween(durationMillis = 250),
              fadeOutSpec = tween(durationMillis = 100),
              placementSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioLowBouncy
              )
            )
            .fillMaxWidth()
            .height(64.dp)
            .semantics {
              contentDescription =
                context.getString(R.string.select_language_content_description)
            }
            .clickable {
              selectLanguageItem(item)
            },
          item = item,
          onCheckedChange = { selectLanguageItem(it) }
        )
      }
    }
  }
}
