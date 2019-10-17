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

package org.kiwix.kiwixmobile.core.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

internal abstract class AbstractContentProvider : ContentProvider() {
  override fun query(
    url: Uri,
    projection: Array<String>?,
    selection: String?,
    selectionArgs: Array<String>?,
    sort: String?
  ): Cursor? {
    throw RuntimeException("Operation not supported")
  }

  override fun insert(uri: Uri, initialValues: ContentValues?): Uri? {
    throw RuntimeException("Operation not supported")
  }

  override fun update(
    uri: Uri,
    values: ContentValues?,
    where: String?,
    whereArgs: Array<String>?
  ): Int {
    throw RuntimeException("Operation not supported")
  }

  override fun delete(uri: Uri, where: String?, whereArgs: Array<String>?): Int {
    throw RuntimeException("Operation not supported")
  }
}
