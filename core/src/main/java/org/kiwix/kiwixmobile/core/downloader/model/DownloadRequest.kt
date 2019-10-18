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
package org.kiwix.kiwixmobile.core.downloader.model

import android.net.Uri
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.core.entity.MetaLinkNetworkEntity
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.StorageUtils

data class DownloadRequest(
  val urlString: String,
  val title: String,
  val description: String
) {

  val uri: Uri get() = Uri.parse(urlString)

  constructor(
    metaLinkNetworkEntity: MetaLinkNetworkEntity,
    book: LibraryNetworkEntity.Book
  ) : this(
    metaLinkNetworkEntity.relevantUrl.value,
    book.title,
    book.description
  )

  fun getDestination(sharedPreferenceUtil: SharedPreferenceUtil): String =
    "${sharedPreferenceUtil.prefStorage}/Kiwix/${
    StorageUtils.getFileNameFromUrl(urlString)
    }"
}
