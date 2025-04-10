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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.MutableLiveData
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isCustomApp
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.DateItem
import org.kiwix.kiwixmobile.core.page.viewmodel.PageState
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP

@Suppress("LongParameterList", "IgnoredReturnValue", "UnusedParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageScreen(
  pageState: MutableLiveData<out PageState<out Page>>,
  effects: PublishProcessor<SideEffect<*>>,
  screenTitle: Int,
  noItemsString: String,
  switchString: String,
  searchQueryHint: String,
  switchIsChecked: Boolean,
  onSwitchChanged: (Boolean) -> Unit,
  onItemClick: (Page) -> Unit,
  onItemLongClick: (Page) -> Unit
) {
  val context = LocalActivity.current as CoreMainActivity

  val state by pageState.observeAsState()

  LaunchedEffect(Unit) {
    effects.subscribe { it.invokeWith(context) }
  }

  KiwixTheme {
    Scaffold(
      topBar = {
        Column {
          KiwixAppBar(
            titleId = screenTitle,
            navigationIcon = {},
          )
          if (!context.isCustomApp()) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SIXTEEN_DP, vertical = EIGHT_DP),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(switchString, modifier = Modifier.weight(1f))
              Switch(checked = switchIsChecked, onCheckedChange = onSwitchChanged)
            }
          }
        }
      }
    ) { padding ->
      val items = state?.visiblePageItems.orEmpty()
      Box(modifier = Modifier.padding(padding)) {
        if (items.isEmpty()) {
          Text(
            text = noItemsString,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.align(Alignment.Center)
          )
        } else {
          LazyColumn {
            items(items) { item ->
              when (item) {
                is Page -> {
                  PageListItem(
                    page = item,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                  )
                }

                is DateItem -> {
                  DateItemText(item)
                }
              }
            }
          }
        }
      }
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
