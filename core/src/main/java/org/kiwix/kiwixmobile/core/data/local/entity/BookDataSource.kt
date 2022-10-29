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
package org.kiwix.kiwixmobile.core.data.local.entity

import com.yahoo.squidb.annotations.TableModelSpec

@TableModelSpec(className = "BookDatabaseEntity", tableName = "book")
class BookDataSource {
  var bookId: String? = null
  var title: String? = null
  var description: String? = null
  var language: String? = null
  var bookCreator: String? = null
  var publisher: String? = null
  var date: String? = null
  var url: String? = null
  var remoteUrl: String? = null
  var articleCount: String? = null
  var mediaCount: String? = null
  var size: String? = null
  var favicon: String? = null
  var name: String? = null
}
