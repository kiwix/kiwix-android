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

package org.kiwix.kiwixmobile.core.main

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextOverflow
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.models.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.MineShaftGray350
import org.kiwix.kiwixmobile.core.ui.theme.MineShaftGray600
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FORTY_EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.NAVIGATION_DRAWER_WIDTH
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWELVE_DP

@Composable
fun LeftDrawerMenu(
  drawerMenuGroupList: List<DrawerMenuGroup>,
  appName: String? = null,
  appIcon: Painter? = null
) {
  val resolvedAppName = appName
    ?: (LocalActivity.current as? CoreMainActivity)?.appName.orEmpty().ifEmpty { "Kiwix" }
  val resolvedAppIcon = appIcon
    ?: IconItem.MipmapImage(R.mipmap.ic_launcher).toPainter()

  Surface(
    modifier = Modifier
      .width(NAVIGATION_DRAWER_WIDTH)
      .fillMaxHeight()
      .statusBarsPadding(),
    shadowElevation = EIGHT_DP
  ) {
    val dividerColor = if (isSystemInDarkTheme()) {
      MineShaftGray600
    } else {
      MineShaftGray350
    }
    // Make it scrollable when there is no enough space to show all item, e.g., in landscape mode.
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = SIXTEEN_DP, vertical = TWELVE_DP),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Image(
          painter = resolvedAppIcon,
          contentDescription = null,
          modifier = Modifier.size(FORTY_EIGHT_DP)
        )
        Spacer(modifier = Modifier.width(TWELVE_DP))
        Text(
          text = resolvedAppName,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
      HorizontalDivider(color = dividerColor)
      drawerMenuGroupList.forEach {
        DrawerGroup(it.drawerMenuItemList, dividerColor)
      }
    }
  }
}

@Composable
private fun DrawerGroup(
  items: List<DrawerMenuItem>,
  dividerColor: Color
) {
  Column {
    val visibleMenuItems = items.filter { it.visible }
    if (visibleMenuItems.size == ONE) {
      HorizontalDivider(color = dividerColor)
    }
    visibleMenuItems.forEach { item ->
      DrawerMenuItemView(item)
    }
    if (visibleMenuItems.size == ONE) {
      HorizontalDivider(color = dividerColor)
    }
  }
}

@Composable
private fun DrawerMenuItemView(item: DrawerMenuItem) {
  ListItem(
    leadingContent = {
      Icon(
        painter = painterResource(id = item.iconRes),
        contentDescription = item.title
      )
    },
    headlineContent = {
      Text(text = item.title)
    },
    modifier = Modifier
      .fillMaxWidth()
      .clickable { item.onClick.invoke() }
      .semantics { testTag = item.testingTag }
  )
}
