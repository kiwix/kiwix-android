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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.models.toPainter
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.NAVIGATION_DRAWER_WIDTH

@Composable
fun LeftDrawerMenu(drawerMenuGroupList: List<DrawerMenuGroup>) {
  Surface(
    modifier = Modifier
      .width(NAVIGATION_DRAWER_WIDTH)
      .fillMaxHeight(),
    shadowElevation = EIGHT_DP
  ) {
    Column {
      // Banner image at the top
      Image(
        painter = IconItem.MipmapImage(R.drawable.ic_home_kiwix_banner).toPainter(),
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        modifier = Modifier.fillMaxWidth()
      )
      drawerMenuGroupList.forEach {
        DrawerGroup(it.drawerMenuItemList)
      }
    }
  }
}

@Composable
private fun DrawerGroup(items: List<DrawerMenuItem>) {
  Column {
    items.filter { it.visible }.forEach { item ->
      DrawerMenuItemView(item)
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
      .clickable { item.onClick }
  )
}
