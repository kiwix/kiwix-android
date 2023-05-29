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

package org.kiwix.kiwixmobile.core

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

object Intents {
  @JvmStatic fun <T : Activity> internal(clazz: Class<T>): Intent =
    Intent(clazz.canonicalName).setPackage(CoreApp.instance.packageName)
}

@RequiresApi(Build.VERSION_CODES.R)
fun Activity.navigateToSettings() {
  val intent = Intent().apply {
    action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
    data = Uri.fromParts("package", packageName, null)
  }
  startActivity(intent)
}

fun Activity.navigateToAppSettings() {
  startActivity(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", packageName, null)
    }
  )
}
