/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.zim_manager

import org.kiwix.kiwixlib.Filter
import org.kiwix.kiwixlib.Library
import org.kiwix.kiwixlib.Manager
import org.kiwix.kiwixmobile.core.di.modules.NetworkModule.KIWIX_DOWNLOAD_URL
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import javax.inject.Inject

class NetworkResponseMapper @Inject constructor() {

  fun map(rawStream: String): List<Book> =
    with(Library()) {
      Manager(this).readOpds(rawStream, KIWIX_DOWNLOAD_URL)
      val filter = filter(Filter().query("wikipedia articles about"))
      filter.map(this::getBookById).map(::OpdsBook).map {
        Book().apply {
          id = it.id
          title = it.title
          description = it.description
          language = it.language
          creator = it.creator
          publisher = it.publisher
          date = it.date
          url = it.url
          articleCount = it.articleCount.toString()
          mediaCount = it.mediaCount.toString()
          size = it.size.toString()
          bookName = it.name
          favicon = it.favicon
          tags = it.tags
        }
      }
    }
}
