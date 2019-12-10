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
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.widget.Toast
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
  get() =
    if (VERSION.SDK_INT >= VERSION_CODES.N) resources.configuration.locales.get(0)
    else resources.configuration.locale
