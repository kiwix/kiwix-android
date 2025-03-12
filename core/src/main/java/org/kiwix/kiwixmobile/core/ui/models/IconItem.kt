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

package org.kiwix.kiwixmobile.core.ui.models

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource

sealed class IconItem {
  data class Vector(val imageVector: ImageVector) : IconItem()
  data class Drawable(
    @DrawableRes val drawableRes: Int
  ) : IconItem()

  data class Bitmap(val bitmap: ImageBitmap) : IconItem()
}

/**
 * Extension function to convert an [IconItem] into a [Painter] for use in Composables.
 * This ensures that any type of icon can be easily used with the [Icon] Composable.
 */
@Composable
fun IconItem.toPainter(): Painter {
  return when (this) {
    is IconItem.Vector -> rememberVectorPainter(imageVector)
    is IconItem.Drawable -> painterResource(drawableRes)
    is IconItem.Bitmap -> remember { BitmapPainter(bitmap) }
  }
}
