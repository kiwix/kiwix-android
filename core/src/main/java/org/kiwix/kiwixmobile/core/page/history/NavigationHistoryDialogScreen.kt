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

package org.kiwix.kiwixmobile.core.page.history

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextOverflow
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.page.NO_ITEMS_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.history.adapter.NavigationHistoryListItem
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.models.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.KiwixDialogTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.PAGE_LIST_ITEM_FAVICON_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP

@Suppress("ComposableLambdaParameterNaming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationHistoryDialogScreen(
  @StringRes titleId: Int,
  navigationHistoryList: MutableList<NavigationHistoryListItem>,
  actionMenuItems: List<ActionMenuItem>,
  onNavigationItemClick: ((NavigationHistoryListItem) -> Unit),
  navigationIcon: @Composable () -> Unit
) {
  KiwixDialogTheme {
    Scaffold(
      topBar = {
        KiwixAppBar(
          title = stringResource(titleId),
          navigationIcon = navigationIcon,
          actionMenuItems = actionMenuItems
        )
      }
    ) { paddingValues ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Transparent)
          .imePadding()
          .padding(paddingValues),
      ) {
        if (navigationHistoryList.isEmpty()) {
          Text(
            text = stringResource(R.string.no_history),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
              .align(Alignment.Center)
              .semantics { testTag = NO_ITEMS_TEXT_TESTING_TAG }
          )
        } else {
          NavigationHistoryList(navigationHistoryList, onNavigationItemClick)
        }
      }
    }
  }
}

@Composable
fun NavigationHistoryList(
  navigationHistoryList: MutableList<NavigationHistoryListItem>,
  onNavigationItemClick: (NavigationHistoryListItem) -> Unit
) {
  LazyColumn {
    itemsIndexed(navigationHistoryList) { index, item ->
      NavigationHistoryItem(index, item, onNavigationItemClick)
    }
  }
}

@Composable
private fun NavigationHistoryItem(
  index: Int,
  item: NavigationHistoryListItem,
  onNavigationItemClick: (NavigationHistoryListItem) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(
        onClick = { onNavigationItemClick(item) },
      )
      .padding(
        horizontal = SIXTEEN_DP,
        vertical = EIGHT_DP
      ),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Image(
      painter = IconItem.MipmapImage(R.mipmap.ic_launcher_round).toPainter(),
      contentDescription = stringResource(R.string.fav_icon) + index,
      modifier = Modifier
        .size(PAGE_LIST_ITEM_FAVICON_SIZE)
    )

    Spacer(modifier = Modifier.width(SIXTEEN_DP))

    Text(
      text = item.title,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier
        .weight(1f)
        .semantics { contentDescription = "${item.title}$index" },
      maxLines = ONE,
      overflow = TextOverflow.Ellipsis
    )
  }
}
