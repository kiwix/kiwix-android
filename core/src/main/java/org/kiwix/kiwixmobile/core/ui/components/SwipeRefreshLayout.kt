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

package org.kiwix.kiwixmobile.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.ui.theme.Black
import org.kiwix.kiwixmobile.core.ui.theme.White

const val SWIPE_REFRESH_TESTING_TAG = "swipeRefreshTestingTag"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeRefreshLayout(
  isRefreshing: Boolean,
  isEnabled: Boolean,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
  state: PullToRefreshState = rememberPullToRefreshState(),
  contentAlignment: Alignment = Alignment.TopStart,
  indicator: @Composable BoxScope.() -> Unit = {
    Indicator(
      modifier = Modifier.align(Alignment.TopCenter),
      isRefreshing = isRefreshing,
      state = state,
      containerColor = White,
      color = Black
    )
  },
  content: @Composable BoxScope.() -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  Box(
    modifier
      .testTag(SWIPE_REFRESH_TESTING_TAG)
      .pullToRefresh(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = {
          coroutineScope.launch {
            state.animateToHidden()
            onRefresh.invoke()
          }
        },
        enabled = isEnabled
      ),
    contentAlignment = contentAlignment
  ) {
    content()
    indicator()
  }
}
