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
enum class Status(val value: Int) {
  NONE(0),
  QUEUED(1),
  DOWNLOADING(2),
  PAUSED(3),
  COMPLETED(4),
  CANCELLED(5),
  FAILED(6),
  REMOVED(7),
  DELETED(8),
  ADDED(9);

  companion object {

    @Suppress("MagicNumber")
    @JvmStatic
    fun valueOf(value: Int): Status {
      return when (value) {
        0 -> NONE
        1 -> QUEUED
        2 -> DOWNLOADING
        3 -> PAUSED
        4 -> COMPLETED
        5 -> CANCELLED
        6 -> FAILED
        7 -> REMOVED
        8 -> DELETED
        9 -> ADDED
        else -> NONE
      }
    }
  }
}
