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

package org.kiwix.kiwixmobile.core.downloader.downloadManager

@Suppress("MagicNumber")
enum class Error(val value: Int) {
  UNKNOWN(-1),
  NONE(0),
  NO_STORAGE_SPACE(1),
  NO_NETWORK_CONNECTION(2),
  UNKNOWN_IO_ERROR(3),
  CANCELLED(4),
  ERROR_CANNOT_RESUME(5),
  ERROR_DEVICE_NOT_FOUND(6),
  ERROR_FILE_ALREADY_EXISTS(7),
  ERROR_FILE_ERROR(8),
  ERROR_HTTP_DATA_ERROR(9),
  ERROR_INSUFFICIENT_SPACE(10),
  ERROR_TOO_MANY_REDIRECTS(11),
  ERROR_UNHANDLED_HTTP_CODE(12),
  QUEUED_FOR_WIFI(13),
  WAITING_FOR_NETWORK(14),
  WAITING_TO_RETRY(15),
  PAUSED_UNKNOWN(16);

  companion object {
    @Suppress("ComplexMethod", "MagicNumber")
    @JvmStatic
    fun valueOf(value: Int): Error {
      return when (value) {
        -1 -> UNKNOWN
        0 -> NONE
        1 -> NO_STORAGE_SPACE
        2 -> NO_NETWORK_CONNECTION
        3 -> UNKNOWN_IO_ERROR
        4 -> CANCELLED
        5 -> ERROR_CANNOT_RESUME
        6 -> ERROR_DEVICE_NOT_FOUND
        7 -> ERROR_FILE_ALREADY_EXISTS
        8 -> ERROR_FILE_ERROR
        9 -> ERROR_HTTP_DATA_ERROR
        10 -> ERROR_INSUFFICIENT_SPACE
        11 -> ERROR_TOO_MANY_REDIRECTS
        12 -> ERROR_UNHANDLED_HTTP_CODE
        13 -> QUEUED_FOR_WIFI
        14 -> WAITING_FOR_NETWORK
        15 -> WAITING_TO_RETRY
        16 -> PAUSED_UNKNOWN
        else -> UNKNOWN
      }
    }
  }
}
