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
package org.kiwix.kiwixmobile.zim_manager

import android.net.Uri
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZimReaderContainer @Inject constructor(
  private val zimFileReaderFactory: ZimFileReader.Factory
) {
  fun isRedirect(url: String): Boolean = zimFileReader?.isRedirect(url) == true
  fun getRedirect(url: String): String = zimFileReader?.getRedirect(url) ?: ""
  var zimFileReader: ZimFileReader? = null
  fun setZimFile(file: File?) {
    if (file?.canonicalPath == zimFileReader?.zimFile?.canonicalPath) {
      return
    }
    zimFileReader =
      if (file?.exists() == true) zimFileReaderFactory.create(file)
      else null
  }

  fun readMimeType(uri: Uri) = zimFileReader?.readMimeType(uri)

  fun load(uri: Uri) = zimFileReader?.load(uri)
  fun searchSuggestions(prefix: String, count: Int) =
    zimFileReader?.searchSuggestions(prefix, count) ?: false

  fun getNextSuggestion() = zimFileReader?.getNextSuggestion()

  fun getPageUrlFromTitle(title: String) = zimFileReader?.getPageUrlFrom(title)
  fun getRandomArticleUrl() = zimFileReader?.getRandomArticleUrl()
  fun search(query: String, count: Int) {
    zimFileReader?.jniKiwixSearcher?.search(query, count)
  }

  fun getNextResult() = zimFileReader?.jniKiwixSearcher?.nextResult

  val zimFile get() = zimFileReader?.zimFile
  val zimCanonicalPath get() = zimFileReader?.zimFile?.canonicalPath
  val zimFileTitle get() = zimFileReader?.title
  val mainPage get() = zimFileReader?.mainPage
  val id get() = zimFileReader?.id
  val fileSize get() = zimFileReader?.fileSize ?: 0
  val creator get() = zimFileReader?.creator
  val publisher get() = zimFileReader?.publisher
  val name get() = zimFileReader?.name
  val date get() = zimFileReader?.date
  val description get() = zimFileReader?.description
  val favicon get() = zimFileReader?.favicon
  val language get() = zimFileReader?.language
}
