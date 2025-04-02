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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.ComposeDimens
import org.kiwix.kiwixmobile.language.composables.LanguageListItem.HeaderItem
import org.kiwix.kiwixmobile.language.composables.LanguageListItem.LanguageItem
import org.kiwix.kiwixmobile.language.viewmodel.State
import org.kiwix.kiwixmobile.language.viewmodel.State.Content

@Composable
fun LanguageList(
  state: State,
  context: Context,
  listState: LazyListState,
  selectLanguageItem: (LanguageItem) -> Unit,
) {
  val viewItem = (state as Content).viewItems

  LaunchedEffect(viewItem) {
    snapshotFlow(listState::firstVisibleItemIndex)
      .collect {
        if (listState.firstVisibleItemIndex == 2) {
          listState.animateScrollToItem(0)
        }
      }
  }
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
        is HeaderItem -> HeaderText(
          item = item,
          modifier = Modifier
            .animateItem()
        )

        is LanguageItem -> LanguageItemRow(
          context = context,
          modifier = Modifier
            .animateItem()
            .fillMaxWidth()
            .height(ComposeDimens.SIXTY_FOUR_DP)
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
