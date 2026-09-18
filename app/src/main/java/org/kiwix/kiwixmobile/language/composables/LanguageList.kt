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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
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
  onMoveUp: (LanguageItem) -> Unit = {},
  onMoveDown: (LanguageItem) -> Unit = {}
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
    itemsIndexed(
      items = viewItem,
      key = { _, item ->
        when (item) {
          is HeaderItem -> "header_${item.id}"
          is LanguageItem -> "language_${item.language.id}"
        }
      }
    ) { index, item ->
      when (item) {
        is HeaderItem -> HeaderText(
          item = item,
          modifier = Modifier
            .animateItem()
        )

        is LanguageItem -> {
          val isFirst = index == 0 || viewItem.getOrNull(index - 1) is HeaderItem
          val isLast = index == viewItem.lastIndex || viewItem.getOrNull(index + 1) is HeaderItem
          LanguageItemRow(
            context = context,
            modifier = Modifier.animateItem(),
            item = item,
            isFirst = isFirst,
            isLast = isLast,
            onItemClick = { selectLanguageItem(it) },
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown
          )
        }
      }
    }
  }
}
