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

package org.kiwix.kiwixmobile.core.page

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isCustomApp
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.page.adapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.DateItem
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixSearchView
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.theme.AlabasterWhite
import org.kiwix.kiwixmobile.core.ui.theme.Black
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOURTEEN_SP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.PAGE_SWITCH_LEFT_RIGHT_MARGIN
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.PAGE_SWITCH_ROW_BOTTOM_MARGIN
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP

@Suppress("ComposableLambdaParameterNaming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageScreen(
  state: PageFragmentScreenState,
  itemClickListener: OnItemClickListener,
  actionMenuItems: List<ActionMenuItem>,
  navigationIcon: @Composable () -> Unit
) {
  KiwixTheme {
    Scaffold(
      topBar = {
        Column {
          KiwixAppBar(
            titleId = state.screenTitle,
            navigationIcon = navigationIcon,
            actionMenuItems = actionMenuItems,
            searchBar = searchBarIfActive(state)
          )
          PageSwitchRow(state)
        }
      }
    ) { padding ->
      val items = state.pageState.pageItems
      Box(
        modifier = Modifier
          .padding(padding)
          .fillMaxSize()
      ) {
        if (items.isEmpty()) {
          Text(
            text = state.noItemsString,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.align(Alignment.Center)
          )
        } else {
          LazyColumn {
            items(state.pageState.visiblePageItems) { item ->
              when (item) {
                is Page -> PageListItem(page = item, itemClickListener = itemClickListener)
                is DateItem -> DateItemText(item)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun searchBarIfActive(
  state: PageFragmentScreenState
): (@Composable () -> Unit)? = if (state.isSearchActive) {
  {
    KiwixSearchView(
      placeholder = state.searchQueryHint,
      value = state.searchText,
      testTag = "",
      onValueChange = { state.searchValueChangedListener(it) },
      onClearClick = { state.clearSearchButtonClickListener.invoke() }
    )
  }
} else {
  null
}

@Composable
fun PageSwitchRow(
  state: PageFragmentScreenState
) {
  val switchTextColor = if (isSystemInDarkTheme()) {
    AlabasterWhite
  } else {
    White
  }
  val context = LocalActivity.current as CoreMainActivity
  // hide switches for custom apps, see more info here https://github.com/kiwix/kiwix-android/issues/3523
  if (!context.isCustomApp()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Black)
        .padding(bottom = PAGE_SWITCH_ROW_BOTTOM_MARGIN),
      horizontalArrangement = Arrangement.Absolute.Right,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        state.switchString,
        color = switchTextColor,
        style = TextStyle(fontSize = FOURTEEN_SP)
      )
      Switch(
        checked = state.switchIsChecked,
        onCheckedChange = { state.onSwitchCheckedChanged(it) },
        enabled = state.switchIsEnabled,
        modifier = Modifier
          .padding(horizontal = PAGE_SWITCH_LEFT_RIGHT_MARGIN),
        colors = SwitchDefaults.colors(
          uncheckedTrackColor = White
        )
      )
    }
  }
}

@Composable
fun DateItemText(dateItem: DateItem) {
  Text(
    text = dateItem.dateString,
    style = MaterialTheme.typography.bodySmall,
    modifier = Modifier.padding(SIXTEEN_DP)
  )
}
