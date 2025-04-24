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

import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap

sealed class IconItem {
  data class Vector(val imageVector: ImageVector) : IconItem()
  data class Drawable(
    @DrawableRes val drawableRes: Int
  ) : IconItem()

  data class ImageBitmap(val bitmap: androidx.compose.ui.graphics.ImageBitmap) : IconItem()
  data class MipmapImage(val mipmapResId: Int) : IconItem()
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
    is IconItem.ImageBitmap -> remember { BitmapPainter(bitmap) }
    is IconItem.MipmapImage -> {
      val drawable = ContextCompat.getDrawable(LocalContext.current, mipmapResId)
      val imageBitmap = when {
        drawable is BitmapDrawable -> drawable.bitmap.asImageBitmap()
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable -> {
          val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
          val canvas = Canvas(bitmap)
          drawable.setBounds(0, 0, canvas.width, canvas.height)
          drawable.draw(canvas)
          bitmap.asImageBitmap()
        }

        else -> {
          createBitmap(0, 0).asImageBitmap()
        }
      }
      return remember { BitmapPainter(imageBitmap) }
    }
  }
}
