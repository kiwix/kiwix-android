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
package org.kiwix.kiwixmobile.core.reader

import android.webkit.WebResourceResponse
import org.kiwix.kiwixmobile.core.reader.ZimReader.Factory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZimReaderContainer @Inject constructor(private val zimFileReaderFactory: Factory) {
  var zimReader: ZimReader? = null
    set(value) {
      field?.dispose()
      field = value
    }

  fun setZimSource(zimSource: ZimSource?) {
    if (zimSource == zimReader?.zimSource) {
      return
    }
    zimReader =
      if (zimSource?.exists() == true) zimFileReaderFactory.create(zimSource) else null
  }

  fun getPageUrlFromTitle(title: String) = zimReader?.getPageUrlFrom(title)

  fun getRandomArticleUrl() = zimReader?.getRandomArticleUrl()
  fun isRedirect(url: String): Boolean = zimReader?.isRedirect(url) == true
  fun getRedirect(url: String): String = zimReader?.getRedirect(url) ?: ""
  fun load(url: String) =
    WebResourceResponse(
      zimReader?.readMimeType(url),
      Charsets.UTF_8.name(),
      zimReader?.load(url)
    )

  fun copyReader(): ZimReader? = zimSource?.let(zimFileReaderFactory::create)

  val zimSource get() = zimReader?.zimSource
  val zimFileTitle get() = zimReader?.title
  val mainPage get() = zimReader?.mainPage
  val id get() = zimReader?.id
  val fileSize get() = zimReader?.fileSize ?: 0
  val creator get() = zimReader?.creator
  val publisher get() = zimReader?.publisher
  val name get() = zimReader?.name
  val date get() = zimReader?.date
  val description get() = zimReader?.description
  val favicon get() = zimReader?.favicon
  val language get() = zimReader?.language
}
