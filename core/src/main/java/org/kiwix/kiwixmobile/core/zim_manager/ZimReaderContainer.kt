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
package org.kiwix.kiwixmobile.core.zim_manager

import android.net.Uri
import org.kiwix.kiwixlib.JNIKiwixSearcher
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZimReaderContainer @Inject constructor(
  private val zimFileReaderFactory: ZimFileReader.Factory,
  private val jniKiwixSearcher: JNIKiwixSearcher?
) {
  private val listOfAddedReaderIds = mutableListOf<String>()
  var zimFileReader: ZimFileReader? = null
    set(value) {
      field = value
      if (value != null && !listOfAddedReaderIds.contains(value.id)) {
        listOfAddedReaderIds.add(value.id)
        jniKiwixSearcher?.addKiwixReader(value.jniKiwixReader)
      }
    }

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
    jniKiwixSearcher?.search(query, count)
  }

  fun getNextResult() = jniKiwixSearcher?.nextResult
  fun isRedirect(url: String): Boolean = zimFileReader?.isRedirect(url) == true
  fun getRedirect(url: String): String = zimFileReader?.getRedirect(url) ?: ""

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
