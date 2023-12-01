/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.extensions

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import org.kiwix.kiwixmobile.core.base.BaseBroadcastReceiver
import java.util.Locale

fun Context?.toast(
  stringId: Int,
  length: Int = Toast.LENGTH_LONG
) {
  this?.let {
    Toast.makeText(this, stringId, length)
      .show()
  }
}

fun Context?.toast(
  text: String,
  length: Int = Toast.LENGTH_LONG
) {
  this?.let {
    Toast.makeText(this, text, length)
      .show()
  }
}

fun Context.registerReceiver(baseBroadcastReceiver: BaseBroadcastReceiver): Intent? =
  registerReceiver(baseBroadcastReceiver, IntentFilter(baseBroadcastReceiver.action))

val Context.locale: Locale
  get() = resources.configuration.locales.get(0)

fun Context.getAttribute(@AttrRes attributeRes: Int) = with(TypedValue()) {
  if (theme.resolveAttribute(attributeRes, this, true))
    data
  else
    throw RuntimeException("invalid attribute $attributeRes")
}

fun Context.getResizedDrawable(resourceId: Int, width: Int, height: Int): Drawable? {
  val drawable = ContextCompat.getDrawable(this, resourceId)

  return if (drawable != null) {
    val bitmap = Bitmap.createScaledBitmap(
      getBitmapFromDrawable(drawable),
      width,
      height,
      false
    )

    BitmapDrawable(resources, bitmap).apply {
      bounds = drawable.bounds
    }
  } else {
    null
  }
}

fun Context.getBitmapFromDrawable(drawable: Drawable): Bitmap {
  if (drawable is BitmapDrawable) {
    return drawable.bitmap
  }

  val bitmap = Bitmap.createBitmap(
    drawable.intrinsicWidth,
    drawable.intrinsicHeight,
    Bitmap.Config.ARGB_8888
  )
  val canvas = Canvas(bitmap)
  drawable.setBounds(0, 0, canvas.width, canvas.height)
  drawable.draw(canvas)

  return bitmap
}

