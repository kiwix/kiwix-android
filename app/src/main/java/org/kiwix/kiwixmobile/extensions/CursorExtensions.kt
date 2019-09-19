/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.extensions

import android.database.Cursor

inline fun Cursor.forEachRow(block: (Cursor) -> Unit) {
  while (moveToNext()) {
    block.invoke(this)
  }
  close()
}

@Suppress("IMPLICIT_CAST_TO_ANY")
inline operator fun <reified T> Cursor.get(columnName: String): T =
  when (T::class) {
    String::class -> getString(columnIndex(columnName))
    Long::class -> getLong(columnIndex(columnName))
    Integer::class -> getInt(columnIndex(columnName))
    else -> throw RuntimeException("Unexpected return type ${T::class.java.simpleName}")
  } as T

fun Cursor.columnIndex(columnName: String) =
  if (columnNames.contains(columnName)) {
    getColumnIndex(columnName)
  } else {
    throw RuntimeException("$columnName not found in $columnNames")
  }
