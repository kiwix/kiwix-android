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

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.Black
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.ui.theme.MineShaftGray350
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP

const val TOOLBAR_TITLE_TESTING_TAG = "toolbarTitle"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KiwixAppBar(
  @StringRes titleId: Int,
  navigationIcon: @Composable () -> Unit,
  actionMenuItems: List<ActionMenuItem> = emptyList(),
  topAppBarScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
  // Optional search bar, used in fragments that require it
  searchBar: (@Composable () -> Unit)? = null
) {
  KiwixTheme {
    TopAppBar(
      title = { AppBarTitleSection(titleId, searchBar) },
      navigationIcon = navigationIcon,
      actions = { ActionMenu(actionMenuItems) },
      scrollBehavior = topAppBarScrollBehavior,
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = Black,
        scrolledContainerColor = Black
      ),
      // Edge-to-Edge mode is already enabled in our application,
      // so we don't need to apply additional top insets.
      // This prevents unwanted extra margin at the top.
      windowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Horizontal)
    )
  }
}

@Suppress("ComposableLambdaParameterNaming")
@Composable
private fun AppBarTitleSection(
  @StringRes titleId: Int,
  searchBar: (@Composable () -> Unit)? = null
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(start = SIXTEEN_DP),
    contentAlignment = Alignment.CenterStart
  ) {
    searchBar?.let {
      it()
    } ?: run {
      AppBarTitle(titleId)
    }
  }
}

@Composable
private fun AppBarTitle(
  @StringRes titleId: Int
) {
  val appBarTitleColor = if (isSystemInDarkTheme()) {
    MineShaftGray350
  } else {
    White
  }
  Text(
    text = stringResource(titleId),
    color = appBarTitleColor,
    style = MaterialTheme.typography.titleMedium,
    overflow = TextOverflow.Ellipsis,
    maxLines = 1,
    modifier = Modifier
      .testTag(TOOLBAR_TITLE_TESTING_TAG),
  )
}

@Composable
private fun ActionMenu(actionMenuItems: List<ActionMenuItem>) {
  Row {
    actionMenuItems.forEach { menuItem ->
      IconButton(
        enabled = menuItem.isEnabled,
        onClick = menuItem.onClick,
        modifier = menuItem.modifier.testTag(menuItem.testingTag)
      ) {
        Icon(
          painter = menuItem.icon.toPainter(),
          contentDescription = stringResource(menuItem.contentDescription),
          tint = if (menuItem.isEnabled) menuItem.iconTint else Color.Gray
        )
      }
    }
  }
}

@Composable
fun rememberBottomNavigationVisibility(lazyListState: LazyListState?): Boolean {
  var isToolbarVisible by remember { mutableStateOf(true) }
  var lastScrollIndex by remember { mutableIntStateOf(ZERO) }
  val updatedLazyListState = rememberUpdatedState(lazyListState)

  LaunchedEffect(updatedLazyListState) {
    updatedLazyListState.value?.let { state ->
      snapshotFlow { state.firstVisibleItemIndex }
        .collect { newScrollIndex ->
          if (newScrollIndex != lastScrollIndex) {
            isToolbarVisible = newScrollIndex < lastScrollIndex
            lastScrollIndex = newScrollIndex
          }
        }
    }
  }
  return isToolbarVisible
}
