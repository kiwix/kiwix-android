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

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import org.kiwix.kiwixmobile.core.extensions.get

/**
 * Wrapper class created for `DocumentsContract` and `ContentResolver` methods.
 * This class facilitates the usage of these methods in test cases where direct
 * mocking is not feasible.
 */
class DocumentResolverWrapper {
  fun getDocumentId(uri: Uri): String = DocumentsContract.getDocumentId(uri)

  @Suppress("LongParameterList")
  fun query(
    context: Context,
    uri: Uri,
    columnName: String,
    selection: String?,
    selectionArgs: Array<String>?,
    sortOrder: String?
  ): String? = context.contentResolver.query(
    uri,
    arrayOf(columnName),
    selection,
    selectionArgs,
    sortOrder
  )?.use {
    return@query if (it.moveToFirst() && it.getColumnIndex(columnName) != -1) {
      it[columnName]
    } else {
      var path: String? = null
      if ("$uri".contains("org.kiwix.kiwixmobile.fileprovider")) {
        // For testing scenarios, as it's within the app-specific directory and the content resolver
        // cannot provide the path for internal app paths, so we extract the path from the URI.
        path = "/storage/" + "$uri".substringAfter("external_files/")
      }
      path
    }
  }
}
