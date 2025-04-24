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

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import org.kiwix.kiwixmobile.core.base.BaseBroadcastReceiver
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
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
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    registerReceiver(
      baseBroadcastReceiver,
      IntentFilter(baseBroadcastReceiver.action),
      RECEIVER_NOT_EXPORTED
    )
  } else {
    registerReceiver(
      baseBroadcastReceiver,
      IntentFilter(baseBroadcastReceiver.action)
    )
  }

val Context.locale: Locale
  get() = resources.configuration.locales.get(0)

fun Context.getAttribute(
  @AttrRes attributeRes: Int
) = with(TypedValue()) {
  if (theme.resolveAttribute(attributeRes, this, true)) {
    data
  } else {
    throw RuntimeException("invalid attribute $attributeRes")
  }
}

fun Context.getResizedDrawable(resourceId: Int, width: Int, height: Int): Drawable? {
  val drawable = ContextCompat.getDrawable(this, resourceId)

  return if (drawable != null) {
    val bitmap = getBitmapFromDrawable(drawable).scale(width, height, false)

    bitmap.toDrawable(resources).apply {
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

  val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
  val canvas = Canvas(bitmap)
  drawable.setBounds(0, 0, canvas.width, canvas.height)
  drawable.draw(canvas)

  return bitmap
}

@Suppress("Deprecation")
fun Context.isServiceRunning(serviceClass: Class<out Service>): Boolean {
  val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
  val services = activityManager.getRunningServices(Int.MAX_VALUE)

  return services.any { it.service.className == serviceClass.name }
}

fun Context.getDialogHostComposeView(alertDialogShower: AlertDialogShower) =
  ComposeView(this).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setContent {
      DialogHost(alertDialogShower)
    }
  }
