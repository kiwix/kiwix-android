/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import java.lang.ref.WeakReference

class ReadAloudService : Service() {
  private val serviceBinder: IBinder = ReadAloudBinder(this)

  override fun onBind(p0: Intent?): IBinder = serviceBinder

  class ReadAloudBinder(readAloudService: ReadAloudService) : Binder() {
    val service: WeakReference<ReadAloudService>

    init {
      service = WeakReference<ReadAloudService>(readAloudService)
    }
  }
}
