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

package org.kiwix.kiwixmobile.custom.download.effects

import androidx.appcompat.app.AppCompatActivity
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.custom.BuildConfig
import javax.inject.Inject

data class DownloadCustom @Inject constructor(val downloader: Downloader) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    downloader.download(emptyBook(id = "custom", url = BuildConfig.ZIM_URL))
  }

  private fun emptyBook(
    id: String = "",
    title: String = "",
    description: String = "",
    language: String = "",
    creator: String = "",
    publisher: String = "",
    date: String = "",
    url: String = "",
    articleCount: String = "",
    mediaCount: String = "",
    size: String = "",
    name: String = "",
    favIcon: String = ""
  ) =
    Book().apply {
      this.id = id
      this.title = title
      this.description = description
      this.language = language
      this.creator = creator
      this.publisher = publisher
      this.date = date
      this.url = url
      this.articleCount = articleCount
      this.mediaCount = mediaCount
      this.size = size
      bookName = name
      favicon = favIcon
    }
}
