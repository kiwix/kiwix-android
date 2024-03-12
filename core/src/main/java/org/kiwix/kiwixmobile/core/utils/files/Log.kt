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

package org.kiwix.kiwixmobile.core.utils.files

import org.kiwix.kiwixmobile.core.BuildConfig

/**
 * Helper class for logging that provides conditional logging for the debug variant.
 */
object Log {
  /**
   * Logs an error message with an optional throwable, but only in debug builds.
   *
   * @param tag The tag to identify the log.
   * @param message The message to be logged.
   * @param throwable An optional throwable to be logged.
   */
  fun e(tag: String, message: String?, throwable: Throwable? = null) {
    if (BuildConfig.DEBUG) {
      android.util.Log.e(tag, message, throwable)
    }
  }

  /**
   * Logs a warning message with an optional throwable, but only in debug builds.
   *
   * @param tag The tag to identify the log.
   * @param message The message to be logged.
   * @param throwable An optional throwable to be logged.
   */
  fun w(tag: String, message: String, throwable: Throwable? = null) {
    if (BuildConfig.DEBUG) {
      android.util.Log.w(tag, message, throwable)
    }
  }

  /**
   * Logs a debug message with an optional throwable, but only in debug builds.
   *
   * @param tag The tag to identify the log.
   * @param message The message to be logged.
   * @param throwable An optional throwable to be logged.
   */
  fun d(tag: String, message: String, throwable: Throwable? = null) {
    if (BuildConfig.DEBUG) {
      android.util.Log.d(tag, message, throwable)
    }
  }

  /**
   * Logs an info message, but only in debug builds.
   *
   * @param tag The tag to identify the log.
   * @param message The message to be logged.
   */
  fun i(tag: String, message: String) {
    if (BuildConfig.DEBUG) {
      android.util.Log.i(tag, message)
    }
  }

  /**
   * Logs a verbose message, but only in debug builds.
   *
   * @param tag The tag to identify the log.
   * @param message The message to be logged.
   */
  fun v(tag: String?, message: String) {
    if (BuildConfig.DEBUG) {
      android.util.Log.v(tag, message)
    }
  }
}
