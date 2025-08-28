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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.models.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.White

const val NAVIGATION_ICON_TESTING_TAG = "navigationIconTestingTag"

/**
 * A composable function that renders a navigation icon, which can be either a vector
 * or drawable image.
 *
 * @param iconItem The icon to be displayed. It can be either a vector (`IconItem.Vector`)
 * or a drawable (`IconItem.Drawable`).
 * @param onClick Callback function triggered when the icon is clicked.
 * @param contentDescription A string resource ID for accessibility purposes, describing
 * the icon's function.
 * @param iconTint The color used to tint the icon. Default is white.
 */
@Composable
fun NavigationIcon(
  iconItem: IconItem = IconItem.Vector(Icons.AutoMirrored.Filled.ArrowBack),
  onClick: () -> Unit,
  @StringRes contentDescription: Int = R.string.toolbar_back_button_content_description,
  iconTint: Color = White,
  testingTag: String = NAVIGATION_ICON_TESTING_TAG
) {
  IconButton(onClick = onClick, modifier = Modifier.semantics { testTag = testingTag }) {
    Icon(
      painter = iconItem.toPainter(),
      contentDescription = stringResource(contentDescription),
      tint = getNavigationIconTintColor(iconTint)
    )
  }
}

/**
 * Returns the navigationIcon color.
 *
 * If iconTint is set as [Color.Unspecified] then return that color. Because it is set to show
 * the actual color of image or icon set on navigation icon.
 *
 * Otherwise: return the navigationIcon color according to theme.
 */
@Composable
private fun getNavigationIconTintColor(iconTint: Color): Color =
  if (iconTint == Color.Unspecified) {
    iconTint
  } else {
    MaterialTheme.colorScheme.onBackground
  }
