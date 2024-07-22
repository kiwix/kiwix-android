/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile

import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import org.kiwix.kiwixmobile.core.utils.files.Log

object RxJavaUncaughtExceptionHandling {
  private const val TAG_RX_JAVA_DEFAULT_ERROR_HANDLER = "RxJavaDefaultErrorHandler"
  fun setUp() {
    RxJavaPlugins.setErrorHandler { exception ->
      when (exception) {
        is UndeliverableException -> {
          // Merely log undeliverable exceptions
          Log.i(
            TAG_RX_JAVA_DEFAULT_ERROR_HANDLER,
            "Caught undeliverable exception: ${exception.cause}"
          )
        }

        else -> {
          Thread.currentThread().also { thread ->
            thread.uncaughtExceptionHandler?.uncaughtException(thread, exception)
          }
        }
      }
    }
  }
}
