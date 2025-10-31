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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.KIWIX_TOOLBAR_SHADOW_ELEVATION
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP

const val TOOLBAR_TITLE_TESTING_TAG = "toolbarTitle"
const val OVERFLOW_MENU_BUTTON_TESTING_TAG = "overflowMenuButtonTestingTag"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KiwixAppBar(
  modifier: Modifier = Modifier,
  title: String,
  navigationIcon: @Composable () -> Unit,
  actionMenuItems: List<ActionMenuItem> = emptyList(),
  topAppBarScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
  // Optional search bar, used in fragments that require it
  searchBar: (@Composable () -> Unit)? = null
) {
  KiwixTheme {
    TopAppBar(
      title = { AppBarTitleSection(title, searchBar) },
      navigationIcon = navigationIcon,
      actions = { ActionMenu(actionMenuItems) },
      scrollBehavior = topAppBarScrollBehavior,
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.onPrimary,
        scrolledContainerColor = MaterialTheme.colorScheme.onPrimary
      ),
      // Edge-to-Edge mode is already enabled in our application,
      // so we don't need to apply additional top insets.
      // This prevents unwanted extra margin at the top.
      windowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Horizontal),
      modifier = modifier.shadow(KIWIX_TOOLBAR_SHADOW_ELEVATION)
    )
  }
}

@Suppress("ComposableLambdaParameterNaming")
@Composable
private fun AppBarTitleSection(
  title: String,
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
      AppBarTitle(title)
    }
  }
}

@Composable
private fun AppBarTitle(
  title: String
) {
  Text(
    text = title,
    color = MaterialTheme.colorScheme.onBackground,
    style = MaterialTheme.typography.titleMedium,
    overflow = TextOverflow.Ellipsis,
    maxLines = 1,
    modifier = Modifier
      .testTag(TOOLBAR_TITLE_TESTING_TAG)
      .semantics { hideFromAccessibility() },
  )
}

@Composable
private fun ActionMenu(actionMenuItems: List<ActionMenuItem>) {
  var overflowExpanded by remember { mutableStateOf(false) }

  Row {
    val (mainActions, overflowActions) = actionMenuItems.partition { !it.isInOverflow }
    MainMenuItems(mainActions)
    if (overflowActions.isNotEmpty()) {
      IconButton(
        onClick = { overflowExpanded = true },
        modifier = Modifier.testTag(OVERFLOW_MENU_BUTTON_TESTING_TAG)
      ) {
        Icon(
          imageVector = Icons.Default.MoreVert,
          contentDescription = stringResource(R.string.more_options),
          tint = MaterialTheme.colorScheme.onBackground
        )
      }
    }
    OverflowMenuItems(overflowExpanded, overflowActions) { overflowExpanded = false }
  }
}

@Composable
private fun MainMenuItems(mainActions: List<ActionMenuItem>) {
  mainActions.forEach { menuItem ->
    val modifier = menuItem.modifier.testTag(menuItem.testingTag)
    menuItem.customView?.let { customComposable ->
      Box(modifier = modifier.clickable(enabled = menuItem.isEnabled) { menuItem.onClick() }) {
        customComposable()
      }
    } ?: run {
      menuItem.icon?.let { iconItem ->
        IconButton(
          enabled = menuItem.isEnabled,
          onClick = menuItem.onClick,
          modifier = modifier
        ) {
          Icon(
            painter = iconItem.toPainter(),
            contentDescription = stringResource(menuItem.contentDescription),
            tint = if (menuItem.isEnabled) MaterialTheme.colorScheme.onBackground else Color.Gray
          )
        }
      } ?: run {
        TextButton(
          enabled = menuItem.isEnabled,
          onClick = menuItem.onClick,
          modifier = modifier
        ) {
          Text(
            text = menuItem.iconButtonText.uppercase(),
            color = getMenuItemTextColor(menuItem),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
private fun getMenuItemTextColor(menuItem: ActionMenuItem) =
  if (menuItem.isEnabled) {
    MaterialTheme.colorScheme.onBackground
  } else {
    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
  }

@Composable
private fun OverflowMenuItems(
  overflowExpanded: Boolean,
  overflowActions: List<ActionMenuItem>,
  onDismiss: () -> Unit
) {
  DropdownMenu(
    expanded = overflowExpanded,
    onDismissRequest = onDismiss
  ) {
    overflowActions.forEachIndexed { index, menuItem ->
      DropdownMenuItem(
        text = {
          Column {
            Text(
              text = menuItem.iconButtonText.ifEmpty {
                stringResource(id = menuItem.contentDescription)
              }
            )
          }
        },
        onClick = {
          onDismiss()
          menuItem.onClick()
        },
        enabled = menuItem.isEnabled,
        modifier = Modifier.testTag(menuItem.testingTag)
      )
    }
  }
}
