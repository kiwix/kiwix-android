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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.IconItem

@Composable
fun AppBarNavigationIcon(
  isSearchActive: Boolean,
  onClick: () -> Unit
) {
  NavigationIcon(
    iconItem = if (isSearchActive) {
      IconItem.Vector(Icons.AutoMirrored.Filled.ArrowBack)
    } else {
      IconItem.Drawable(
        R.drawable.ic_close_white_24dp
      )
    },
    onClick = onClick
  )
}
